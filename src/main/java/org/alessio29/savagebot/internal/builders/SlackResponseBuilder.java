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
                    .text(message)
            );
        } catch (IOException | SlackApiException e) {
            System.err.println("Failed to send Slack message: " + e.getMessage());
        }
    }

    @Override
    protected void sendPrivateReply(String message) {
        try {
            com.slack.api.methods.response.conversations.ConversationsOpenResponse openResult =
                    client.conversationsOpen(r -> r
                            .users(java.util.Collections.singletonList(userId))
                    );
            if (openResult.isOk()) {
                client.chatPostMessage(r -> r
                        .channel(openResult.getChannel().getId())
                        .text(message)
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
