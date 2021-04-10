package com.lauriethefish.betterportals.bukkit.config;

import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Link that contains extra parameters specific to spawning nether portals
 */
@Getter
public class NetherLink extends WorldLink   {
    private final int minSpawnY;
    private final int maxSpawnY;

    private final double coordinateRescalingFactor;

    public NetherLink(ConfigurationSection config, Logger logger)    {
        super(config, logger);
        if(!isValid) {
            minSpawnY = -1;
            maxSpawnY = -1;
            coordinateRescalingFactor = -1;
            return;
        }

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