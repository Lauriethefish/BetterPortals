package com.lauriethefish.betterportals;

import java.lang.reflect.Method;

import com.lauriethefish.betterportals.math.MathUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData {
    public static Object edgeData = makeEdgeData();
    private static Object makeEdgeData()    {
        // Different colours of concrete depend on version
        if(ReflectUtils.isLegacy)  {
            return getNMSData(Material.valueOf("CONCRETE"), (byte) 15);
        }   else    {
            return getNMSData(Material.BLACK_CONCRETE);
        }
    }

    public Vector originVec;
    public Object originData;
    public Object destData;
    public BlockRaycastData(Location originLoc, Location destLoc, boolean edge)   {
        this.originVec = MathUtils.moveVectorToCenterOfBlock(originLoc.toVector());
        this.originData = getNMSData(originLoc.getBlock());
        this.destData = edge ? edgeData : getNMSData(destLoc.getBlock());
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

    // Find the method used to get the NMS IBlockData from a Block
    private static Method getDataMethod = findGetDataMethod();
    private static Method findGetDataMethod()  {
        if(ReflectUtils.isLegacy)   {
            Method method = ReflectUtils.findMethod(ReflectUtils.getBukkitClass("block.CraftBlock"), "getData0", new Class[]{});
            method.setAccessible(true);
            return method;
        }   else    {
            Method method = ReflectUtils.findMethod(ReflectUtils.getBukkitClass("block.CraftBlock"), "getNMS", new Class[]{});
            method.setAccessible(true);
            return method;
        }
    }

    // Finds the NMS IBlockData from a bukkit block
    public static Object getNMSData(Block block)   {
        try {
            return getDataMethod.invoke(block);
        } catch(ReflectiveOperationException ex)    {
            ex.printStackTrace();
            return null;
        }
    }
}