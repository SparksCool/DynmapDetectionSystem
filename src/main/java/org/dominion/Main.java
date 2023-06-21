package org.dominion;

import org.dominion.discord.DiscordBot;
import org.dominion.dynmap.DynmapDetection;
import org.dominion.dynmap.DynmapParser;
import org.dominion.logging.ConsoleColors;
import org.dominion.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static DynmapParser dynmapParser;
    private static Main activeInstance;
    private static DiscordBot discordBot;



    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Initializing Dynmap Detection Software!");

        dynmapParser = new DynmapParser("http://mc.voidedsky.net:9250/up/world/world/");

        discordBot = new DiscordBot();

        Config.checkAndCreateJsonConfig();

        Timer timer = new Timer();
        timer.schedule(new DynmapDetection(), 0, 30000);
    }

    public static DynmapParser getDynmapParser() {
        return dynmapParser;
    }

    public static DiscordBot getDiscordBot() {
        return discordBot;
    }

}