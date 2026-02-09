package org.alessio29.savagebot.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.socket_mode.SocketModeClient;
import com.slack.api.socket_mode.response.AckResponse;
import com.slack.api.util.json.GsonFactory;
import org.alessio29.savagebot.internal.builders.SlackResponseBuilder;
import org.alessio29.savagebot.internal.commands.CommandInterpreter;

import java.util.Collection;

public class SlackAdapter implements PlatformAdapter {

    private final String botToken;
    private final String appToken;
    private SocketModeClient client;
    private MethodsClient methodsClient;
    private Thread keepAlive;
    private String selfMention;
    private String selfUserId;
    private final Gson gson = GsonFactory.createSnakeCase(Slack.getInstance().getConfig());

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
        return client != null ? 1 : 0;
    }

    @Override
    public String getSelfMention() {
        return selfMention;
    }

    @Override
    public void registerSlashCommands(Collection<SlashCommandDefinition> commands) {
        // Slash commands are handled in the WebSocket message listener
        // via the envelope type "slash_commands". No pre-registration needed.
    }

    @Override
    public void start() {
        try {
            Slack slack = Slack.getInstance();
            methodsClient = slack.methods(botToken);

            // Resolve the bot's own user ID for self-mention
            try {
                com.slack.api.methods.response.auth.AuthTestResponse authTest =
                        methodsClient.authTest(r -> r);
                if (authTest.isOk()) {
                    selfUserId = authTest.getUserId();
                    selfMention = "<@" + selfUserId + ">";
                    System.out.println("Authenticated as: " + authTest.getUserId()
                            + " (" + authTest.getBotId() + ") in team " + authTest.getTeamId());
                } else {
                    System.err.println("auth.test failed: " + authTest.getError());
                }
            } catch (Exception e) {
                System.err.println("Warning: could not resolve bot self-mention: " + e.getMessage());
            }

            client = slack.socketMode(appToken);
            client.addWebSocketMessageListener(this::handleWebSocketMessage);
            client.addWebSocketErrorListener(throwable -> {
                System.err.println("[Slack] WebSocket error: " + throwable.getMessage());
            });
            client.setAutoReconnectEnabled(true);
            client.connect();

            System.out.println("Slack adapter started (Socket Mode)");

            // Keep the JVM alive with a non-daemon thread
            keepAlive = new Thread(() -> {
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException e) {
                    // shutdown
                }
            }, "slack-keepalive");
            keepAlive.setDaemon(false);
            keepAlive.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Slack adapter", e);
        }
    }

    private void handleWebSocketMessage(String rawMessage) {
        try {
            JsonObject envelope = JsonParser.parseString(rawMessage).getAsJsonObject();
            String type = envelope.has("type") ? envelope.get("type").getAsString() : "";
            String envelopeId = envelope.has("envelope_id")
                    ? envelope.get("envelope_id").getAsString() : null;

            if ("events_api".equals(type)) {
                // Acknowledge immediately so Slack doesn't retry
                if (envelopeId != null) {
                    client.sendSocketModeResponse(new AckResponse(envelopeId));
                }
                handleEventCallback(envelope);
            } else if ("slash_commands".equals(type)) {
                handleSlashCommand(envelope, envelopeId);
            }
            // Ignore "hello", "disconnect", etc.
        } catch (Exception e) {
            System.err.println("[Slack] Error handling message: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void handleEventCallback(JsonObject envelope) {
        try {
            JsonObject payload = envelope.getAsJsonObject("payload");
            JsonObject eventObj = payload.getAsJsonObject("event");
            String eventType = eventObj.has("type") ? eventObj.get("type").getAsString() : "";

            if ("app_home_opened".equals(eventType)) {
                String userId = eventObj.get("user").getAsString();
                publishHomeTab(userId);
                return;
            }

            if (!"message".equals(eventType)) {
                return;
            }

            // Skip bot messages
            if (eventObj.has("bot_id") && !eventObj.get("bot_id").isJsonNull()) {
                return;
            }
            JsonElement subtype = eventObj.get("subtype");
            if (subtype != null && !subtype.isJsonNull() && "bot_message".equals(subtype.getAsString())) {
                return;
            }

            // Skip messages from Slackbot (user ID USLACKBOT)
            String userId = eventObj.has("user") ? eventObj.get("user").getAsString() : null;
            if ("USLACKBOT".equals(userId)) {
                return;
            }

            // Skip thread replies (messages with thread_ts that differs from ts)
            if (eventObj.has("thread_ts") && !eventObj.get("thread_ts").isJsonNull()) {
                String threadTs = eventObj.get("thread_ts").getAsString();
                String ts = eventObj.has("ts") ? eventObj.get("ts").getAsString() : "";
                if (!threadTs.equals(ts)) {
                    return;
                }
            }

            MessageEvent event = gson.fromJson(eventObj, MessageEvent.class);
            System.out.println("[Slack] Message from " + event.getUser()
                    + " in " + event.getChannel() + ": " + event.getText());

            SlackMessageReceived message = new SlackMessageReceived(event);
            SlackResponseBuilder responseBuilder = new SlackResponseBuilder(
                    methodsClient, event.getChannel(), event.getUser()
            );
            new CommandInterpreter().run(message, responseBuilder);
            responseBuilder.sendResponse();
        } catch (Exception e) {
            System.err.println("[Slack] Error handling event: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void handleSlashCommand(JsonObject envelope, String envelopeId) {
        try {
            JsonObject payload = envelope.getAsJsonObject("payload");
            String command = payload.has("command") ? payload.get("command").getAsString() : "";
            String text = payload.has("text") ? payload.get("text").getAsString() : "";
            String userId = payload.has("user_id") ? payload.get("user_id").getAsString() : "";
            String channelId = payload.has("channel_id") ? payload.get("channel_id").getAsString() : "";
            String teamId = payload.has("team_id") ? payload.get("team_id").getAsString() : "";

            System.out.println("[Slack] Slash command: " + command + " " + text
                    + " from " + userId + " in " + channelId);

            // Build the raw text as "command text" for the interpreter
            String rawText = (command.startsWith("/") ? "!" + command.substring(1) : command)
                    + (text.isEmpty() ? "" : " " + text);

            IMessageReceived message = new SlackSlashCommandMessage(userId, channelId, teamId, rawText);
            SlackResponseBuilder responseBuilder = new SlackResponseBuilder(
                    methodsClient, channelId, userId
            );
            new CommandInterpreter().run(message, responseBuilder);
            responseBuilder.sendResponse();

            // Acknowledge the slash command (empty body = no ephemeral response)
            if (envelopeId != null) {
                client.sendSocketModeResponse(new AckResponse(envelopeId));
            }
        } catch (Exception e) {
            System.err.println("[Slack] Error handling slash command: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void publishHomeTab(String userId) {
        try {
            methodsClient.viewsPublish(r -> r
                    .userId(userId)
                    .view(SlackHomeTab.buildView())
            );
        } catch (Exception e) {
            System.err.println("[Slack] Error publishing home tab: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (keepAlive != null) {
            keepAlive.interrupt();
        }
        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                System.err.println("Error stopping Slack adapter: " + e.getMessage());
            }
        }
    }
}
