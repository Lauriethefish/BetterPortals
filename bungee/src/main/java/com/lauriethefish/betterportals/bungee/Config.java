package com.lauriethefish.betterportals.bungee;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IllegalFormatException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Bungeecord doesn't have a simple getConfig method like in spigot.
 * This class handles loading and saving of the config file.
 */
@Singleton
public class Config {
    private final Plugin pl;
    private final Logger logger;

    @Getter private InetSocketAddress bindAddress;
    @Getter private UUID key;

    @Inject
    public Config(Plugin pl, Logger logger) {
        this.pl = pl;
        this.logger = logger;
    }

    private Path getConfigFilePath() {
        File dataFolder = pl.getDataFolder();
        dataFolder.mkdir();

        return dataFolder.toPath().resolve("config.yml");
    }

    private Configuration loadFile() throws IOException {
        Path configFilePath = getConfigFilePath();
        File configFile = configFilePath.toFile();
        if (!configFile.exists()) {
            logger.info("Saving default config . . .");
            Files.copy(pl.getResourceAsStream("bungeeconfig.yml"), configFilePath);
        }

        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
    }

    private void saveFile(Configuration file) throws IOException {
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(file, getConfigFilePath().toFile());
    }

    public void load() throws IOException {
        Configuration config = loadFile();
        boolean wasModified = false;

        if(!config.contains("logLevel")) {
            config.set("logLevel", "INFO");
            config.set("enableDebugLogging", null);
            wasModified = true;
        }

        Level configuredLevel = Level.parse(config.getString("logLevel"));
        logger.setLevel(configuredLevel);

        String addressStr = config.getString("bindAddress");
        if(addressStr == null) {
            throw new RuntimeException("Invalid bind address: " + addressStr);
        }

        int port = config.getInt("serverPort");
        if(port == 0) {
            throw new RuntimeException("Invalid bind port " + port);
        }

        try {
            key = UUID.fromString(Objects.requireNonNull(config.getString("key"), "No encryption key found in the config"));
        }   catch(IllegalFormatException ex) {
            key = UUID.randomUUID();
            wasModified = true;
        }

        bindAddress = new InetSocketAddress(addressStr, port);

        if(wasModified) {
            saveFile(config);
        }
    }
}
