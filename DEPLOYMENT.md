# Deployment

SavageBot is a Java application that connects to Discord or Slack (one platform per process) and uses Redis for persistent game state. It runs as a single fat JAR with no inbound network connections required.

## Architecture

```
┌──────────┐      ┌──────────────┐      ┌───────┐
│ Discord / │◄────►│  SavageBot   │◄────►│ Redis │
│  Slack    │ WSS  │  (Java JAR)  │ TCP  │       │
└──────────┘      └──────────────┘      └───────┘
```

- **Bot process** — connects outbound to Discord or Slack over WebSocket. No open ports needed.
- **Redis** — stores card decks, initiative hands, character data, command prefixes, and channel configs. Without Redis the bot still works for dice rolling, but card/initiative/character state won't persist across restarts.

## Quick start (Docker Compose)

1. Copy the example files and fill in your tokens:

   ```
   cp docker-compose.example.yml docker-compose.yml
   cp .env.example .env
   ```

2. Edit `.env` with your platform tokens (see [Environment variables](#environment-variables)).

3. Start everything:

   ```
   docker compose up -d
   ```

3. Check logs:

   ```
   docker compose logs -f bot
   ```

The compose file starts Redis with disk persistence (`--save 60 1`) so game state survives container restarts.

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `SAVAGEBOT_PLATFORM` | yes | `discord` or `slack` |
| `SAVAGEBOT_PASSWORD` | yes | Admin password for `!shutdown` and similar commands |
| `DISCORD_TOKEN` | for Discord | Bot token from the [Discord Developer Portal](https://discord.com/developers/applications) |
| `SLACK_BOT_TOKEN` | for Slack | Bot User OAuth Token (`xoxb-...`) from [Slack API](https://api.slack.com/apps) |
| `SLACK_APP_TOKEN` | for Slack | App-Level Token (`xapp-...`) with `connections:write` scope |

## Platform setup

### Discord

1. Create an application at https://discord.com/developers/applications
2. Go to **Bot** and create a bot user. Copy the token.
3. Under **OAuth2 > URL Generator**, select the `bot` scope and generate an invite link.
4. Invite the bot to your server using the link.
5. Set `SAVAGEBOT_PLATFORM=discord` and `DISCORD_TOKEN` in `.env`.

### Slack

SavageBot connects to Slack via **Socket Mode** (outbound WebSocket, no public URL needed).

1. Create a Slack app at https://api.slack.com/apps. You can use `slack-app-manifest.yml` in this repo as a starting point — paste its contents into **App Configuration > App Manifest**.
2. Enable **Socket Mode** under **Settings > Socket Mode**. Generate an app-level token with the `connections:write` scope. This is your `SLACK_APP_TOKEN` (`xapp-...`).
3. Install the app to your workspace. Copy the **Bot User OAuth Token** from **OAuth & Permissions**. This is your `SLACK_BOT_TOKEN` (`xoxb-...`).
4. Set `SAVAGEBOT_PLATFORM=slack` and both Slack tokens in `.env`.
5. Invite the bot to channels with `/invite @savagebot`.

## Building without Docker

Requirements: JDK 8+ and Maven 3.6+.

```
mvn clean compile assembly:single -DskipTests
```

This produces `target/savagebot-0.2.0-SNAPSHOT-jar-with-dependencies.jar`.

### Running directly

**Discord:**
```
java -jar target/savagebot-*-jar-with-dependencies.jar \
  <password> <discord-token> [redisHost redisPort redisPass] [debug]
```

**Slack:**
```
java -jar target/savagebot-*-jar-with-dependencies.jar slack \
  <password> <bot-token> <app-token> [redisHost redisPort redisPass] [debug]
```

- Use `dummyPass` as the Redis password to connect without authentication.
- Append `debug` as the last argument to enable the debug command prefix (`~` instead of `!`).

## Redis

Redis stores persistent game state: card decks, dealt hands, character data, custom prefixes, and channel configuration. All data is stored as Redis hashes.

Without Redis, dice rolling and other stateless commands work fine, but card/initiative/character state resets when the bot restarts.

Any Redis 6+ instance works. The Docker Compose setup includes Redis with periodic RDB snapshots so data survives restarts.

## Running both platforms

To run on Discord and Slack simultaneously, run two bot processes (or two compose stacks) pointed at the same Redis instance. Game state is keyed by guild/team + channel, so there's no conflict.
