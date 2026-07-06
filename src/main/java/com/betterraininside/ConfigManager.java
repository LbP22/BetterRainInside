package com.betterraininside;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BetterRainInside/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "betterraininside.json";

    private ConfigManager() {
    }

    public static Config loadOrCreate() {
        Path path = getConfigPath();

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Config config = GSON.fromJson(reader, Config.class);
                if (config == null) {
                    config = new Config();
                }
                config.sanitize();
                save(config);
                return config;
            } catch (IOException exception) {
                LOGGER.warn("Failed to read config at {}. Using defaults.", path, exception);
            }
        }

        Config config = new Config();
        config.sanitize();
        save(config);
        return config;
    }

    public static void save(Config config) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to write config at {}", path, exception);
        }
    }

    private static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }
}
