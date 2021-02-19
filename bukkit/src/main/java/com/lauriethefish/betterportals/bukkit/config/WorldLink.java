package com.lauriethefish.betterportals.bukkit.config;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Stores the link between two worlds, for instance the overworld and the nether
 * There can be multiple links, for instance to link multiverses together
 * The overworld and the nether will have two links, one for each direction
 * This is to allow one way connections
 */
@Getter
public class WorldLink  {
    private final String originWorldName;
    private final String destWorldName;

    private final World originWorld;
    private final World destinationWorld;

    private final int minSpawnY;
    private final int maxSpawnY;

    private final double coordinateRescalingFactor;

    public WorldLink(ConfigurationSection config)    {
        originWorldName = Objects.requireNonNull(config.getString("originWorld"), "Missing originWorld key in world link");
        destWorldName = Objects.requireNonNull(config.getString("destinationWorld"), "Missing destinationWorld key in world link");

        originWorld = Bukkit.getWorld(originWorldName);
        destinationWorld = Bukkit.getWorld(destWorldName);

        minSpawnY = config.getInt("minSpawnY");
        maxSpawnY = config.getInt("maxSpawnY");
        coordinateRescalingFactor = config.getDouble("coordinateRescalingFactor");
    }

    public boolean isValid()    {
        return originWorld != null && destinationWorld != null;
    }

    @NotNull
    public Location moveFromOriginWorld(@NotNull Location loc) {
        assert loc.getWorld() == originWorld;
        loc = loc.clone();

        // Avoid multiplying the Y coordinate
        loc.setX(loc.getX() * coordinateRescalingFactor);
        loc.setZ(loc.getZ() * coordinateRescalingFactor);
        loc.setWorld(destinationWorld);
        return loc;
    }
}