package com.lauriethefish.betterportals.bukkit.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Set;

@Singleton
public class ConfigManager {
    private final MessageConfig messages;
    private final PortalSpawnConfig spawning;
    private final RenderConfig rendering;
    private final ProxyConfig proxy;
    private final MiscConfig misc;

    private final Logger logger;

    @Inject
    public ConfigManager(Logger logger, MessageConfig messages, PortalSpawnConfig spawning, RenderConfig rendering, ProxyConfig proxy, MiscConfig misc) {
        this.logger = logger;
        this.messages = messages;
        this.spawning = spawning;
        this.rendering = rendering;
        this.proxy = proxy;
        this.misc = misc;
    }

    /**
     * Loads all config values, throwing an error if parsing fails.
     * @param pl The plugin instance, or null if loading during testing
     * @param file The configuration file to load from
     */
    public void loadValues(@NotNull FileConfiguration file, @Nullable JavaPlugin pl) {
        if(pl != null) {
            file = updateFromResources(file, pl);
        }

        misc.load(file);
        messages.load(file);
        spawning.load(file);
        rendering.load(file);
        proxy.load(file);
    }

    /**
     * Reads the resource in the JAR file of <code>pl</code> with path <code>name</code>.
     * @param pl The plugin to read the resource from
     * @param name The resource path
     * @return The string value of the resource, or null if failed
     */
    private String readResourceToString(@NotNull JavaPlugin pl, @NotNull String name)    {
        InputStream resource = pl.getResource(name);
        if(resource == null) {
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
        StringBuilder buffer = new StringBuilder();
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

    /**
     * Copies any missing parameters from the config into the default config if any are missing.
     * This has the side effect of removing comments, so it only happens if there are actually keys missing
     * @param pl The BetterPortals plugin
     * @return The updated config file
     */
    private FileConfiguration updateFromResources(FileConfiguration file, JavaPlugin pl) {
        FileConfiguration defaultConfig = new YamlConfiguration();

        try {
            defaultConfig.loadFromString(Objects.requireNonNull(readResourceToString(pl, "config.yml"), "Failed to read default config resource - this should never happen!"));

            // If the saved config file has the right keys and values, return it since it is on the correct version
            Set<String> savedFileKeys = file.getKeys(true);
            if(defaultConfig.getKeys(true).size() <= savedFileKeys.size())   {
                return file;
            }

            logger.info("Updating config file . . .");

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

        }   catch(Exception ex)    {
            ex.printStackTrace();
        }

        return defaultConfig;
    }
}