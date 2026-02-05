package org.alessio29.savagebot.internal;

import java.util.List;

public interface IMessageReceived {

    String getGuildId();

    String getChannelId();

    String getAuthorId();

    String getAuthorMention();

    String getRawMessage();

    Object getOriginalEvent();

    List<String> getMentions();

}
