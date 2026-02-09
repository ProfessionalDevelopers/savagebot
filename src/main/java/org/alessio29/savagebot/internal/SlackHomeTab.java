package org.alessio29.savagebot.internal;

import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Slack App Home tab view — a Block Kit rendering of the
 * SavageBot user guide (ported from README.md).
 */
public class SlackHomeTab {

    public static View buildView() {
        List<LayoutBlock> blocks = new ArrayList<>();

        // ── Welcome ──
        blocks.add(header("SavageBot — Dice Roller & Game Aide"));
        blocks.add(section(
                "Bot for playing tabletop RPG online. Supports dice rolling, " +
                "Savage Worlds initiative cards, bennies, tokens, states, and more.\n\n" +
                "All commands start with `!` by default. " +
                "You can also use slash commands for initiative (like `/deal` or `/fight`)."
        ));
        blocks.add(divider());

        // ── Getting Started ──
        blocks.add(header("Getting Started"));
        blocks.add(section(
                "*Check if the bot is alive:*\n" +
                "```!ping```\n" +
                "*Change command prefix* (if `!` conflicts with another bot):\n" +
                "```!prefix #```\n" +
                "*Get help:*\n" +
                "```!help```"
        ));
        blocks.add(divider());

        // ── Simple Rolls ──
        blocks.add(header("Dice Rolling"));
        blocks.add(section(
                "*Basic rolls* — use `!r` or `!roll`. No acing.\n" +
                "```!r d12\n" +
                "!r 2d6+d4+2\n" +
                "!6x4d6k3```\n" +
                "You can roll any die: `!d20`, `!d100`, even `!d73`. " +
                "Modifiers work inline: `!6+d6`."
        ));

        // ── Savage Worlds Trait Rolls ──
        blocks.add(section(
                "*Extra trait rolls* — acing, no Wild Die. Use `!e`.\n" +
                "```!e6          → Fighting d6\n" +
                "!e8-2        → Shooting d8 with -2 penalty\n" +
                "!e4-2        → Untrained```\n" +
                "Shows success/raise count automatically."
        ));
        blocks.add(section(
                "*Wild Card trait rolls* — trait die + Wild Die, keep highest. Use `!s`.\n" +
                "```!s8          → Persuasion d8\n" +
                "!s4-2        → Untrained Wild Card```\n" +
                "Shows both dice, detects Snake Eyes.\n" +
                "```> s8: [4; w5] = 5 (success)```"
        ));

        // ── Damage ──
        blocks.add(section(
                "*Damage rolls* — sum all dice. Add `!` for acing.\n" +
                "```!d6!           → Exploding d6\n" +
                "!d6!+d4!       → Str d6 + knife d4\n" +
                "!2d6!          → Bow damage\n" +
                "!2d6!+2        → Bow + smite```"
        ));
        blocks.add(divider());

        // ── Advanced Dice ──
        blocks.add(header("Advanced Rolls"));
        blocks.add(section(
                "*Variable Target Number* — add `t` for non-standard TN:\n" +
                "```!s10t6      → Fighting d10 vs Parry 6\n" +
                "!2d6!t6     → Damage vs Toughness 6```\n" +
                "*Multiple modifiers:*\n" +
                "```!s10-4-2+1+2   → Called Shot, Dim, Trademark, Wild Attack```"
        ));
        blocks.add(section(
                "*Custom Wild Die* — use `w` for Master edge etc:\n" +
                "```!s12w10+2     → d12+2 with d10 Wild Die```\n" +
                "*Multi-dice* — Rate of Fire, Frenzy, etc. Prefix with count:\n" +
                "```!2e6t5        → Frenzy with d6 Fighting vs Parry 5\n" +
                "!2s10         → Work the Crowd, 2 Support rolls```"
        ));
        blocks.add(section(
                "*Repeat rolls* — run the same roll multiple times with `x`:\n" +
                "```!6x4d6k3     → D&D stat generation\n" +
                "!5xe6         → 5 extras rolling d6 trait```\n" +
                "*Keep highest/lowest:*\n" +
                "```!4d6k3       → Roll 4d6, keep best 3\n" +
                "!5d10kl2     → Roll 5d10, keep lowest 2\n" +
                "!d20adv+2    → Advantage + modifier\n" +
                "!d20dis      → Disadvantage```"
        ));
        blocks.add(section(
                "*Inline narration* — mix text and rolls on one line:\n" +
                "```The assassin shoots !s8 Damage: !2d6!+1 Bonus: !d6!```\n" +
                "*Roll statistics:*\n" +
                "```!rh 1000x2d6    → Distribution histogram of 2d6```\n" +
                "*Custom raise step:*\n" +
                "```!s8r6          → Raises in steps of 6 instead of 4```"
        ));
        blocks.add(divider());

        // ── Initiative ──
        blocks.add(header("Savage Worlds Initiative"));
        blocks.add(section(
                "*Start a fight* — shuffles deck, resets tracker:\n" +
                "```!fight```\n" +
                "*Deal initiative cards:*\n" +
                "```!di Huey Dewey Bandits Wolves```\n" +
                "Edge/Hindrance modifiers follow the character name:\n" +
                "`-q` Quick · `-l` Level Headed · `-i` Imp. Level Headed · `-h` Hesitant\n" +
                "```!di Huey -q Dewey Assassin -i Bandits -h```"
        ));
        blocks.add(section(
                "*Show initiative tracker:*\n" +
                "```!init```\n" +
                "*New round* — clears cards, reshuffles if Joker was dealt. " +
                "Add `+` to keep characters and re-deal:\n" +
                "```!rd +```\n" +
                "*Extra card* (Tactician, benny spend, etc):\n" +
                "```!card Dewey```"
        ));
        blocks.add(section(
                "*Remove from fight:*\n" +
                "```!drop Huey Bandits```\n" +
                "Or remove during new round with `-`:\n" +
                "```!rd + -Bandits```\n" +
                "*Put on hold / return:*\n" +
                "```!hold LastActionHero\n" +
                "!hold -LastActionHero```"
        ));
        blocks.add(divider());

        // ── Bennies ──
        blocks.add(header("Bennies"));
        blocks.add(section(
                "*Give bennies:*\n" +
                "```!gb Huey          → Give 1\n" +
                "!gb Huey 3        → Give 3\n" +
                "!gb Huey 2 Louie Dewey 3   → Multiple characters```\n" +
                "*Take/spend bennies:*\n" +
                "```!tb Huey\n" +
                "!tb Huey Louie 2 Dewey 2```"
        ));
        blocks.add(section(
                "*See who has what:*\n" +
                "```!list```\n" +
                "Shows bennies and states for all characters.\n\n" +
                "*Clear all bennies:* `!cb`\n" +
                "*Remove character:* `!remove Assassin`\n" +
                "*Remove multiple:* `!remove LordDenak GoblinWarlord`"
        ));
        blocks.add(section(
                "*Deadlands colored bennies:*\n" +
                "Switch mode: `!sbm deadlands` (back with `!sbm normal`)\n" +
                "Colors: `w` white · `b` blue · `r` red · `g` golden\n" +
                "```!gb Dewey w 2b g     → 1 white, 2 blue, 1 golden\n" +
                "!tb Huey 2w r         → Take 2 white, 1 red\n" +
                "!ib                   → Initialize benny pool\n" +
                "!pb GM 3              → Pull 3 from pool```"
        ));
        blocks.add(divider());

        // ── Characters & States ──
        blocks.add(header("Characters & States"));
        blocks.add(section(
                "Characters are added automatically when you deal them a card, benny, or state. " +
                "Use `!list` to see all, `!remove` to clean up.\n\n" +
                "*Apply states:*\n" +
                "```!state Huey +Vulnerable\n" +
                "!state Huey +stn Dewey +dis Louie +bnd```\n" +
                "Short codes: `sha` Shaken · `stn` Stunned · `ent` Entangled · " +
                "`bnd` Bound · `dis` Distracted · `vul` Vulnerable"
        ));
        blocks.add(section(
                "*Remove states:*\n" +
                "```!state Huey -vul\n" +
                "!state Huey clear       → Remove all states```\n" +
                "*Mix apply/remove:*\n" +
                "```!state Huey +stunned -vul dis Dewey dis -ent Louie clear```"
        ));
        blocks.add(divider());

        // ── Other Systems ──
        blocks.add(header("Other Systems"));
        blocks.add(section(
                "*West End Games D6* — one d6 is Wild (explodes on 6, subtracts on 1):\n" +
                "```!3w   → 3w: w11 + 6 + 4 = 21```\n" +
                "*World of Darkness* — count successes at a target:\n" +
                "```!6d6s4           → 6d6, success on 4+\n" +
                "!12d10f1s7       → 12d10, success 7+, failure 1```\n" +
                "*Fate/Fudge:*\n" +
                "```!4dF```"
        ));
        blocks.add(section(
                "*D&D advantage/disadvantage:*\n" +
                "```!d20adv+2       → Roll with advantage\n" +
                "!d20dis+2       → Roll with disadvantage```\n" +
                "*D&D sorted initiative:*\n" +
                "```!rs Huey d20 Dewey d20-2 Louie d20+4```\n" +
                "*Coriolis:* `!d66` · *Carcosa:* `!dC`"
        ));
        blocks.add(divider());

        // ── Cards ──
        blocks.add(header("Cards"));
        blocks.add(section(
                "Standard 54-card deck commands:\n" +
                "```!deal 5       → Deal 5 cards to yourself (DM)\n" +
                "!show         → Show your hand\n" +
                "!put 3        → Draw 3 cards face-up to table\n" +
                "!shuffle      → Collect all cards, reshuffle```"
        ));

        return View.builder()
                .type("home")
                .blocks(blocks)
                .build();
    }

    private static HeaderBlock header(String text) {
        return HeaderBlock.builder()
                .text(PlainTextObject.builder().text(text).build())
                .build();
    }

    private static SectionBlock section(String markdown) {
        return SectionBlock.builder()
                .text(MarkdownTextObject.builder().text(markdown).build())
                .build();
    }

    private static DividerBlock divider() {
        return new DividerBlock();
    }
}
