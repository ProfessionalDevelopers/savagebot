package org.alessio29.savagebot.slack;

import com.slack.api.model.event.MessageEvent;
import org.alessio29.savagebot.internal.SlackMessageReceived;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TestSlackMessageReceived {

    private static MessageEvent makeEvent(String user, String channel, String team, String text) {
        MessageEvent event = new MessageEvent();
        event.setUser(user);
        event.setChannel(channel);
        event.setTeam(team);
        event.setText(text);
        return event;
    }

    @Test
    public void testBasicFields() {
        MessageEvent event = makeEvent("U123", "C456", "T789", "hello world");
        SlackMessageReceived msg = new SlackMessageReceived(event);

        Assert.assertEquals("U123", msg.getAuthorId());
        Assert.assertEquals("C456", msg.getChannelId());
        Assert.assertEquals("T789", msg.getGuildId());
        Assert.assertEquals("hello world", msg.getRawMessage());
        Assert.assertEquals("<@U123>", msg.getAuthorMention());
        Assert.assertSame(event, msg.getOriginalEvent());
    }

    @Test
    public void testNullText() {
        MessageEvent event = makeEvent("U123", "C456", "T789", null);
        SlackMessageReceived msg = new SlackMessageReceived(event);

        Assert.assertEquals("", msg.getRawMessage());
    }

    @Test
    public void testMentionExtraction() {
        MessageEvent event = makeEvent("U123", "C456", "T789", "Hey <@U111> and <@U222> check this");
        SlackMessageReceived msg = new SlackMessageReceived(event);

        Assert.assertEquals(Arrays.asList("<@U111>", "<@U222>"), msg.getMentions());
    }

    @Test
    public void testNoMentions() {
        MessageEvent event = makeEvent("U123", "C456", "T789", "just a plain message");
        SlackMessageReceived msg = new SlackMessageReceived(event);

        Assert.assertEquals(Collections.emptyList(), msg.getMentions());
    }

    @Test
    public void testMentionsAreUnmodifiable() {
        MessageEvent event = makeEvent("U123", "C456", "T789", "Hey <@U111>");
        SlackMessageReceived msg = new SlackMessageReceived(event);

        try {
            msg.getMentions().add("<@UFAKE>");
            Assert.fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testCommandProcessingCompatibility() {
        // Verify a message with a command prefix works as expected through getRawMessage
        MessageEvent event = makeEvent("U123", "C456", "T789", "!ping");
        SlackMessageReceived msg = new SlackMessageReceived(event);

        Assert.assertEquals("!ping", msg.getRawMessage());
    }
}
