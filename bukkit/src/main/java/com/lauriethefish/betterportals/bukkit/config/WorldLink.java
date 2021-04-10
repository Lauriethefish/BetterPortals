package com.lauriethefish.betterportals.bukkit.config;

import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * Stores the link between two worlds, for instance the overworld and the nether
 * There can be multiple links, for instance to link multiverses together
 * The overworld and the nether will have two links, one for each direction
 * This is to allow one way connections
 */
@Getter
public class WorldLink {
    protected final World originWorld;
    protected final World destinationWorld;

    protected final boolean isValid;

    public WorldLink(ConfigurationSection config, Logger logger) {
        String originWorldName = Objects.requireNonNull(config.getString("originWorld"), "Missing originWorld key in world link");
        String destWorldName = Objects.requireNonNull(config.getString("destinationWorld"), "Missing destinationWorld key in world link");

        this.originWorld = Bukkit.getWorld(originWorldName);
        this.destinationWorld = Bukkit.getWorld(destWorldName);

        if(originWorld == null || destinationWorld == null) {
            logger.warning("An invalid world link was found in the config, please check that your world names are correct.");
            if (originWorld == null) {
                logger.warning("No world with name \"%s\" exists (for the origin)", originWorldName);
            }
            if (destinationWorld == null) {
                logger.warning("No world with name \"%s\" exists (for the destination)", destWorldName);
            }
            isValid = false;
        }   else    {
            logger.fine("Loaded world link with origin \"%s\" and destination \"%s\"", originWorldName, destWorldName);
            isValid = true;
        }
    }
}
