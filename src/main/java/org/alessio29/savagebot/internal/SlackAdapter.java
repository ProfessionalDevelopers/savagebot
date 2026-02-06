package org.alessio29.savagebot.internal;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.MessageEvent;
import org.alessio29.savagebot.internal.builders.SlackResponseBuilder;
import org.alessio29.savagebot.internal.commands.CommandInterpreter;

import java.util.Collection;

public class SlackAdapter implements PlatformAdapter {

    private final String botToken;
    private final String appToken;
    private App app;
    private SocketModeApp socketModeApp;
    private String selfMention;

    public SlackAdapter(String botToken, String appToken) {
        this.botToken = botToken;
        this.appToken = appToken;
    }

    @Override
    public String getPlatformName() {
        return "Slack";
    }

    @Override
    public int getConnectedWorkspaceCount() {
        return app != null ? 1 : 0;
    }

    @Override
    public String getSelfMention() {
        return selfMention;
    }

    @Override
    public void registerSlashCommands(Collection<SlashCommandDefinition> commands) {
        if (app == null || commands.isEmpty()) {
            return;
        }
        for (SlashCommandDefinition def : commands) {
            String slashName = "/" + def.getName();
            app.command(slashName, (req, ctx) -> {
                String text = req.getPayload().getText();
                String userId = req.getPayload().getUserId();
                String channelId = req.getPayload().getChannelId();

                IMessageReceived message = new SlackSlashCommandMessage(
                        userId, channelId, req.getPayload().getTeamId(), text
                );
                SlackResponseBuilder responseBuilder = new SlackResponseBuilder(
                        ctx.client(), channelId, userId
                );
                new CommandInterpreter().run(message, responseBuilder);
                responseBuilder.sendResponse();
                return ctx.ack();
            });
        }
    }

    @Override
    public void start() {
        try {
            AppConfig config = new AppConfig();
            config.setSingleTeamBotToken(botToken);

            app = new App(config);

            app.event(MessageEvent.class, (payload, ctx) -> {
                MessageEvent event = payload.getEvent();
                // Skip bot messages
                if (event.getBotId() != null || "bot_message".equals(event.getSubtype())) {
                    return ctx.ack();
                }

                SlackMessageReceived message = new SlackMessageReceived(event);
                SlackResponseBuilder responseBuilder = new SlackResponseBuilder(
                        ctx.client(), event.getChannel(), event.getUser()
                );
                new CommandInterpreter().run(message, responseBuilder);
                responseBuilder.sendResponse();
                return ctx.ack();
            });

            // Resolve the bot's own user ID for self-mention
            try {
                com.slack.api.methods.response.auth.AuthTestResponse authTest =
                        app.client().authTest(r -> r.token(botToken));
                if (authTest.isOk()) {
                    selfMention = "<@" + authTest.getUserId() + ">";
                }
            } catch (Exception e) {
                System.err.println("Warning: could not resolve bot self-mention: " + e.getMessage());
            }

            socketModeApp = new SocketModeApp(appToken, app);
            socketModeApp.startAsync();
            System.out.println("Slack adapter started (Socket Mode)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Slack adapter", e);
        }
    }

    @Override
    public void stop() {
        if (socketModeApp != null) {
            try {
                socketModeApp.stop();
            } catch (Exception e) {
                System.err.println("Error stopping Slack adapter: " + e.getMessage());
            }
        }
    }
}
