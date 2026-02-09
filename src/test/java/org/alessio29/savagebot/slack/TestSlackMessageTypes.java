package org.alessio29.savagebot.slack;

import org.alessio29.savagebot.TestUtils;
import org.alessio29.savagebot.internal.SlackSlashCommandMessage;
import org.alessio29.savagebot.internal.builders.SplittingResponseBuilder;
import org.alessio29.savagebot.internal.commands.CommandInterpreter;
import org.alessio29.savagebot.internal.commands.CommandRegistry;
import org.alessio29.savagebot.internal.commands.Commands;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestSlackMessageTypes {

    @Before
    public void setup() {
        CommandRegistry.getInstance().reset();
        Commands.registerDefaultCommands();
    }

    @Test
    public void testSlackSlashCommandMessageGetters() {
        SlackSlashCommandMessage msg = new SlackSlashCommandMessage("U123", "C456", "T789", "some text");
        Assert.assertEquals("U123", msg.getAuthorId());
        Assert.assertEquals("C456", msg.getChannelId());
        Assert.assertEquals("T789", msg.getGuildId());
        Assert.assertEquals("some text", msg.getRawMessage());
        Assert.assertEquals("<@U123>", msg.getAuthorMention());
        Assert.assertNull(msg.getOriginalEvent());
        Assert.assertTrue(msg.getMentions().isEmpty());
    }

    @Test
    public void testSlackSlashCommandMessageNullText() {
        SlackSlashCommandMessage msg = new SlackSlashCommandMessage("U123", "C456", "T789", null);
        Assert.assertEquals("", msg.getRawMessage());
    }

    @Test
    public void testCommandPipelineWithSlackMessage() {
        // Prove that CommandInterpreter works with SlackSlashCommandMessage
        SlackSlashCommandMessage msg = new SlackSlashCommandMessage("U123", "C456", "T789", "!ping");

        StringBuilder result = new StringBuilder();
        TestUtils.StringResponseBuilder responseBuilder =
                new TestUtils.StringResponseBuilder(4000, result, msg);
        new CommandInterpreter().run(msg, responseBuilder);
        responseBuilder.sendResponse();

        String output = result.toString();
        Assert.assertTrue("Expected 'SavageBot is ready' in output, got: " + output,
                output.contains("SavageBot is ready"));
    }

    @Test
    public void testHelpCommandViaSlackMessage() {
        SlackSlashCommandMessage msg = new SlackSlashCommandMessage("U123", "C456", "T789", "!help");

        StringBuilder result = new StringBuilder();
        TestUtils.StringResponseBuilder responseBuilder =
                new TestUtils.StringResponseBuilder(4000, result, msg);
        new CommandInterpreter().run(msg, responseBuilder);
        responseBuilder.sendResponse();

        String output = result.toString();
        Assert.assertTrue("Expected 'DICE category' in output", output.contains("DICE category"));
        Assert.assertTrue("Expected 'CARDS category' in output", output.contains("CARDS category"));
    }

    @Test
    public void testSlackMessagePartsLimit() {
        // Verify that a SplittingResponseBuilder with Slack-like limits (4000 chars, 3 parts)
        // correctly truncates long output
        final List<String> sentParts = new ArrayList<>();
        final List<String> sentPrivate = new ArrayList<>();

        SplittingResponseBuilder builder = new SplittingResponseBuilder(100) {
            @Override
            protected String getUserMention() {
                return "<@U123>";
            }

            @Override
            protected void sendReplyToOrigin(String message) {
                sentParts.add(message);
            }

            @Override
            protected void sendReplyPartsToOrigin(List<String> parts) {
                // Mimic SlackResponseBuilder: cap at 3 parts
                if (parts.size() > 3) {
                    super.sendReplyPartsToOrigin(parts.subList(0, 3));
                    sendReplyToOrigin("...and so on. Command result is too long.");
                    return;
                }
                super.sendReplyPartsToOrigin(parts);
            }

            @Override
            protected void sendPrivateReply(String message) {
                sentPrivate.add(message);
            }
        };

        // Generate a very long result that will split into many parts at 100-char limit
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("word").append(i).append(" ");
        }
        builder.addRaw(longText.toString());
        builder.sendResponse();

        // Should have at most 3 real parts + 1 truncation message
        Assert.assertTrue("Expected at most 4 sent parts, got " + sentParts.size(), sentParts.size() <= 4);
        if (sentParts.size() == 4) {
            Assert.assertTrue("Last part should be truncation message",
                    sentParts.get(3).contains("too long"));
        }
    }
}
