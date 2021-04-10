package com.lauriethefish.betterportals.bukkit.config;

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

    public WorldLink(ConfigurationSection config) {
        String originWorldName = Objects.requireNonNull(config.getString("originWorld"), "Missing originWorld key in world link");
        String destWorldName = Objects.requireNonNull(config.getString("destinationWorld"), "Missing destinationWorld key in world link");

        this.originWorld = Bukkit.getWorld(originWorldName);
        this.destinationWorld = Bukkit.getWorld(destWorldName);
    }
}
