package com.lauriethefish.betterportals.bukkit.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
public class PortalSpawnConfig {
    private final Logger logger;

    private Map<World, NetherLink> netherLinks;
    private Set<World> disabledWorlds;

    @Getter private Vector maxPortalSize; // Maximum size of natural/nether portals
    @Getter private int minimumPortalSpawnDistance; // How close portals will spawn to each other

    // Copies some blocks randomly from the destination to the origin of a portal when lit
    // Creates a cool sort of "creeping" effect for the player.
    @Getter private boolean dimensionBlendEnabled;
    @Getter private double blendFallOff;

    @Getter private double allowedSpawnTimePerTick;

    @Inject
    public PortalSpawnConfig(Logger logger) {
        this.logger = logger;
    }

    public void load(FileConfiguration file) {
        netherLinks = new HashMap<>();
        disabledWorlds = new HashSet<>();

        ConfigurationSection dimBlendSection = Objects.requireNonNull(file.getConfigurationSection("dimensionBlend"));
        dimensionBlendEnabled = dimBlendSection.getBoolean("enable");
        blendFallOff = dimBlendSection.getDouble("fallOffRate");

        ConfigurationSection worldLinksSection = Objects.requireNonNull(file.getConfigurationSection("worldConnections"), "World connections section missing");

        for (String s : worldLinksSection.getKeys(false)) {
            NetherLink newLink = new NetherLink(Objects.requireNonNull(worldLinksSection.getConfigurationSection(s)));
            if (!newLink.isValid()) {
                logger.warning("An invalid worldConnection was found in the config, please check that your world names are correct.");
                if (newLink.getOriginWorld() == null) {
                    logger.warning("No world with name \"%s\" exists (for the origin)", newLink.getOriginWorld().getName());
                }
                if(newLink.getDestinationWorld() == null) {
                    logger.warning("No world with name \"%s\" exists (for the destination)", newLink.getDestinationWorld().getName());
                }

                continue;
            }

            netherLinks.put(newLink.getOriginWorld(), newLink);
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

    public @Nullable NetherLink getWorldLink(@NotNull World originWorld) {
        return netherLinks.get(originWorld);
    }
}
