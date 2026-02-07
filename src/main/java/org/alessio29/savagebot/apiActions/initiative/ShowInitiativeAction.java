package org.alessio29.savagebot.apiActions.initiative;

import org.alessio29.savagebot.cards.Card;
import org.alessio29.savagebot.characters.Character;
import org.alessio29.savagebot.characters.Characters;
import org.alessio29.savagebot.initiative.Rounds;
import org.alessio29.savagebot.internal.IMessageReceived;
import org.alessio29.savagebot.internal.builders.ReplyBuilder;
import org.alessio29.savagebot.internal.builders.TableData;
import org.alessio29.savagebot.internal.commands.CommandExecutionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ShowInitiativeAction {

    public CommandExecutionResult doAction(IMessageReceived message, String[] args) {

        ReplyBuilder reply = new ReplyBuilder();
        Integer round = Rounds.getGuildRound(message.getGuildId(), message.getChannelId());
        Set<Character> chars = Characters.getFightingCharactersWithCards(message.getGuildId(), message.getChannelId());

        if (!chars.isEmpty()) {
            List<Character> sortedList = new ArrayList<>(chars);
            sortedList.sort((o1, o2) -> {
                int r = -Boolean.compare(o1.isOnHold(), o2.isOnHold());
                if (r == 0) {
                    return -o1.getBestCard().compareTo(o2.getBestCard());
                }
                return r;
            });

            String[] tableHeaders = {"NAME", "CARD"};
            List<String[]> tableRows = new ArrayList<>();

            for (Character c : sortedList) {
                String displayName = c.getName().toUpperCase();
                if (c.isOnHold()) {
                    displayName += " <H>";
                }

                tableRows.add(new String[]{
                        displayName,
                        c.getBestCard().toString()
                });

                reply.attach(displayName).attach("  ").attach(c.getBestCard().toString()).newLine();
            }

            String tableTitle = "Round " + round;
            reply.newLine();
            TableData tableData = new TableData(tableTitle, tableHeaders, tableRows);
            return new CommandExecutionResult(reply.toString(), args.length + 1, tableData);
        } else {
            reply.attach("No cards dealt!");
        }
        reply.newLine();
        return new CommandExecutionResult(reply.toString(), args.length + 1);
    }
}
