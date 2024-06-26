package org.dominion.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.dominion.Config;
import org.dominion.Main;
import org.dominion.dynmap.DynmapParser;
import org.dominion.logging.ConsoleColors;
import org.dominion.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;


public class DiscordBot extends ListenerAdapter {

    private final DynmapParser dynmapParser;
    public static Guild activeGuild;
    private final HashMap<String, Long> memberReportTimes;
    private JSONArray playerList;

    public DiscordBot() throws InterruptedException {

        dynmapParser = Main.getDynmapParser();

        memberReportTimes = new HashMap<>();

        // slash commands don't need any intents
        JDA jda = JDABuilder.createLight(Config.getConfigStringWithDefault("token", ""), EnumSet.noneOf(GatewayIntent.class)) // slash commands don't need any intents
                .addEventListeners(new DiscordListener())
                .build().awaitReady();

        activeGuild = jda.getGuildById(Config.getConfigStringWithDefault("guildID", "1084575613744066764"));

        // These commands might take a few minutes to be active after creation/update/delete
        CommandListUpdateAction commands = jda.updateCommands();
        // Add our dynmap commands

        commands.addCommands(
                Commands.slash("summary", "Gets a summary of this nations players")
                        .addOptions(new OptionData(STRING, "name", "Name of the nation to check.")
                                .setRequired(true))
                        .setGuildOnly(true)

                //everyone has perm

        );

        commands.addCommands(
                Commands.slash("whitelist", "Toggle whitelist of a player")
                        .addOptions(new OptionData(STRING, "name", "Name of the player to whitelist.")
                                .setRequired(true))
                        .setGuildOnly(true)
        );
        commands.addCommands(
                Commands.slash("whitelist-ally", "Toggle whitelist of a player")
                        .addOptions(new OptionData(STRING, "name", "Name of the player to whitelist.")
                                .setRequired(true))
                        .setGuildOnly(true)
        );

        commands.addCommands(
                Commands.slash("threatlist", "Toggle threat of a player")
                        .addOptions(new OptionData(STRING, "name", "Name of the player to put on threat list.",true))

                        .setGuildOnly(true)
        );


        commands.addCommands(
                Commands.slash("unlink-account", "Remove the link to your ign and reverts the nick")
                        .setGuildOnly(true)
        );
        commands.addCommands(
                Commands.slash("link-account", "Links discord to your ign")
                        .addOption(STRING,"mc-username","Your Minecraft username (Case sensitive)",true)
                        .addOption(STRING,"mc-username-2","Your Minecraft username Alt account (Case sensitive)",false)
                        .setGuildOnly(true)
        );

        commands.addCommands(
                Commands.slash("reloadconfig", "Reloads the defense systems config")
                        .setGuildOnly(true)
        );

        commands.queue();

    }

