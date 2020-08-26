package com.lauriethefish.betterportals.multiblockchange;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Allows you to add blocks to a HashMap that will then be divided to be sent to the player
public class MultiBlockChangeManager_Old implements MultiBlockChangeManager {
    private Object playerConnection;
    // Stores the changes, separated out into chunks
    private HashMap<ChunkCoordIntPair, HashMap<Vector, Object>> changes = new HashMap<>();

    public MultiBlockChangeManager_Old(Player player)   {
        Object craftPlayer = ReflectUtils.runMethod(player, "getHandle");
        playerConnection = ReflectUtils.getField(craftPlayer, "playerConnection");
    }

    // Adds a new block to the HashMap
    public void addChange(Vector location, Object newType)  {
        // Add a hashmap for this chunk if one doesn't already exist
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(location);
        if(changes.get(chunk) == null)  {
            changes.put(chunk, new HashMap<>());
        }

        changes.get(chunk).put(location, newType);
    }

    // Sends all the queued changes
    public void sendChanges()   {
        for(Map.Entry<ChunkCoordIntPair, HashMap<Vector, Object>> entry : changes.entrySet())   {
            sendMultiBlockChange(entry.getValue(), entry.getKey());
        }
    }

    // Constructs a multiple block change packet from the given blocks, and sends it to the player
    // All the blocks MUST be in the same chunk
    private void sendMultiBlockChange(Map<Vector, Object> blocks, ChunkCoordIntPair chunk) {
        // Make a new PacketPlayOutMultiBlockChange
        Class<?> packetClass = ReflectUtils.getMcClass("PacketPlayOutMultiBlockChange");
        Object packet = ReflectUtils.newInstance(packetClass);

        // Find the coords of the chunk
        Object chunkCoords = chunk.toNMS();
        
        ReflectUtils.setField(packet, "a", chunkCoords);

        // Loop through each block in the map
        Class<?> infoClass = ReflectUtils.getMcClass("PacketPlayOutMultiBlockChange$MultiBlockChangeInfo");
        Object array = Array.newInstance(infoClass, blocks.size());
        int i = 0;
        for(Map.Entry<Vector, Object> entry : blocks.entrySet())   {
            Vector loc = entry.getKey();
            // Find the chunk relative position
            int x = loc.getBlockX() & 15;
            int z = loc.getBlockZ() & 15;

            // Make the NMS MultiBlockChangeInfo object
            Object info = ReflectUtils.newInstance(infoClass, new Class[]{packetClass, short.class, ReflectUtils.getMcClass("IBlockData")},
                                                new Object[]{packet, (short) (x << 12 | z << 8 | loc.getBlockY()), entry.getValue()});
            Array.set(array, i, info); i++;
        }

        // Set it in the packet
        ReflectUtils.setField(packet, "b", array);

        // Send the packet using more reflection stuff
        ReflectUtils.runMethod(playerConnection, "sendPacket", new Class[]{ReflectUtils.getMcClass("Packet")}, new Object[]{packet});
    }
}