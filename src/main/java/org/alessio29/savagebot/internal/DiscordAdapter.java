package org.alessio29.savagebot.internal;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DiscordAdapter implements PlatformAdapter {

    private final String token;
    private ShardManager shardManager;

    public DiscordAdapter(String token) {
        this.token = token;
    }

    @Override
    public String getPlatformName() {
        return "Discord";
    }

    @Override
    public int getConnectedWorkspaceCount() {
        if (shardManager == null) {
            return 0;
        }
        int count = 0;
        for (JDA jda : shardManager.getShards()) {
            count += jda.getGuilds().size();
        }
        return count;
    }

    @Override
    public String getSelfMention() {
        if (shardManager == null) {
            return null;
        }
        List<JDA> shards = shardManager.getShards();
        if (shards.isEmpty()) {
            return null;
        }
        return shards.get(0).getSelfUser().getAsMention();
    }

    @Override
    public void registerSlashCommands(Collection<SlashCommandDefinition> commands) {
        if (shardManager == null || commands.isEmpty()) {
            return;
        }
        List<SlashCommandData> commandDataList = new ArrayList<>();
        for (SlashCommandDefinition def : commands) {
            commandDataList.add(toSlashCommandData(def));
        }
        // Register on the first shard (global commands propagate across all shards)
        List<JDA> shards = shardManager.getShards();
        if (!shards.isEmpty()) {
            shards.get(0).updateCommands().addCommands(commandDataList).queue(
                    registered -> {
                        for (Command command : registered) {
                            System.out.println("Registered /-command: " + command.getName());
                        }
                    },
                    throwable -> {
                        throw new RuntimeException("/-command registration failed", throwable);
                    }
            );
        }
    }

    @Override
    public void start() {
        try {
            shardManager = DefaultShardManagerBuilder.createDefault(token)
                    .addEventListeners(new ParseInputListener(), new DiscordSlashCommandListener())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Discord adapter", e);
        }
    }

    @Override
    public void stop() {
        if (shardManager != null) {
            shardManager.shutdown();
        }
    }

    private static SlashCommandData toSlashCommandData(SlashCommandDefinition def) {
        SlashCommandData slash = Commands.slash(def.getName(), def.getDescription());
        List<SlashCommandDefinition.Option> options = def.getOptions();
        int numOptions = options.size();
        if (def.getVarargOptionName().length() > 0) {
            numOptions += 1;
        }
        if (numOptions > 0) {
            OptionData[] optionData = new OptionData[numOptions];
            for (int i = 0; i < options.size(); ++i) {
                SlashCommandDefinition.Option opt = options.get(i);
                optionData[i] = new OptionData(
                        toJdaOptionType(opt.getType()),
                        opt.getName(),
                        opt.getDescription()
                );
            }
            if (def.getVarargOptionName().length() > 0) {
                optionData[options.size()] = new OptionData(
                        OptionType.STRING,
                        def.getVarargOptionName(),
                        def.getVarargOptionDescription()
                );
            }
            slash.addOptions(optionData);
        }
        return slash;
    }

    private static OptionType toJdaOptionType(SlashCommandDefinition.OptionType type) {
        switch (type) {
            case STRING:
            default:
                return OptionType.STRING;
        }
    }
}
