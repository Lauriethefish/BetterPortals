package com.lauriethefish.betterportals.bungee;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Handler;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BetterPortals extends Plugin {
    // TODO: config for changing logging setting
    private static final boolean DEBUG_LOGGING = true;

    @Getter private PortalServer portalServer;
    @Getter private Configuration config;

    @Override
    public void onEnable() {
        // Load the config, printing any errors that occured
        try {
            loadConfig();
        } catch (IOException ex) {
            getLogger().severe("An error occured while loading the config (maybe it isn't valid YAML)!");
            ex.printStackTrace();
            return;
        }

        // copy the common logger's two handlers
        for (Handler handler : getLogger().getParent().getHandlers()) {
            getLogger().addHandler(handler);
        }
        // ensure that the common logger's handlers don't get used too
        getLogger().setUseParentHandlers(false);

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
        if(DEBUG_LOGGING)   { // Make sure debug logging is enabled first
            getLogger().info("[DEBUG] " + message);
        }
    }
}
