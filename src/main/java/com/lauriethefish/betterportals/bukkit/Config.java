package com.lauriethefish.betterportals.bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

// Stores all of the configuration for the BetterPortals plugin, so that it can be
// easy sent between events
public class Config {
    private BetterPortals pl; // Reference to the plugin

    public List<WorldLink> worldLinks = new ArrayList<>();
    private Set<World> disabledWorlds = new HashSet<>();

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
    private int yMultip;
    private int zMultip;

    public int arraySizeXZ;
    public int arraySizeY;
    public int totalArrayLength;

    public int[] surroundingOffsets;

    // Maximum size of portals
    public Vector maxPortalSize;

    // The offset of the portal's collision box
    public Vector portalCollisionBox;

    // How often the portal re-checks its surrounding blocks
    public int portalBlockUpdateInterval;

    public boolean enableEntitySupport;
    public int entityCheckInterval;

    // If this is true then we will send packets to hide and show the portal blocks
    public boolean hidePortalBlocks;

    // How close portals will spawn to each other
    public int minimumPortalSpawnDistance;

    // How long the plugin waits before rendering portals after switching worlds
    public int worldSwitchWaitTime;

    // Makes additional things be run on other threads that probably shouldn't be run on other threads
    public boolean unsafeMode;
    public boolean enableDebugLogging;

    // Contains all the customisable messages of the plugin
    public ConfigurationSection messagesSection;
    public String chatPrefix;

    // Name of all given portal wand items
    public String portalWandName;

    public boolean enableProxy; // Whether or not bungeecord support will be enabled
    public String proxyAddress; // The address of the proxy to connect to
    public int proxyPort; // The port on the proxy to connect to

    // Loads the configuration from the given file, setting all the parameters of the class
    public Config(BetterPortals pl)   {
        this.pl = pl;
        FileConfiguration file = pl.getConfig();

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

        // Calculate the multipliers for accessing the table
        zMultip = (int) (maxXZ - minXZ + 1);
        yMultip = zMultip * zMultip;
        totalArrayLength = yMultip * (int) (maxY - minY + 1);

        // Calculate the differences in index for quickly accessing the block array while building the mesh
        surroundingOffsets = new int[]{
            1,
            -1,
            yMultip,
            -yMultip,
            zMultip,
            -zMultip
        };

        portalActivationDistance = file.getDouble("portalActivationDistance");
        portalBlockUpdateInterval = file.getInt("portalBlockUpdateInterval");
        rayCastIncrement = file.getDouble("rayCastIncrement");
        maxRayCastDistance = file.getDouble("maxRayCastDistance");
        enableEntitySupport = file.getBoolean("enableEntitySupport");
        entityCheckInterval = file.getInt("entityCheckInterval");
        hidePortalBlocks = file.getBoolean("hidePortalBlocks");
        minimumPortalSpawnDistance = file.getInt("minimumPortalSpawnDistance");
        worldSwitchWaitTime = file.getInt("waitTimeAfterSwitchingWorlds");
        unsafeMode = file.getBoolean("unsafeMode");
        enableDebugLogging = file.getBoolean("enableDebugLogging");

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
            if(!newLink.isValid())  {
                pl.getLogger().info(ChatColor.RED + "An invalid worldConnection was found in config.yml, please check that your world names are correct");
                continue;
            }
            
            worldLinks.add(newLink);        
        }

        // Add all the disabled worlds to a set
        List<String> disabledWorldsString = file.getStringList("disabledWorlds");
        for(String worldString : disabledWorldsString)  {
            World world = pl.getServer().getWorld(worldString);
            disabledWorlds.add(world);
        }

        messagesSection = file.getConfigurationSection("chatMessages");
        chatPrefix = getChatMessageRaw("prefix");
        portalWandName = ChatColor.translateAlternateColorCodes('&', file.getString("portalWandName"));

        // Load the values to do with proxy configuration
        ConfigurationSection proxySection = file.getConfigurationSection("proxy");
        enableProxy = proxySection.getBoolean("enableProxy");
        proxyAddress = proxySection.getString("proxyAddress");
        proxyPort = proxySection.getInt("proxyPort");
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

    // Finds the index in an array of blocks surrounding the portal
    // Coordinates should be relative to the bottom left and lowest corner of the box
    public int calculateBlockArrayIndex(double x, double y, double z)  {
        return (int) (z * zMultip + y * yMultip + x) + totalArrayLength / 2;
    }

    // Convenience methods for getting if a world is disabled
    public boolean isWorldDisabled(Location loc) {
        return isWorldDisabled(loc.getWorld());
    }

    public boolean isWorldDisabled(World world) {
        return disabledWorlds.contains(world);
    }

    // Gets the chat message with color codes for the key
    public String getChatMessageRaw(String key)  {
        return ChatColor.translateAlternateColorCodes('&', messagesSection.getString(key));
    }

    // Gets a chat message with the chat prefix before it
    public String getChatMessage(String key)    {
        return chatPrefix + getChatMessageRaw(key);
    }

    // Gets a chat message without the prefix, and colors it red
    public String getErrorMessage(String key)   {
        return ChatColor.RED + getChatMessageRaw(key);
    }
}