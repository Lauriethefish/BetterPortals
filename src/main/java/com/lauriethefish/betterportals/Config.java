package com.lauriethefish.betterportals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

// Stores all of the configuration for the BetterPortals plugin, so that it can be
// easy sent between events
public class Config {
    private BetterPortals pl; // Reference to the plugin

    public List<WorldLink> worldLinks = new ArrayList<>();

    // The min and max values for the blocks that the raycast will check
    public double minXZ;
    public double maxXZ;
    public double minY;
    public double maxY;

    // The minimum distance required for the  portal effect to be displayed
    public double portalActivationDistance;
    // The maximum distance that the ray will travel before giving up
    public double maxRayCastDistance;
    // The amount that the ray needs to be advanced each raycast iteration
    public double rayCastIncrement;

    // Multiplyers used to access the array of changed blocks
    // This array stores the ghost blocks that have been changed
    // to help performance
    public int yMultip;
    public int zMultip;
    public int totalArrayLength;

    // Maximum size of portals
    public Vector maxPortalSize;

    // The offset of the portal's collision box
    public Vector portalCollisionBox;

    // How often the portal re-checks its surrounding blocks
    public int portalBlockUpdateInterval;

    // Loads the configuration from the given file, setting all the parameters of the class
    public Config(BetterPortals pl, FileConfiguration file)   {
        this.pl = pl;

        // Call a function to copy all of the keys in the loaded file into the default
        // config file, in order to add any keys required for a new version
        // This has the side effect of deleting comments, so it only happens if an update is required
        file = updateConfigFile(file);

        // Load all of the parameters from the config file
        // Calculate the min and max values
        maxXZ = file.getInt("portalEffectSizeXZ");
        minXZ = maxXZ * -1.0;
        maxY = file.getInt("portalEffectSizeY");
        minY = maxY * -1.0;

        // Calculate the multiplyers for accessing the table
        yMultip = (int) (maxXZ - minXZ);
        zMultip = (int) (yMultip * (maxY - minY));
        totalArrayLength = yMultip * zMultip;

        portalActivationDistance = file.getDouble("portalActivationDistance");
        portalBlockUpdateInterval = file.getInt("portalBlockUpdateInterval");

        rayCastIncrement = file.getDouble("rayCastIncrement");
        maxRayCastDistance = file.getDouble("maxRayCastDistance");

        // If the maxRayCastDistance is set to -1, work it out based on the portalActivationDistance
        if(maxRayCastDistance == -1)    {
            maxRayCastDistance = portalActivationDistance + 3.0;
        }

        // Load the max portal size
        ConfigurationSection maxSizeSection = file.getConfigurationSection("maxPortalSize");
        maxPortalSize = new Vector(
            maxSizeSection.getInt("x"),
            maxSizeSection.getInt("y"),
            0.0
        );

        // Load the portal's collision box
        ConfigurationSection cBoxSection = file.getConfigurationSection("portalCollisionBox");
        portalCollisionBox = new Vector(
            cBoxSection.getDouble("x"),
            cBoxSection.getDouble("y"),
            cBoxSection.getDouble("z")
        );

        // Get the list that contains all of the world links
        ConfigurationSection worldLinksSection = file.getConfigurationSection("worldConnections");
        // Get an iterator over all of the value
        Iterator<String> links  = worldLinksSection.getKeys(false).iterator();

        while(links.hasNext())  {
            // Get the configuration section of the link and retrieve all of the values for it using the constructor
            WorldLink newLink = new WorldLink(pl, worldLinksSection.getConfigurationSection(links.next()));
            // Add it to the list
            worldLinks.add(newLink);        
        }
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