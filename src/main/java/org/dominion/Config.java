package org.dominion;

import org.dominion.logging.ConsoleColors;
import org.dominion.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Config {

    private static JSONObject config = checkAndCreateJsonConfig();
    private static final String defaultContent =  "{\n" +
            "    \"threatsList\": [],\n" +
            "    \"whitelisted\": [\n" +
            "    ],\n" +
            "    \"nationsList\": [\n" +
            "    ],\n" +
            "    \"guildID\": \"\",\n" +
            "    \"detectionChannel\": \"\",\n" +
            "    \"token\": \"\"\n" +
            "}";

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
        for(Object entry : getConfigJSONArrayWithDefault(key, new JSONArray())) {
            configList.add(entry.toString());
        }
        return configList;
    }

    public static JSONObject checkAndCreateJsonConfig() {
        Path path = Paths.get("./config.json");

        if (!Files.exists(path)) {
            try {
                Files.write(path, defaultContent.getBytes());
                System.out.println("JSON config file created at ./config.json");
                config = new JSONObject(readJsonConfig(path));
            } catch (IOException e) {
                Logger.log(ConsoleColors.RED + "Error while creating JSON config file: " + e.getMessage());
            }
        } else {
            Logger.log(ConsoleColors.YELLOW + "JSON config file already exists at ./config.json");
            config = new JSONObject(readJsonConfig(path));


        }

        return config;
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
