package org.alessio29.savagebot.internal;

import java.util.Collection;

public interface PlatformAdapter {
    /** Human-readable platform name, e.g. "Discord", "Slack" */
    String getPlatformName();

    /** How many workspaces/guilds is this adapter connected to? */
    int getConnectedWorkspaceCount();

    /** The self-mention string the bot responds to, or null if not applicable */
    String getSelfMention();

    /** Register slash/shortcut commands with the platform, if supported */
    void registerSlashCommands(Collection<SlashCommandDefinition> commands);

    /** Start the adapter (connect, listen for events) */
    void start();

    /** Stop the adapter */
    void stop();
}
