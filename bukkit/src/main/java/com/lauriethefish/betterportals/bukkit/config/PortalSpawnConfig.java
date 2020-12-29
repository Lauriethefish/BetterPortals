package com.lauriethefish.betterportals.bukkit.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.WorldLink;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import lombok.Getter;

// Handles all config options relating to portal spawning
public class PortalSpawnConfig {
    @Getter private List<WorldLink> worldLinks = new ArrayList<>();
    private Set<World> disabledWorlds = new HashSet<>();

    @Getter public Vector maxPortalSize; // Maximum size of natural/nether portals
    @Getter public int minimumPortalSpawnDistance; // How close portals will spawn to each other

    // Copies some blocks randomly from the destination to the origin of a portal when lit
    // Creates a cool sort of "creeping" effect for the player.
    @Getter private boolean dimensionBlendEnabled;
    @Getter private double blendFallOff;

    public PortalSpawnConfig(BetterPortals pl, FileConfiguration file) {
        ConfigurationSection dimBlendSection = file.getConfigurationSection("dimensionBlend");
        dimensionBlendEnabled = dimBlendSection.getBoolean("enable");
        blendFallOff = dimBlendSection.getDouble("fallOffRate");

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

        // Load the max portal size
        ConfigurationSection maxSizeSection = file.getConfigurationSection("maxPortalSize");
        maxPortalSize = new Vector(
            maxSizeSection.getInt("x"),
            maxSizeSection.getInt("y"),
            0.0
        );

        minimumPortalSpawnDistance = file.getInt("minimumPortalSpawnDistance");
    }

    // Convenience methods for getting if a world is disabled
    public boolean isWorldDisabled(Location loc) {
        return isWorldDisabled(loc.getWorld());
    }

    public boolean isWorldDisabled(World world) {
        return disabledWorlds.contains(world);
    }
}
