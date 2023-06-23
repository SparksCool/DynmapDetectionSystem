package org.dominion.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.dominion.Config;
import org.dominion.Main;
import org.dominion.dynmap.DynmapParser;
import org.dominion.logging.ConsoleColors;
import org.dominion.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.dominion.discord.DiscordBot.activeGuild;
public class DiscordListener extends ListenerAdapter {

    DynmapParser dynmapParser = Main.getDynmapParser();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        //Public commands
        switch (event.getName()) {
            case "link-account" -> {
                String MCAccount1 = event.getOption("mc-username").getAsString();
                String MCAccount2 = "";
                if (event.getOption("mc-username-2") != null) {
                    MCAccount2 = event.getOption("mc-username-2").getAsString();
                }
                String userID = event.getUser().getId();
                String userPrevNick = event.getMember().getNickname();
                int i = 0;
                boolean LinkedAccountAlreadyExists = false;
                while (i < Config.getConfig().getJSONArray("LinkedAccounts").length()) {
                    String[] splitAccount = Config.getConfig().getJSONArray("LinkedAccounts").getString(i).split(":");
                    if (splitAccount[0].equals(userID)) {
                        Config.getConfig().getJSONArray("LinkedAccounts").remove(i);
                        LinkedAccountAlreadyExists = true;
                    }

                    i++;
                }

                if (event.getOption("mc-username-2") == null){
                    Config.getConfig().getJSONArray("LinkedAccounts").put(userID+":"+MCAccount1);
                    event.getMember().modifyNickname(userPrevNick+" | "+ MCAccount1).queue();
                }else {
                    Config.getConfig().getJSONArray("LinkedAccounts").put(userID+":"+MCAccount1+":"+MCAccount2);
                    event.getMember().modifyNickname(userPrevNick+" | "+ MCAccount1+" | "+MCAccount2).queue();
                }
                try {
                    Files.write(Paths.get("./config.json"), Config.getConfig().toString(4).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Config.checkAndCreateJsonConfig();
                if (LinkedAccountAlreadyExists == false){
                    event.reply("``Linked Account!``").queue();

                } else {
                    event.reply("``Replaced Linked Account with new version``").queue();
                }
            }

            case "unlink-account" -> {
                int i = 0;
                int linkedAccounts = Config.getConfig().getJSONArray("LinkedAccounts").length();
                while (i < linkedAccounts) {
                    String[] splitAccount = Config.getConfig().getJSONArray("LinkedAccounts").getString(i).split(":");
                    if (splitAccount[0].equals(event.getUser().getId())) {
                        Config.getConfig().getJSONArray("LinkedAccounts").remove(i);
                    }
                    i++;

                    //on last one if still not then make a boolean true
                    if (i == linkedAccounts && !splitAccount[0].equals(event.getUser().getId())) {
                        event.reply("``You do not have a linked account!``").queue();
                        return;
                    }
                }
                String newNick = event.getMember().getNickname().split(" | ")[0];
                event.getMember().modifyNickname(newNick).queue();
                try {
                    Files.write(Paths.get("./config.json"), Config.getConfig().toString(4).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                event.reply("``Unlinked Account!``").queue();
            }

        }

        // Bot Operator Commands
        Role BOT_OPERATOR = activeGuild.getRoleById("1121141229409812562");

        if (event.getMember().getRoles().contains(BOT_OPERATOR)) {


            // Only accept commands from guilds
            if (event.getGuild() == null)
                return;
            switch (event.getName()) {
                case "reloadconfig" -> {
                    Config.checkAndCreateJsonConfig();
                    event.reply("``Config Reloaded!``").queue();
                }
                case "summary" -> {
                    String nationName = event.getOption("name").getAsString();
                    JSONObject nationData;
                    try {
                        nationData = dynmapParser.getDynmapNation(nationName);

                        if (nationData == null) {
                            event.reply("That is not a valid nation name!").queue();
                            return;
                        }

                        List<String> onlineMembers = new ArrayList<>(dynmapParser.getAllNationPlayers(nationName));
                        List<String> knownThreats = Config.getConfigStringList("threatsList");
                        List<String> unknownPlayers = new ArrayList<>();
                        List<String> membersInLands = new ArrayList<>();
                        List<String> threatsInLands = new ArrayList<>();

                        for (String player : dynmapParser.getPlayersInAllNationLands(nationName)) {
                            if (onlineMembers.toString().contains(player)) {
                                membersInLands.add(player);
                            } else if (knownThreats.toString().contains(player)) {
                                threatsInLands.add(player);
                            } else {
                                unknownPlayers.add(player);
                            }
                        }

                        EmbedBuilder embedBuilder = new EmbedBuilder();
                        embedBuilder.setTitle("Nation Summary");
                        embedBuilder.setColor(Color.decode(nationData.getString("color")));

                        embedBuilder.addField("Nation name:", nationData.getString("label"), false);
                        embedBuilder.addField("Online Members:", onlineMembers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                        embedBuilder.addField("Members near their lands:", membersInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                        embedBuilder.addField("Threats near their lands:", threatsInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                        embedBuilder.addField("Unknown Players near their lands:", unknownPlayers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);

                        embedBuilder.setFooter("Official Property of the Sunset Dominion.");

                        event.replyEmbeds(embedBuilder.build()).queue();

                        Logger.log(ConsoleColors.BLUE + nationName + ConsoleColors.YELLOW + " has been analyzed without error!");

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                case "whitelist" -> {
                    String playerName = event.getOption("name").getAsString();

                    int i = 0;
                    boolean whitelistAlreadyExists = false;
                    while (i < Config.getConfig().getJSONArray("whitelisted").length()) {

                        if (Config.getConfig().getJSONArray("whitelisted").getString(i).equals(playerName)) {
                            Config.getConfig().getJSONArray("whitelisted").remove(i);
                            whitelistAlreadyExists = true;
                        }

                        i++;
                    }

                    if (!whitelistAlreadyExists) {
                        Config.getConfig().getJSONArray("whitelisted").put(playerName);
                    }

                    try {
                        Files.write(Paths.get("./config.json"), Config.getConfig().toString(4).getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Config.checkAndCreateJsonConfig();

                    event.reply(whitelistAlreadyExists ? "``Removed " + playerName + " from whitelist!``" : "``Added " + playerName + " to whitelist!``").queue();
                }
                case "threatlist" -> {
                    String playerName = event.getOption("name").getAsString();

                    int i = 0;
                    boolean ThreatlistAlreadyExists = false;
                    while (i < Config.getConfig().getJSONArray("threatsList").length()) {

                        if (Config.getConfig().getJSONArray("threatsList").getString(i).equals(playerName)) {
                            Config.getConfig().getJSONArray("threatsList").remove(i);
                            ThreatlistAlreadyExists = true;
                        }

                        i++;
                    }

                    if (!ThreatlistAlreadyExists) {
                        Config.getConfig().getJSONArray("threatsList").put(playerName);
                    }

                    try {
                        Files.write(Paths.get("./config.json"), Config.getConfig().toString(4).getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    Config.checkAndCreateJsonConfig();

                    event.reply(ThreatlistAlreadyExists ? "``Removed " + playerName + " to Threats List!``" : "``Added " + playerName + " from Threats List!``").queue();
                }


            }
        } else {
            event.reply("You do not have permission for that :(").setEphemeral(true).queue();
        }
    }
}
