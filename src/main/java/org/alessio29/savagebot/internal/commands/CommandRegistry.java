package org.alessio29.savagebot.internal.commands;

import org.alessio29.savagebot.commands.*;
import org.alessio29.savagebot.internal.SlashCommandDefinition;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CommandRegistry {

	private static final CommandRegistry INSTANCE = new CommandRegistry();

	private final Map<String, ICommand> registeredCommands = new HashMap<>();
	private final List<IParsingCommand> parsingCommands = new ArrayList<>();
	private final Map<String, IDiscordCommand> discordCommands = new HashMap<>();
	private final List<SlashCommandDefinition> slashCommandDefinitions = new ArrayList<>();

	public void registerCommand(ICommand newCommand) {
		if (newCommand.getAliases() != null) {
			for (String alias : newCommand.getAliases()) {
				registeredCommands.put(alias, newCommand);
			}
		}
		if (newCommand instanceof IParsingCommand) {
			registerParsingCommand((IParsingCommand) newCommand);
		}
		registeredCommands.put(newCommand.getName(), newCommand);
	}

	private void registerParsingCommand(IParsingCommand newCommand) {
		parsingCommands.add(newCommand);
	}

	public void registerCommandsFromStaticMethods(Class<?> methodClass) {
		registerCommandsFromMethods(null, methodClass);
	}

	private static String[] getOptionNames(DiscordCommandCallback cb) {
		DiscordOption[] options = cb.options();
		int numOptions = options.length;
		if (cb.varargOptionName().length() > 0) {
			numOptions += 1;
		}
		String[] optionNames = new String[numOptions];
		for (int i = 0; i < options.length; ++i) {
			optionNames[i] = options[i].name();
		}
		if (cb.varargOptionName().length() > 0) {
			optionNames[options.length] = cb.varargOptionName();
		}
		return optionNames;
	}

	public void registerCommandsFromMethods(Object methodOwner, Class<?> methodClass) {
		boolean shouldBeStatic = methodOwner == null;

		CommandCategoryOwner commandCategoryAnn = methodClass.getDeclaredAnnotation(CommandCategoryOwner.class);
		CommandCategory classCommandCategory = commandCategoryAnn != null ? commandCategoryAnn.value() : null;

		for (Method method : methodClass.getDeclaredMethods()) {
			if (shouldBeStatic != Modifier.isStatic(method.getModifiers())) continue;

			CommandCallback commandAnn = method.getDeclaredAnnotation(CommandCallback.class);
			if (commandAnn != null) {
				registerCommand(new MethodCommand(
						commandAnn.name(),
						classCommandCategory != null ? classCommandCategory : commandAnn.category(),
						commandAnn.description(),
						commandAnn.aliases(),
						commandAnn.arguments(),
						methodOwner,
						method
				));
			}

			ParsingCommandCallback parsingCommandAnn = method.getDeclaredAnnotation(ParsingCommandCallback.class);
			if (parsingCommandAnn != null) {
				registerParsingCommand(new ParsingMethodCommand(methodOwner, method));
			}

			DiscordCommandCallback discordCommandCallback = method.getDeclaredAnnotation(DiscordCommandCallback.class);
			if (discordCommandCallback != null) {
				discordCommands.put(
						discordCommandCallback.name(),
						createDiscordMethodSlashCommand(methodOwner, method, discordCommandCallback));
				slashCommandDefinitions.add(buildSlashCommandDefinition(discordCommandCallback));
			}
		}
	}

	@NotNull
	private DiscordMethodSlashCommand createDiscordMethodSlashCommand(
			Object methodOwner,
			Method method,
			DiscordCommandCallback discordCommandCallback) {
		return new DiscordMethodSlashCommand(
				discordCommandCallback.name(),
				method, methodOwner,
				discordCommandCallback.shouldDefer(),
				getOptionNames(discordCommandCallback),
				discordCommandCallback.varargOptionName().length() > 0);
	}

	private static SlashCommandDefinition buildSlashCommandDefinition(DiscordCommandCallback dc) {
		List<SlashCommandDefinition.Option> options = new ArrayList<>();
		for (DiscordOption opt : dc.options()) {
			options.add(new SlashCommandDefinition.Option(
					opt.name(),
					opt.description(),
					SlashCommandDefinition.OptionType.STRING,
					opt.isRequired()
			));
		}
		return new SlashCommandDefinition(
				dc.name(),
				dc.description(),
				options,
				dc.shouldDefer(),
				dc.varargOptionName(),
				dc.varargOptionDescription()
		);
	}

	public List<SlashCommandDefinition> getSlashCommandDefinitions() {
		return Collections.unmodifiableList(slashCommandDefinitions);
	}

	public static CommandRegistry getInstance() {
		return INSTANCE;
	}

	public Collection<ICommand> getRegisteredCommands() {
		return registeredCommands.values();
	}

	public Collection<IParsingCommand> getRegisteredParsingCommands() {
		return parsingCommands;
	}

	public ICommand getCommandByName(String command) {
		return registeredCommands.get(command);
	}

	public void reset() {
		registeredCommands.clear();
		parsingCommands.clear();
	}

	public IDiscordCommand getDiscordCommand(String commandName) {
		return discordCommands.get(commandName);
	}
}
