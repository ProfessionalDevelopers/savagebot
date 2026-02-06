package org.alessio29.savagebot.internal.builders;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackResponseBuilder extends SplittingResponseBuilder {

    public static final int MESSAGE_LENGTH_LIMIT = 4000;

    public static final int MESSAGE_PARTS_LIMIT = 3;

    private final MethodsClient client;
    private final String channelId;
    private final String userId;

    public SlackResponseBuilder(MethodsClient client, String channelId, String userId) {
        super(MESSAGE_LENGTH_LIMIT);
        this.client = client;
        this.channelId = channelId;
        this.userId = userId;
    }

    @Override
    protected void sendReplyPartsToOrigin(List<String> parts) {
        if (parts.size() > MESSAGE_PARTS_LIMIT) {
            super.sendReplyPartsToOrigin(parts.subList(0, MESSAGE_PARTS_LIMIT));
            sendReplyToOrigin(
                    "...and so on. Command result is too long. " +
                    "If you really want to do such thing, you can send commands to bot privately."
            );
            return;
        }
        super.sendReplyPartsToOrigin(parts);
    }

    @Override
    protected void sendReplyToOrigin(String message) {
        try {
            // Try table conversion first with original symbols intact
            String tableBlocks = tryBuildTableBlocks(message);
            if (tableBlocks != null) {
                String fallback = stripMarkup(message);
                client.chatPostMessage(r -> r
                        .channel(channelId)
                        .blocksAsString(tableBlocks)
                        .text(fallback)
                );
            } else {
                // Code block fallback: replace suit symbols for monospace alignment
                String slackMsg = convertToSlackMarkdown(message);
                client.chatPostMessage(r -> r
                        .channel(channelId)
                        .text(slackMsg)
                );
            }
        } catch (IOException | SlackApiException e) {
            System.err.println("Failed to send Slack message: " + e.getMessage());
        }
    }

    // ── Suit symbol replacement ──

    static String replaceSuitSymbols(String message) {
        return message
                .replace("\u2660", "S")   // ♠ spades
                .replace("\u2665", "H")   // ♥ hearts
                .replace("\u2666", "D")   // ♦ diamonds
                .replace("\u2663", "C")   // ♣ clubs
                .replace("\uD83C\uDCCF", "JK")  // 🃏 color joker
                .replace("\uD83C\uDCBF", "JK"); // 🃿 black joker
    }

    // ── Discord → Slack markdown ──

    static String convertToSlackMarkdown(String message) {
        message = replaceSuitSymbols(message);
        message = message.replaceAll("__(.+?)__", "$1");
        message = message.replaceAll("\\*\\*(.+?)\\*\\*", "\u0001$1\u0002");
        message = message.replaceAll("\\*(.+?)\\*", "_$1_");
        message = message.replaceAll("\u0001(.+?)\u0002", "*$1*");
        message = message.replaceAll("~~(.+?)~~", "~$1~");
        return message;
    }

    private static String stripMarkup(String message) {
        return message.replaceAll("```", "").replaceAll("[*_~]", "").trim();
    }

    // ── Code-block table → Block Kit table conversion ──

    /**
     * If the message contains a code block that looks like a padded table,
     * convert it to Block Kit JSON with a table block. Returns null if
     * the message doesn't contain a parseable table.
     */
    String tryBuildTableBlocks(String message) {
        int blockStart = message.indexOf("```");
        if (blockStart < 0) return null;
        int blockEnd = message.indexOf("```", blockStart + 3);
        if (blockEnd < 0) return null;

        String prefix = message.substring(0, blockStart).trim();
        String codeContent = message.substring(blockStart + 3, blockEnd).trim();

        String[] lines = codeContent.split("\n");
        if (lines.length < 2) return null;

        // Separate decorative title lines from table data lines
        List<String> titleLines = new ArrayList<>();
        List<String> tableLines = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            if (line.trim().matches("^=+.*=+$")) {
                titleLines.add(line.trim());
            } else {
                tableLines.add(line);
            }
        }

        if (tableLines.size() < 2) return null;

        // Detect column boundaries from the header row
        List<Integer> colStarts = detectColumnStarts(tableLines.get(0));
        if (colStarts.size() < 2) return null;

        // Parse all rows
        List<String[]> parsedRows = new ArrayList<>();
        for (String line : tableLines) {
            parsedRows.add(extractColumns(line, colStarts));
        }

        // Build blocks JSON
        JsonArray blocks = new JsonArray();

        // Section block for mention + title
        StringBuilder sectionText = new StringBuilder();
        if (!prefix.isEmpty()) {
            sectionText.append(prefix);
        }
        for (String title : titleLines) {
            if (sectionText.length() > 0) sectionText.append("\n");
            sectionText.append("*").append(title).append("*");
        }
        if (sectionText.length() > 0) {
            JsonObject section = new JsonObject();
            section.addProperty("type", "section");
            JsonObject text = new JsonObject();
            text.addProperty("type", "mrkdwn");
            text.addProperty("text", sectionText.toString());
            section.add("text", text);
            blocks.add(section);
        }

        // Table block
        JsonObject table = new JsonObject();
        table.addProperty("type", "table");

        JsonArray jsonRows = new JsonArray();
        for (String[] row : parsedRows) {
            JsonArray jsonCells = new JsonArray();
            for (String cell : row) {
                JsonObject cellObj = new JsonObject();
                cellObj.addProperty("type", "raw_text");
                cellObj.addProperty("text", cell);
                jsonCells.add(cellObj);
            }
            jsonRows.add(jsonCells);
        }
        table.add("rows", jsonRows);
        blocks.add(table);

        return blocks.toString();
    }

    /**
     * Detect column start positions from a header line by finding where
     * non-space text begins after a gap of 2+ spaces.
     */
    static List<Integer> detectColumnStarts(String headerLine) {
        List<Integer> starts = new ArrayList<>();
        int spaceRun = 0;
        boolean seenNonSpace = false;

        for (int i = 0; i < headerLine.length(); i++) {
            if (headerLine.charAt(i) == ' ') {
                spaceRun++;
            } else {
                if (!seenNonSpace || spaceRun >= 2) {
                    starts.add(i);
                }
                seenNonSpace = true;
                spaceRun = 0;
            }
        }
        return starts;
    }

    /**
     * Extract column values from a line using known column start positions.
     */
    static String[] extractColumns(String line, List<Integer> colStarts) {
        String[] cols = new String[colStarts.size()];
        for (int c = 0; c < colStarts.size(); c++) {
            int start = colStarts.get(c);
            int end = (c + 1 < colStarts.size()) ? colStarts.get(c + 1) : line.length();
            if (start >= line.length()) {
                cols[c] = "";
            } else {
                cols[c] = line.substring(start, Math.min(end, line.length())).trim();
            }
        }
        return cols;
    }

    // ── Private messages ──

    @Override
    protected void sendPrivateReply(String message) {
        try {
            com.slack.api.methods.response.conversations.ConversationsOpenResponse openResult =
                    client.conversationsOpen(r -> r
                            .users(java.util.Collections.singletonList(userId))
                    );
            if (openResult.isOk()) {
                String slackMessage = convertToSlackMarkdown(message);
                client.chatPostMessage(r -> r
                        .channel(openResult.getChannel().getId())
                        .text(slackMessage)
                );
            }
        } catch (IOException | SlackApiException e) {
            System.err.println("Failed to send Slack private message: " + e.getMessage());
        }
    }

    @Override
    protected String getUserMention() {
        return "<@" + userId + ">";
    }
}
