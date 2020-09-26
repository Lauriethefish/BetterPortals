package com.lauriethefish.betterportals;

import java.lang.reflect.Method;

import com.lauriethefish.betterportals.math.MathUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

import lombok.Getter;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData {
    private static Object edgeData = makeEdgeData();
    private static Object makeEdgeData()    {
        // Different colours of concrete depend on version
        if(ReflectUtils.isLegacy)  {
            return getNMSData(Material.valueOf("CONCRETE"), (byte) 15);
        }   else    {
            return getNMSData(Material.BLACK_CONCRETE);
        }
    }

    @Getter private Vector originVec;
    @Getter private Object originData;
    @Getter private Object destData;
    public BlockRaycastData(BlockRotator rotator, Location originLoc, Location destLoc, boolean edge)   {
        // Find the location at the exact center of the origin block, used for portal intersection checking
        this.originVec = MathUtils.moveVectorToCenterOfBlock(originLoc.toVector());
        this.originData = getNMSData(originLoc.getBlock().getState()); // Find the IBlockData in the origin block

        // Rotate the block at the other side if we need to, so it is at the origin
        BlockState destBlockState = destLoc.getBlock().getState();
        rotator.rotateToOrigin(destBlockState);
        this.destData = edge ? edgeData : getNMSData(destBlockState);
    }

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
}