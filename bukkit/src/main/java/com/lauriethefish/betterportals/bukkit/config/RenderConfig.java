package com.lauriethefish.betterportals.bukkit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import lombok.Getter;

// Technical config options relating to rendering/block arrays.
@Getter
public class RenderConfig {
    // The min and max values for the blocks that the raycast will check
    private double minXZ;
    private double maxXZ;
    private double minY;
    private double maxY;

    // Multiplyers used to access the array of changed blocks
    // This array stores the ghost blocks that have been changed to help performance
    private int yMultip;
    private int zMultip;

    private Vector halfFullSize;

    private int arraySizeXZ;
    private int arraySizeY;
    private int totalArrayLength;

    private int[] surroundingOffsets;

    private Vector collisionBox; // The offset of the portal's collision box
    private int blockUpdateInterval; // How often the portal re-checks its surrounding blocks

    private int worldSwitchWaitTime; // How long the plugin waits before rendering portals after switching worlds

    @Getter private boolean portalBlocksHidden; // If this is true then we will send packets to hide and show the portal blocks

    public RenderConfig(FileConfiguration file) {
        // Calculate the min and max values
        maxXZ = file.getInt("portalEffectSizeXZ");
        minXZ = maxXZ * -1.0;
        maxY = file.getInt("portalEffectSizeY");
        minY = maxY * -1.0;

        // Calculate the multipliers for accessing the table
        zMultip = (int) (maxXZ - minXZ + 1);
        yMultip = zMultip * zMultip;
        totalArrayLength = yMultip * (int) (maxY - minY + 1);

        // Calculate the differences in index for quickly accessing the block array while building the mesh
        surroundingOffsets = new int[]{
            1,
            -1,
            yMultip,
            -yMultip,
            zMultip,
            -zMultip
        };
        halfFullSize = new Vector((maxXZ - minXZ) / 2, (maxY - minY) / 2, (maxXZ - minXZ) / 2);

        // Load the portal's collision box
        ConfigurationSection cBoxSection = file.getConfigurationSection("portalCollisionBox");
        collisionBox = new Vector(
            cBoxSection.getDouble("x"),
            cBoxSection.getDouble("y"),
            cBoxSection.getDouble("z")
        );

        blockUpdateInterval = file.getInt("portalBlockUpdateInterval");
        worldSwitchWaitTime = file.getInt("waitTimeAfterSwitchingWorlds");
        portalBlocksHidden = file.getBoolean("hidePortalBlocks");
    }

    // Finds the index in an array of blocks surrounding the portal
    // Coordinates should be relative to the centre of the box
    public int calculateBlockArrayIndex(double x, double y, double z)  {
        return (int) (z * zMultip + y * yMultip + x) + totalArrayLength / 2;
    }

    // Essentially just does the opposite of the above
    public Vector calculateRelativePos(int index) {
        int x = Math.floorMod(index, zMultip);
        index -= x;
        int z = Math.floorMod(index, yMultip) / zMultip;
        index -= z * zMultip;
        int y = index / yMultip;
        index -= y * yMultip;

        return new Vector(x, y, z).subtract(halfFullSize);
    }
}
