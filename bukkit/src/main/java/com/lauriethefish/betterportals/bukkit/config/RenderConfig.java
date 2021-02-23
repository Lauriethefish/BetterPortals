package com.lauriethefish.betterportals.bukkit.config;

import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.math.IntVector;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.Objects;

@Singleton
@Getter
public class RenderConfig {
    private double minXZ;
    private double maxXZ;
    private double minY;
    private double maxY;

    private int yMultip;
    private int zMultip;
    private int totalArrayLength;

    private IntVector halfFullSize;

    private final IntVector[] surroundingOffsets = new IntVector[]{
        new IntVector(1, 0, 0),
        new IntVector(-1, 0, 0),
        new IntVector(0, 1, 0),
        new IntVector(0, -1, 0),
        new IntVector(0, 0, 1),
        new IntVector(0, 0, -1),
    };

    private Vector collisionBox;
    private int blockUpdateInterval;

    private int worldSwitchWaitTime;

    @Getter private boolean portalBlocksHidden;

    private int blockStateRefreshInterval;

    public void load(FileConfiguration file) {
        maxXZ = file.getInt("portalEffectSizeXZ");
        minXZ = maxXZ * -1.0;
        maxY = file.getInt("portalEffectSizeY");
        minY = maxY * -1.0;

        if(maxXZ <= 0 || maxY <= 0) {
            throw new IllegalArgumentException("The portal effect size must be at least one");
        }

        zMultip = (int) (maxXZ - minXZ + 1);
        yMultip = zMultip * zMultip;
        totalArrayLength = yMultip * (int) (maxY - minY + 1);

        halfFullSize = new IntVector((maxXZ - minXZ) / 2, (maxY - minY) / 2, (maxXZ - minXZ) / 2);

        ConfigurationSection cBoxSection = Objects.requireNonNull(file.getConfigurationSection("portalCollisionBox"), "Collision box missing");
        collisionBox = new Vector(
                cBoxSection.getDouble("x"),
                cBoxSection.getDouble("y"),
                cBoxSection.getDouble("z")
        );

        blockUpdateInterval = file.getInt("portalBlockUpdateInterval");
        if(blockUpdateInterval <= 0) {
            throw new IllegalArgumentException("Block update interval must be at least 1");
        }

        worldSwitchWaitTime = file.getInt("waitTimeAfterSwitchingWorlds"); // TODO: implement or yeet
        portalBlocksHidden = file.getBoolean("hidePortalBlocks");
        blockStateRefreshInterval = file.getInt("blockStateRefreshInterval");
    }

    public boolean isEdge(int x, int y, int z) {
        return x == minXZ || x == maxXZ || y == minY || y == maxY || z == minXZ || z == maxXZ;
    }

    public boolean isEdge(IntVector vec) {
        return isEdge(vec.getX(), vec.getY(), vec.getZ());
    }
}
