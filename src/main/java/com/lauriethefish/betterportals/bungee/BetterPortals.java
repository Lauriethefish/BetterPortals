package com.lauriethefish.betterportals.bungee;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BetterPortals extends Plugin {
    private boolean enableDebugLogging;

    @Getter private PortalServer portalServer;
    @Getter private Configuration config;

    @Override
    public void onEnable() {
        // Load the config, printing any errors that occured
        try {
            loadConfig();
            validateEncryptionKey(); // Generate the encryption key
        } catch (Throwable ex) {
            getLogger().severe("An error occured while loading the config (maybe it isn't valid YAML)!");
            ex.printStackTrace();
            return;
        }

        portalServer = new PortalServer(this);
    }

    // Saves the default config file if it doesn't exist, then loads the config file
    private void loadConfig() throws IOException {
        File dataFolder = getDataFolder();
        dataFolder.mkdir();

        // Save the default config file if it doesn't already exist
        Path configFilePath = dataFolder.toPath().resolve("config.yml");
        File configFile = configFilePath.toFile();
        if (!configFile.exists()) {
            getLogger().info("Saving default config . . .");
            Files.copy(getResourceAsStream("bungeeconfig.yml"), configFilePath);
        }

        // Load the config from disk
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        enableDebugLogging = config.getBoolean("enableDebugLogging");

    }

    // Saves the currently loaded config file back to config.yml (useful for saving changes)
    private void saveConfig() throws IOException {
        ConfigurationProvider.getProvider(YamlConfiguration.class)
                .save(config, new File(getDataFolder(), "config.yml"));
    }

    private void validateEncryptionKey() throws IOException {
        try {
            UUID.fromString(config.getString("key")); // Attempt to parse the key as a UUID
        }   catch(IllegalArgumentException ex) { // This will be thrown if it has never been generated before, or is invalid
            getLogger().info("Generating new random encryption key . . .");
            config.set("key", UUID.randomUUID().toString());
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        portalServer.shutdown();
    }

    // Methods for conveniently logging debug messages
    public void logDebug(String format, Object... args) {        
        logDebug(String.format(format, args));
    }

    public void logDebug(String message) {
        if(enableDebugLogging)   { // Make sure debug logging is enabled first
            getLogger().info("[DEBUG] " + message);
        }
    }
}
