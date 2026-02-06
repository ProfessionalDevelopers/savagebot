package org.alessio29.savagebot.internal.builders;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;

import java.io.IOException;
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
            client.chatPostMessage(r -> r
                    .channel(channelId)
                    .text(convertToSlackMarkdown(message))
            );
        } catch (IOException | SlackApiException e) {
            System.err.println("Failed to send Slack message: " + e.getMessage());
        }
    }

    /**
     * Convert Discord-style markdown to Slack mrkdwn.
     * Discord: **bold** *italic* __underline__ ~~strike~~
     * Slack:   *bold*  _italic_ (no underline)  ~strike~
     */
    static String convertToSlackMarkdown(String message) {
        // Underline: __text__ → plain text (no Slack equivalent); handle before italic
        message = message.replaceAll("__(.+?)__", "$1");
        // Bold: **text** → placeholder to avoid conflict with italic conversion
        message = message.replaceAll("\\*\\*(.+?)\\*\\*", "\u0001$1\u0002");
        // Italic: *text* → _text_
        message = message.replaceAll("\\*(.+?)\\*", "_$1_");
        // Restore bold: placeholder → *text*
        message = message.replaceAll("\u0001(.+?)\u0002", "*$1*");
        // Strikethrough: ~~text~~ → ~text~
        message = message.replaceAll("~~(.+?)~~", "~$1~");
        return message;
    }

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
