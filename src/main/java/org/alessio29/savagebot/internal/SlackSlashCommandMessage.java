package org.alessio29.savagebot.internal;

import java.util.Collections;
import java.util.List;

public class SlackSlashCommandMessage implements IMessageReceived {

    private final String userId;
    private final String channelId;
    private final String teamId;
    private final String rawMessage;

    public SlackSlashCommandMessage(String userId, String channelId, String teamId, String rawMessage) {
        this.userId = userId;
        this.channelId = channelId;
        this.teamId = teamId;
        this.rawMessage = rawMessage != null ? rawMessage : "";
    }

    @Override
    public String getGuildId() {
        return teamId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getAuthorId() {
        return userId;
    }

    @Override
    public String getAuthorMention() {
        return "<@" + userId + ">";
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }

    @Override
    public Object getOriginalEvent() {
        return null;
    }

    @Override
    public List<String> getMentions() {
        return Collections.emptyList();
    }
}
