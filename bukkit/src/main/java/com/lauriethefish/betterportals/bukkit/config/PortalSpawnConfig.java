package com.lauriethefish.betterportals.bukkit.config;

import java.util.*;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class PortalSpawnConfig {
    private final FileConfiguration file;
    private final Logger logger;

    private final Map<World, WorldLink> worldLinks = new HashMap<>();
    private final Set<World> disabledWorlds = new HashSet<>();

    @Getter private Vector maxPortalSize; // Maximum size of natural/nether portals
    @Getter private int minimumPortalSpawnDistance; // How close portals will spawn to each other

    // Copies some blocks randomly from the destination to the origin of a portal when lit
    // Creates a cool sort of "creeping" effect for the player.
    @Getter private boolean dimensionBlendEnabled;
    @Getter private double blendFallOff;

    @Getter private double allowedSpawnTimePerTick;

    @Inject
    public PortalSpawnConfig(@Named("configFile") FileConfiguration file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void load() {
        ConfigurationSection dimBlendSection = Objects.requireNonNull(file.getConfigurationSection("dimensionBlend"));
        dimensionBlendEnabled = dimBlendSection.getBoolean("enable");
        blendFallOff = dimBlendSection.getDouble("fallOffRate");

        ConfigurationSection worldLinksSection = Objects.requireNonNull(file.getConfigurationSection("worldConnections"), "World connections section missing");

        for (String s : worldLinksSection.getKeys(false)) {
            WorldLink newLink = new WorldLink(Objects.requireNonNull(worldLinksSection.getConfigurationSection(s)));
            if (!newLink.isValid()) {
                logger.info(ChatColor.RED + "An invalid worldConnection was found in config.yml, please check that your world names are correct");
                continue;
            }

            worldLinks.put(newLink.getOriginWorld(), newLink);
        }

        List<String> disabledWorldsString = file.getStringList("disabledWorlds");
        for(String worldString : disabledWorldsString)  {
            World world = Bukkit.getWorld(worldString);
            disabledWorlds.add(world);
        }

        ConfigurationSection maxSizeSection = Objects.requireNonNull(file.getConfigurationSection("maxPortalSize"), "Maximum portal size section missing");
        maxPortalSize = new Vector(
                maxSizeSection.getInt("x"),
                maxSizeSection.getInt("y"),
                0.0
        );

        minimumPortalSpawnDistance = file.getInt("minimumPortalSpawnDistance");
        allowedSpawnTimePerTick = file.getDouble("allowedSpawnTimePerTick");
    }

    public boolean isWorldDisabled(World world) {
        return disabledWorlds.contains(world);
    }

    public @Nullable WorldLink getWorldLink(@NotNull World originWorld) {
        return worldLinks.get(originWorld);
    }
}
