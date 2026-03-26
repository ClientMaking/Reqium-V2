package com.reqium.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of(System.getenv("APPDATA"), ".reqium");
    
    public static void initializeConfig() {
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void saveConfig(String name, Object config) {
        try {
            Files.createDirectories(CONFIG_DIR);
            File file = CONFIG_DIR.resolve(name + ".json").toFile();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static <T> T loadConfig(String name, Class<T> clazz) {
        try {
            File file = CONFIG_DIR.resolve(name + ".json").toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    return GSON.fromJson(reader, clazz);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
