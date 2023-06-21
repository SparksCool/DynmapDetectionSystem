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
    private static JSONObject config;



    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Initializing Dynmap Detection Software!");

        dynmapParser = new DynmapParser("http://mc.voidedsky.net:9250/up/world/world/");

        discordBot = new DiscordBot();

        checkAndCreateJsonConfig("./config.json", "{}");

        Timer timer = new Timer();
        timer.schedule(new DynmapDetection(), 0, 30000);
    }

    public static DynmapParser getDynmapParser() {
        return dynmapParser;
    }

    public static DiscordBot getDiscordBot() {
        return discordBot;
    }

    public static JSONObject getConfig() {
        return config;
    }

    public static String getConfigStringWithDefault(String key, String defaultValue) {
        if (config.has(key)) {
            return config.getString(key);
        } else {
            return defaultValue;
        }
    }

    public static Object getConfigObjectWithDefault(String key, Object defaultValue) {
        if (config.has(key)) {
            return config.get(key);
        } else {
            return defaultValue;
        }
    }

    public static JSONArray getConfigJSONArrayWithDefault(String key, JSONArray defaultValue) {
        if (config.has(key)) {
            return config.getJSONArray(key);
        } else {
            return defaultValue;
        }
    }

    public static List<String> getConfigStringList(String key) {
        List<String> configList = new ArrayList<>();
        for(Object entry : Main.getConfigJSONArrayWithDefault(key, new JSONArray())) {
            configList.add(entry.toString());
        }
        return configList;
    }

    public static void checkAndCreateJsonConfig(String filePath, String defaultContent) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            try {
                Files.write(path, defaultContent.getBytes());
                System.out.println("JSON config file created at " + filePath);
                config = new JSONObject(readJsonConfig(path));
            } catch (IOException e) {
                Logger.log(ConsoleColors.RED + "Error while creating JSON config file: " + e.getMessage());
            }
        } else {
            Logger.log(ConsoleColors.YELLOW + "JSON config file already exists at " + filePath);
            config = new JSONObject(readJsonConfig(path));


        }
    }

    public static String readJsonConfig(Path path) {
        String content = "";

        try {
            content = Files.readString(path);
        } catch (IOException e) {
            System.err.println("Error while reading JSON config file: " + e.getMessage());
        }

        return content;
    }
}