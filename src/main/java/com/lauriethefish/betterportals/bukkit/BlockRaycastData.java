package com.lauriethefish.betterportals.bukkit;

import java.lang.reflect.Method;

import com.lauriethefish.betterportals.bukkit.math.MathUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

import lombok.Getter;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData {
    private static int edgeDataCombinedId = makeEdgeData();

    private static int makeEdgeData() {
        // Different colours of concrete depend on version if(ReflectUtils.isLegacy)
        if(ReflectUtils.isLegacy)   {
            return getCombinedId(Material.valueOf("CONCRETE"), (byte) 15);
        }   else    {
            return getCombinedId(Material.BLACK_CONCRETE, (byte) 0);
        }
    }

    @Getter private Vector originVec;
    private transient Object originDataCache;
    private transient Object destDataCache;
    private int originDataCombinedId;
    private int destDataCombinedId;

    public BlockRaycastData(BlockRotator rotator, Location originLoc, Location destLoc, boolean edge) {
        // Find the location at the exact center of the origin block, used for portal
        // intersection checking
        this.originVec = MathUtils.moveToCenterOfBlock(originLoc.toVector());
        this.originDataCombinedId = getCombinedId(originLoc.getBlock().getState()); // Find the IBlockData in the origin block

        // Rotate the block at the other side if we need to, so it is at the origin
        BlockState destBlockState = destLoc.getBlock().getState();
        rotator.rotateToOrigin(destBlockState);
        this.destDataCombinedId = edge ? edgeDataCombinedId : getCombinedId(destBlockState);
    }

    // Fetches the NMS IBlockData if we need to, otherwise just returns the cached data
    public Object getOriginData() {
        if(originDataCache == null) {originDataCache = getNMSData(originDataCombinedId);}
        return originDataCache;
    }

    public Object getDestData() {
        if(destDataCache == null) {destDataCache = getNMSData(destDataCombinedId);}
        return destDataCache;
    }

    // Finds the combined block ID and data from a BlockState
    @SuppressWarnings("deprecation")
    public static int getCombinedId(BlockState state) {
        return getCombinedId(state.getType(), state.getRawData());
    }

    // Finds the combined ID for a material and raw data byte
    @SuppressWarnings("deprecation")
    private static int getCombinedId(Material material, byte rawData) {
        return material.getId() + (rawData << 12);
    }

    private static Method getNMSDataFromIdMethod = ReflectUtils.findMethod(ReflectUtils.getMcClass("Block"), "getByCombinedId",
            new Class[] { int.class });
    
    // Finds the NMS IBlockData object from a combined block ID and data
    private static Object getNMSData(int combinedId) {
        try {
            return getNMSDataFromIdMethod.invoke(null, combinedId);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object getNMSData(Material material, byte rawData) {
        return getNMSData(getCombinedId(material, rawData));
    }

    public static Object getNMSData(Material material) {
        return getNMSData(material, (byte) 0);
    }

    public static Object getNMSData(BlockState state) {
        return getNMSData(getCombinedId(state));
    }

    /*
    public static Object getNMSData(Material mat)  {
        Object block = ReflectUtils.runMethod(null, ReflectUtils.getBukkitClass("util.CraftMagicNumbers"), "getBlock", new Class[]{Material.class}, new Object[]{mat});
        return ReflectUtils.getField(block, "blockData");
    }

    @SuppressWarnings("deprecation")
    public static Object getNMSData(Material mat, byte data)    {
        int combinedId = mat.getId() + (data << 12);
        return ReflectUtils.runMethod(null, ReflectUtils.getMcClass("Block"), "getByCombinedId", new Class[]{int.class}, new Object[]{combinedId});
    }

    private static Method getDataMethod = findGetDataMethod();
    private static Method findGetDataMethod()  {
        if(ReflectUtils.isLegacy)   {
            return ReflectUtils.findMethod(ReflectUtils.getMcClass("Block"), "getByCombinedId", new Class[]{int.class});
        }   else    {
            return ReflectUtils.findMethod(ReflectUtils.getBukkitClass("block.CraftBlockState"), "getHandle", new Class[]{});
        }
    }

    // Finds the NMS IBlockData from a bukkit block state
    @SuppressWarnings("deprecation")
    public static Object getNMSData(BlockState state)   {
        try {
            // Use the combinedId to get IBlockData in legacy versions, or just use getHandle on a BlockState in modern versions
            if(ReflectUtils.isLegacy)    {
                int combined = state.getType().getId() + (state.getRawData() << 12);
                return getDataMethod.invoke(null, combined);
            }   else    {
                return getDataMethod.invoke(state);
            }
        }   catch(ReflectiveOperationException ex)  {
            ex.printStackTrace();
            return null;
        }
    }
    */
}