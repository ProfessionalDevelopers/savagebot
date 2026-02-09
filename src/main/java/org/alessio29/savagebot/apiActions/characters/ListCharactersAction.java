package org.alessio29.savagebot.apiActions.characters;

import org.alessio29.savagebot.bennies.BennyType;
import org.alessio29.savagebot.cards.Card;
import org.alessio29.savagebot.characters.Character;
import org.alessio29.savagebot.characters.Characters;
import org.alessio29.savagebot.internal.IMessageReceived;
import org.alessio29.savagebot.internal.builders.ReplyBuilder;
import org.alessio29.savagebot.internal.builders.TableData;
import org.alessio29.savagebot.internal.commands.CommandExecutionResult;
import org.alessio29.savagebot.internal.utils.ChannelConfigs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ListCharactersAction {

    public CommandExecutionResult doAction(IMessageReceived message, String[] args) {

        Collection<Character> chars = Characters.getCharacters(message.getGuildId(), message.getChannelId()).values();
        if (chars.isEmpty()) {
            return new CommandExecutionResult("No characters found!", 1);
        }
        BennyType bType = ChannelConfigs.getChannelConfig(message.getChannelId()).getBennyType();

        ReplyBuilder replyBuilder = new ReplyBuilder();

        String[] tableHeaders = {"NAME", "BENNIES", "CARD"};
        List<String[]> tableRows = new ArrayList<>();

        List<Character> sorted = chars.stream()
                .sorted(Comparator.comparing(c -> c.getName().toUpperCase()))
                .collect(Collectors.toList());

        for (Character chr : sorted) {
            String name = chr.getName().toUpperCase();
            String bennies = chr.getBennyValue(bType);
            Card bestCard = chr.getBestCard();
            String card = bestCard != null ? bestCard.toString() : "";

            tableRows.add(new String[]{name, bennies, card});
            replyBuilder.attach(name).attach("  ")
                    .attach("B:").attach(bennies).attach("  ")
                    .attach(card).newLine();
        }

        replyBuilder.newLine();
        TableData tableData = new TableData(tableHeaders, tableRows);
        return new CommandExecutionResult(replyBuilder.toString(), 2, tableData);
    }
}
