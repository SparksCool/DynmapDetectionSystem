package org.dominion.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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

public class DiscordListener extends ListenerAdapter {

    DynmapParser dynmapParser = Main.getDynmapParser();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        // Only accept commands from guilds
        if (event.getGuild() == null)
            return;
        switch (event.getName()) {
            case "reloadconfig" -> {
                Main.checkAndCreateJsonConfig("./config.json", "{}");
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
                    List<String> knownThreats = Main.getConfigStringList("threatsList");
                    List<String> unknownPlayers = new ArrayList<>();
                    List<String> membersInLands = new ArrayList<>();
                    List<String> threatsInLands = new ArrayList<>();

                    for (String player : dynmapParser.getPlayersInAllNationLands(nationName)) {
                        if (onlineMembers.toString().contains(player)) {
                            membersInLands.add(player);
                        }
                        else if (knownThreats.toString().contains(player)) {
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
                boolean alreadyExists = false;
                while (i < Main.getConfig().getJSONArray("whitelisted").length()) {

                    if (Main.getConfig().getJSONArray("whitelisted").getString(i).equals(playerName)) {
                        Main.getConfig().getJSONArray("whitelisted").remove(i);
                        alreadyExists = true;
                    }

                    i++;
                }

                if (!alreadyExists) {
                    Main.getConfig().getJSONArray("whitelisted").put(playerName);
                }

                try {
                    Files.write(Paths.get("./config.json"), Main.getConfig().toString(4).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Main.checkAndCreateJsonConfig("./config.json", "{}");

                event.reply(alreadyExists ? "``Removed " + playerName +" to whitelist!``" : "``Added " + playerName +" from whitelist!``" ).queue();
            }

        }
    }
}
