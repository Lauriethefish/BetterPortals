package com.lauriethefish.betterportals;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.lauriethefish.betterportals.runnables.PlayerRayCast;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData {
    // Store the methods and fields for later, as this is performance sensitive
    private static Method NMS_BLOCK_METHOD = null;
    private static Field BLOCK_DATA_FIELD = null;

    // Fetches the NMS_BLOCK_METHOD and BLOCK_DATA_FIELD
    public void initReflection() {
        try {
            NMS_BLOCK_METHOD = ReflectUtils.getBukkitClass("block.CraftBlock").getMethod("getNMSBlock", (Class<?>[]) null);
            BLOCK_DATA_FIELD = ReflectUtils.getMcClass("Block").getField("blockData");
        } catch (NoSuchMethodException | NoSuchFieldException | SecurityException ex) {
            ex.printStackTrace();
        }
    }

    public Vector originVec;
    public Object originData;
    public Object destData;
    public BlockRaycastData(Location originLoc, Location destLoc, BlockData destData)   {
        this.originVec = PlayerRayCast.moveVectorToCenterOfBlock(originLoc.toVector());
        this.originData = getNMSData(originLoc.getBlock());
        this.destData = destData;
    }

    // Finds the NMS IBlockData from a bukkit block
    public Object getNMSData(Block block)   {
        // Find the methods if not found already
        if(NMS_BLOCK_METHOD == null)    {initReflection();}

        try {
            return BLOCK_DATA_FIELD.get(NMS_BLOCK_METHOD.invoke(block, (Object[]) null));
        }   catch(InvocationTargetException | IllegalAccessException ex)   {
            ex.printStackTrace();
            return null;
        }
    }
}