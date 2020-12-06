package com.lauriethefish.betterportals.bukkit;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

import lombok.Getter;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData implements Serializable   {
    private static final long serialVersionUID = -198098468444437819L;

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
    // Store the origin and destination IBlockData after they have been fetched once
    private transient Object originDataCache;
    private transient Object destDataCache;

    // Store the combined ID of the origin and destination data, since these can be serialized
    private int originDataCombinedId;
    private int destDataCombinedId;

    public BlockRaycastData(Vector originPos, int originCombinedId, int destCombinedID, boolean edge) {
        this.originVec = originPos;
        this.originDataCombinedId = originCombinedId;
        this.destDataCombinedId = edge ? edgeDataCombinedId : destCombinedID;
    }

    // Used when a block state only changes at the origin and not the destination
    public void changeOriginData(int newValue) {
        originDataCombinedId = newValue;
        originDataCache = null; // Invalidate the old cached data
    }

    // Fetches the NMS IBlockData if we need to, otherwise just returns the cached data
    public Object getOriginData() {
        if(originDataCache == null) {
            originDataCache = getNMSData(originDataCombinedId);
        }
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

    // Manually implement readObject and writeObject for efficiency and because Vector isn't serializable
    private void readObject(ObjectInputStream inputStream) throws IOException {
        originVec = new Vector(inputStream.readDouble(), inputStream.readDouble(), inputStream.readDouble());
        destDataCombinedId = inputStream.readInt();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.writeDouble(originVec.getX());
        outputStream.writeDouble(originVec.getY());
        outputStream.writeDouble(originVec.getZ());
        outputStream.writeInt(destDataCombinedId);
    }
}