package org.alessio29.savagebot;

import org.alessio29.savagebot.cards.Decks;
import org.alessio29.savagebot.cards.Hands;
import org.alessio29.savagebot.characters.Characters;
import org.alessio29.savagebot.internal.*;
import org.alessio29.savagebot.internal.commands.CommandRegistry;
import org.alessio29.savagebot.internal.commands.Commands;

public class SavageBotRunner {

	private static String passwd;
	private static PlatformAdapter adapter;

	public static void main(String[] args) {

		if (args.length >= 1 && "slack".equalsIgnoreCase(args[0].trim())) {
			startSlack(stripFirst(args));
		} else {
			startDiscord(args);
		}
	}

	private static void startDiscord(String[] args) {
		if (args.length < 2) {
			System.out.println("Discord usage: password token [redisHost redisPort redisPass [debug]]");
			return;
		}
		passwd = args[0].trim();
		String token = args[1].trim();

		setupRedis(args, 2);
		setupDebug(args, 5);

		Commands.registerDefaultCommands();

		adapter = new DiscordAdapter(token);
		adapter.start();
		adapter.registerSlashCommands(CommandRegistry.getInstance().getSlashCommandDefinitions());

		SelfMentionContainer.initialize(adapter.getSelfMention());
	}

	private static void startSlack(String[] args) {
		if (args.length < 3) {
			System.out.println("Slack usage: slack password botToken appToken [redisHost redisPort redisPass [debug]]");
			return;
		}
		passwd = args[0].trim();
		String botToken = args[1].trim();
		String appToken = args[2].trim();

		setupRedis(args, 3);
		setupDebug(args, 6);

		Commands.registerDefaultCommands();

		adapter = new SlackAdapter(botToken, appToken);
		adapter.start();
		adapter.registerSlashCommands(CommandRegistry.getInstance().getSlashCommandDefinitions());

		SelfMentionContainer.initialize(adapter.getSelfMention());
	}

	private static void setupRedis(String[] args, int offset) {
		if (args.length >= offset + 3) {
			String host = args[offset].trim();
			int port = Integer.parseInt(args[offset + 1].trim());
			String pass = (args[offset + 2].equals("dummyPass")) ? null : args[offset + 2];
			RedisClient.setup(host, port, pass);
			Prefixes.loadFromRedis();
			Decks.loadFromRedis();
			Hands.loadFromRedis();
			Characters.loadFromRedis();
		} else {
			System.out.println("Starting without redis, some functionality is unavailable.");
		}
	}

	private static void setupDebug(String[] args, int index) {
		if (args.length > index) {
			String debug = args[index].trim();
			if (debug.equalsIgnoreCase("debug")) {
				Prefixes.setDebugPrefix();
			}
		}
	}

	private static String[] stripFirst(String[] args) {
		if (args.length <= 1) {
			return new String[0];
		}
		String[] result = new String[args.length - 1];
		System.arraycopy(args, 1, result, 0, result.length);
		return result;
	}

	public static boolean passwdOk(String str) {
		if (passwd == null || passwd.isEmpty()) {
			return false;
		}
		return passwd.equals(str);
	}

	public static PlatformAdapter getAdapter() {
		return adapter;
	}
}
