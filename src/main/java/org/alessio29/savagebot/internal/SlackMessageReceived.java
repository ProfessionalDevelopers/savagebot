package org.alessio29.savagebot.internal;

import com.slack.api.model.event.MessageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlackMessageReceived implements IMessageReceived {

    private static final Pattern MENTION_PATTERN = Pattern.compile("<@(\\w+)>");

    private final MessageEvent event;
    private final String guildId;
    private final String channelId;
    private final String authorId;
    private final String authorMention;
    private final String rawMessage;
    private final List<String> mentions;

    public SlackMessageReceived(MessageEvent event) {
        this.event = event;
        this.guildId = event.getTeam();
        this.channelId = event.getChannel();
        this.authorId = event.getUser();
        this.authorMention = "<@" + event.getUser() + ">";
        this.rawMessage = event.getText() != null ? event.getText() : "";
        this.mentions = extractMentions(this.rawMessage);
    }

    private static List<String> extractMentions(String text) {
        Matcher matcher = MENTION_PATTERN.matcher(text);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add("<@" + matcher.group(1) + ">");
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public String getGuildId() {
        return guildId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public String getAuthorId() {
        return authorId;
    }

    @Override
    public String getAuthorMention() {
        return authorMention;
    }

    @Override
    public String getRawMessage() {
        return rawMessage;
    }

    @Override
    public Object getOriginalEvent() {
        return event;
    }

    @Override
    public List<String> getMentions() {
        return mentions;
    }
}
