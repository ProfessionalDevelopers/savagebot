#!/bin/sh
set -e

ARGS=""

if [ "$SAVAGEBOT_PLATFORM" = "slack" ]; then
  ARGS="slack ${SAVAGEBOT_PASSWORD:-changeme} ${SLACK_BOT_TOKEN:?SLACK_BOT_TOKEN is required} ${SLACK_APP_TOKEN:?SLACK_APP_TOKEN is required}"
else
  ARGS="${SAVAGEBOT_PASSWORD:-changeme} ${DISCORD_TOKEN:?DISCORD_TOKEN is required}"
fi

if [ -n "$REDIS_HOST" ]; then
  ARGS="$ARGS ${REDIS_HOST} ${REDIS_PORT:-6379} ${REDIS_PASSWORD:-dummyPass}"
fi

exec java -jar savagebot.jar $ARGS