    public void sendDetectionUpdate() {
        TextChannel channel = activeGuild.getTextChannelById(Config.getConfigStringWithDefault("detectionChannel", "1084576582913499216"));

        JSONObject nationData;
        java.util.List<String> onlineMembers = new ArrayList<>();
        java.util.List<String> onlineAlliedMembers = new ArrayList<>();
        java.util.List<String> onlineThreatMembers = new ArrayList<>();


        for (String nationName : Config.getConfigStringList("nationsList")) {
            try {
                onlineMembers.addAll(dynmapParser.getAllNationPlayers(nationName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (String nationName : Config.getConfigStringList("alliesList")) {
            try {
                onlineAlliedMembers.addAll(dynmapParser.getAllNationPlayers(nationName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (String nationName : Config.getConfigStringList("threatNations")) {
            try {
                onlineThreatMembers.addAll(dynmapParser.getAllNationPlayers(nationName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (String nationName : Config.getConfigStringList("nationsList")) {
            try {
                nationData = dynmapParser.getDynmapNation(nationName);

                if (nationData == null) {
                    throw new RuntimeException(nationName + " has returned null!");
                }

                java.util.List<String> knownThreats = Config.getConfigStringList("threatsList");
                java.util.List<String> unknownPlayers = new ArrayList<>();
                java.util.List<String> membersInLands = new ArrayList<>();
                java.util.List<String> alliesInLands = new ArrayList<>();

                List<String> threatsInLands = new ArrayList<>();

                for (String player : dynmapParser.getPlayersInAllNationLands(nationName)) {
                    if (onlineMembers.toString().contains(player) || Config.getConfigStringList("whitelisted").contains(player)) {
                        membersInLands.add(player);
                    }
                    else if (memberReportTimes == null || (memberReportTimes.get(player) != null && (((System.currentTimeMillis() - memberReportTimes.get(player)) / 1000 / 60 != 15)))) {
                        Logger.log(ConsoleColors.GREEN + player + ConsoleColors.BLUE + " must wait " + (15 - ((System.currentTimeMillis() - memberReportTimes.get(player)) / 1000 / 60) + " more minutes to be logged again!" ));
                    }
                    else if (knownThreats.toString().contains(player) || onlineThreatMembers.toString().contains(player)) {
                        threatsInLands.add(player);
                        memberReportTimes.put(player, System.currentTimeMillis());
                    }
                    else if (onlineAlliedMembers.toString().contains(player)  || Config.getConfigStringList("alliedWhiteList").contains(player)){
                        alliesInLands.add(player);
                    }
                    else {
                        unknownPlayers.add(player);
                        memberReportTimes.put(player, System.currentTimeMillis());
                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("National Defense Alert");
                if (threatsInLands.size() > 0) {
                    embedBuilder.setColor(Color.RED);
                } else {
                    embedBuilder.setColor(new Color(0xE86713));
                }
                embedBuilder.addField("Province name", nationData.getString("label"), false);
                embedBuilder.addField("Online Dominion Members:", onlineMembers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Members near lands:", membersInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Allies near lands:", alliesInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Threats near our lands:", threatsInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Unknown Players near lands:", unknownPlayers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);

                embedBuilder.setFooter("Official Property of the Sunset Dominion.");

                if (threatsInLands.size() > 0 || unknownPlayers.size() > 0) {
                    channel.sendMessageEmbeds(embedBuilder.build()).queue();
                    if (threatsInLands.size() > 0) {
                        channel.sendMessage(":bangbang: <@&1084884328598741032> :bangbang:").queue(); // Millitary Role
                    } else {
                        channel.sendMessage("<@&1116977788541476904>").queue(); // Detection Ping Role
                    }

                }

                Logger.log(ConsoleColors.BLUE + nationName + ConsoleColors.YELLOW + " has been analyzed without error!");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (String nationName : Config.getConfigStringList("alliesList")){
            try {
                nationData = dynmapParser.getDynmapNation(nationName);

                if (nationData == null) {
                    throw new RuntimeException(nationName + " has returned null!");
                }

                java.util.List<String> knownThreats = Config.getConfigStringList("threatsList");
                java.util.List<String> unknownPlayers = new ArrayList<>();
                java.util.List<String> membersInLands = new ArrayList<>();
                List<String> threatsInLands = new ArrayList<>();


                for (String player : dynmapParser.getPlayersInAllNationLands(nationName)) {
                    if (onlineMembers.toString().contains(player) || Config.getConfigStringList("whitelisted").contains(player) || Config.getConfigStringList("alliesList").contains(player) || Config.getConfigStringList("alliedWhiteList").contains(player)) {
                        membersInLands.add(player);
                    }
                    else if (memberReportTimes == null || (memberReportTimes.get(player) != null && (((System.currentTimeMillis() - memberReportTimes.get(player)) / 1000 / 60 != 15)))) {
                        Logger.log(ConsoleColors.GREEN + player + ConsoleColors.BLUE + " must wait " + (15 - ((System.currentTimeMillis() - memberReportTimes.get(player)) / 1000 / 60) + " more minutes to be logged again!" ));
                    }
                    else if (knownThreats.toString().contains(player)) {
                        threatsInLands.add(player);
                        memberReportTimes.put(player, System.currentTimeMillis());
                    } else {
                        unknownPlayers.add(player);
                        memberReportTimes.put(player, System.currentTimeMillis());
                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Defense Alert for " + nationData.getString("label"));
                if (threatsInLands.size() > 0) {
                    embedBuilder.setColor(Color.RED);
                } else {
                    embedBuilder.setColor(new Color(0xE86713));
                }
                embedBuilder.addField("Online Allied Members:", onlineMembers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Members near lands:", membersInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Threats near lands:", threatsInLands.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Unknown Players near lands:", unknownPlayers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);

                embedBuilder.setFooter("Official Property of the Sunset Dominion.");

                if (threatsInLands.size() > 0 || unknownPlayers.size() > 0) {
                    channel.sendMessageEmbeds(embedBuilder.build()).queue();
                    channel.sendMessage("<@&1116977788541476904>").queue();

                }

                Logger.log(ConsoleColors.BLUE + nationName + ConsoleColors.YELLOW + " has been analyzed without error!");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void checkLogOut(JSONObject playerDetected) {
        TextChannel channel = activeGuild.getTextChannelById(Config.getConfigStringWithDefault("detectionChannel", "1084576582913499216"));

        JSONObject nationData;
        java.util.List<String> onlineDominionMembers = new ArrayList<>();
        java.util.List<String> onlineAlliedMembers = new ArrayList<>();

        for (String nationName : Config.getConfigStringList("nationsList")) {
            try {
                onlineDominionMembers.addAll(dynmapParser.getAllNationPlayers(nationName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (String nationName : Config.getConfigStringList("alliesList")) {
            try {
                onlineAlliedMembers.addAll(dynmapParser.getAllNationPlayers(nationName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        for (String nationName : Config.getConfigStringList("nationsList")) {
            try {
                nationData = dynmapParser.getDynmapNation(nationName);

                if (nationData == null) {
                    throw new RuntimeException(nationName + " has returned null!");
                }

                java.util.List<String> knownThreats = Config.getConfigStringList("threatsList");
                java.util.List<String> unknownPlayers = new ArrayList<>();
                java.util.List<String> membersInLands = new ArrayList<>();
                List<String> threatsInLands = new ArrayList<>();

                for (String player : dynmapParser.getPlayersInAllNationLands(nationName)) {
                    if (onlineDominionMembers.toString().contains(player) || Config.getConfigStringList("whitelisted").contains(player)) {
                        membersInLands.add(player);
                    }
                    else if (knownThreats.toString().contains(player)) {
                        threatsInLands.add(player);
                    } else {
                        unknownPlayers.add(player);
                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Threatening Log Off Detected!");
                if (threatsInLands.size() > 0) {
                    embedBuilder.setColor(Color.RED);
                } else {
                    embedBuilder.setColor(new Color(0xE86713));
                }
                embedBuilder.addField("Province name", nationData.getString("label"), false);
                embedBuilder.addField("Online Dominion Members:", onlineDominionMembers.toString().replaceAll("[\\[\\]\\(\\),]", ""), false);
                embedBuilder.addField("Suspect player:", playerDetected.getString("name").replaceAll("[\\[\\]\\(\\),]", ""), false);

                embedBuilder.setFooter("Official Property of the Sunset Dominion.");

                if (threatsInLands.toString().contains(playerDetected.getString("name")) || unknownPlayers.toString().contains(playerDetected.getString("name"))) {
                    channel.sendMessageEmbeds(embedBuilder.build()).queue();
                    channel.sendMessage("<@&1116977788541476904>").queue();

                }

                Logger.log(ConsoleColors.BLUE + nationName + ConsoleColors.YELLOW + " has been checked for bad log offs!");

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
