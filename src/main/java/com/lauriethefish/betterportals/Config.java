package com.lauriethefish.betterportals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

// Stores all of the configuration for the BetterPortals plugin, so that it can be
// easy sent between events
public class Config {
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

    // Loads the configuration from the given file, setting all the parameters of the class
    public Config(BetterPortals pl, FileConfiguration file)   {
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
}