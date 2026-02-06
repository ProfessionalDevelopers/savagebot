package org.alessio29.savagebot.internal.builders;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class SlackResponseBuilder extends SplittingResponseBuilder {

    private static final Logger log = LogManager.getLogger(SlackResponseBuilder.class);

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
            String slackMsg = convertToSlackMarkdown(message);
            client.chatPostMessage(r -> r
                    .channel(channelId)
                    .text(slackMsg)
            );
        } catch (IOException | SlackApiException e) {
            log.error("Failed to send Slack message: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void sendTableResponse(String mention, String textFallback, TableData table) {
        try {
            JsonArray blocks = new JsonArray();

            // Section block for mention + title
            StringBuilder sectionText = new StringBuilder(mention);
            if (table.getTitle() != null) {
                sectionText.append("\n*").append(table.getTitle()).append("*");
            }
            JsonObject section = new JsonObject();
            section.addProperty("type", "section");
            JsonObject text = new JsonObject();
            text.addProperty("type", "mrkdwn");
            text.addProperty("text", sectionText.toString());
            section.add("text", text);
            blocks.add(section);

            // Table block
            JsonObject tableBlock = new JsonObject();
            tableBlock.addProperty("type", "table");

            JsonArray jsonRows = new JsonArray();

            // Header row
            JsonArray headerCells = new JsonArray();
            for (String header : table.getHeaders()) {
                JsonObject cell = new JsonObject();
                cell.addProperty("type", "raw_text");
                cell.addProperty("text", header);
                headerCells.add(cell);
            }
            jsonRows.add(headerCells);

            // Data rows
            for (String[] row : table.getRows()) {
                JsonArray rowCells = new JsonArray();
                for (String cellValue : row) {
                    String val = replaceSuitSymbols(cellValue);
                    if (val == null || val.isEmpty()) {
                        val = " ";
                    }
                    JsonObject cell = new JsonObject();
                    cell.addProperty("type", "raw_text");
                    cell.addProperty("text", val);
                    rowCells.add(cell);
                }
                jsonRows.add(rowCells);
            }
            tableBlock.add("rows", jsonRows);
            blocks.add(tableBlock);

            String blocksJson = blocks.toString();
            log.debug("Sending Block Kit table: {}", blocksJson);

            String fallback = stripMarkup(textFallback);
            ChatPostMessageResponse response = client.chatPostMessage(r -> r
                    .channel(channelId)
                    .blocksAsString(blocksJson)
                    .text(fallback)
            );

            if (!response.isOk()) {
                log.error("Slack rejected Block Kit table: {}", response.getError());
                log.debug("Falling back to text for table response");
                super.sendTableResponse(mention, textFallback, table);
            }
        } catch (IOException | SlackApiException e) {
            log.error("Failed to send Slack table: {}", e.getMessage(), e);
            log.debug("Falling back to text for table response");
            super.sendTableResponse(mention, textFallback, table);
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
