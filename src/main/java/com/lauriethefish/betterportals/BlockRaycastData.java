package com.lauriethefish.betterportals;

import com.lauriethefish.betterportals.runnables.PlayerRayCast;

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
        this.originVec = PlayerRayCast.moveVectorToCenterOfBlock(originLoc.toVector());
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

    // Finds the NMS IBlockData from a bukkit block, this differs depending on if using a legacy version or modern version
    public static Object getNMSData(Block block)   {
        if(ReflectUtils.isLegacy)  {
            return ReflectUtils.runMethod(block, "getData0");
        }   else    {
            return ReflectUtils.getField(block.getBlockData(), "state");
        }
    }
}