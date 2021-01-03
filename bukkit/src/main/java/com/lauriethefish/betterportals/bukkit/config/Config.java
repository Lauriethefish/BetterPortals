package com.lauriethefish.betterportals.bukkit.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import lombok.Getter;

// Stores all of the configuration for the BetterPortals plugin, so that it can be
// easy sent between events
public class Config {
    private BetterPortals pl;

    @Getter private MessageConfig messages;
    @Getter private PortalSpawnConfig spawning;
    @Getter private RenderConfig rendering;
    @Getter private ProxyConfig proxy;

    // Miscellaneous config options that don't fit in to any of the sub-configs
    @Getter private double portalActivationDistance;

    @Getter private boolean entitySupportEnabled;
    @Getter private int entityCheckInterval;

    @Getter private boolean debugLoggingEnabled;

    @Getter private int teleportCooldown;

    // Loads the configuration from the given file, setting all the parameters of the class
    public Config(BetterPortals pl)   {
        this.pl = pl;
        FileConfiguration file = pl.getConfig();

        // Call a function to copy all of the keys in the loaded file into the default
        // config file, in order to add any keys required for a new version
        // This has the side effect of deleting comments, so it only happens if an update is required
        file = updateConfigFile(file);

        // Load miscellaneous config options.
        portalActivationDistance = file.getDouble("portalActivationDistance");
        entitySupportEnabled = file.getBoolean("enableEntitySupport");
        entityCheckInterval = file.getInt("entityCheckInterval");
        debugLoggingEnabled = file.getBoolean("enableDebugLogging");
        teleportCooldown = file.getInt("teleportCooldown");

        // Load each of the sub-configs.
        messages = new MessageConfig(file);
        spawning = new PortalSpawnConfig(pl, file);
        rendering = new RenderConfig(file);
        proxy = new ProxyConfig(pl, file);
    }

    // Reads everything inside a resource of the JAR to a string
    private String readResourceToString(String name)    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(pl.getResource(name)));
        StringBuffer buffer = new StringBuffer();
        String str;
        try {
            while((str = reader.readLine()) != null) {
                buffer.append(str); buffer.append("\n");
            }
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return buffer.toString();
    }

    // Copies the config in the current config file into the default config in order to update it with any values that may have been added in an update
    private FileConfiguration updateConfigFile(FileConfiguration file) {
        FileConfiguration defaultConfig = new YamlConfiguration();

        try {
            // Load the default config from inside the plugin
            defaultConfig.loadFromString(readResourceToString("config.yml"));

            // If the saved config file has the right keys and values, return it since it is on the correct version
            Set<String> savedFileKeys = file.getKeys(true);
            if(defaultConfig.getKeys(true).size() == savedFileKeys.size())   {
                return file;
            }

            pl.getLogger().info("Updating config file . . .");

            // Copy over all of the config from the loaded file to the default config
            for(String key : savedFileKeys)   {
                // Only set the value if it is not a section
                Object value = file.get(key);
                if(!(value instanceof ConfigurationSection))  {
                    defaultConfig.set(key, value);
                }
            }

            // Save the default config back to config.yml
            defaultConfig.save(pl.getDataFolder().toPath().resolve("config.yml").toFile());

        }   catch(Exception e)    {
            e.printStackTrace(); // Print any exceptions for debugging
        }

        // Return the default config
        return defaultConfig;
    }
}