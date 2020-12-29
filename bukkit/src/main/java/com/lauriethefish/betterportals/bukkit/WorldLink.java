package com.lauriethefish.betterportals.bukkit;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

// Stores the link between two worlds, for instance the overworld and the nether
// There can be multiple links, for instance to link multiverses together
// The overworld and the nether will have two links, one for each direction
// This is to allow one way connections
public class WorldLink  {
    // The two worlds that are linked together
    public World originWorld;
    public World destinationWorld;

    // The boundaries on where portals can spawn in the destinationWorld
    public int minSpawnY;
    public int maxSpawnY;

    // The amount that coordinates are multiplied by when going between worlds
    public double coordinateRescalingFactor;

    // Loads all of the parameters from the given config section
    public WorldLink(BetterPortals pl, ConfigurationSection config)    {
        // Load the names of the two worlds, then get them from the server
        originWorld = pl.getServer().getWorld(config.getString("originWorld"));
        destinationWorld = pl.getServer().getWorld(config.getString("destinationWorld"));

        // Get the other parameters directly from the config
        minSpawnY = config.getInt("minSpawnY");
        maxSpawnY = config.getInt("maxSpawnY");
        coordinateRescalingFactor = config.getDouble("coordinateRescalingFactor");
    }

    public boolean isValid()    {
        return originWorld != null && destinationWorld != null;
    }
}