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

		if (args.length < 2) {
			System.out.println("Parameters must be provided: password token redisHost redisPort redisPass");
			return;
		}
		passwd = args[0].trim();
		String token = args[1].trim();


		if (args.length >= 5) {
			String host = args[2].trim();
			int port = Integer.parseInt(args[3].trim());
			String pass = (args[4].equals("dummyPass")) ? null : args[4];
			RedisClient.setup(host, port, pass);
			Prefixes.loadFromRedis();
			Decks.loadFromRedis();
			Hands.loadFromRedis();
			Characters.loadFromRedis();
		} else {
			System.out.println("Starting without redis, some functionality is unavailable.");
		}

		if (args.length >= 6) {
			String debug = args[5].trim();
			if (debug.equalsIgnoreCase("debug")) {
				Prefixes.setDebugPrefix();
			}
		}

		Commands.registerDefaultCommands();

		adapter = new DiscordAdapter(token);
		adapter.start();
		adapter.registerSlashCommands(CommandRegistry.getInstance().getSlashCommandDefinitions());

		SelfMentionContainer.initialize(adapter.getSelfMention());
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
