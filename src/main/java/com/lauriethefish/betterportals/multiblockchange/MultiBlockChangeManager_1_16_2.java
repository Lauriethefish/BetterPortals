package com.lauriethefish.betterportals.multiblockchange;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Allows you to add blocks to a HashMap that will then be divided to be sent to the player
public class MultiBlockChangeManager_1_16_2 implements MultiBlockChangeManager {
    private Object playerConnection;
    // Stores the changes, separated out into chunk sections
    private HashMap<SectionPosition, HashMap<Vector, Object>> changes = new HashMap<>();

    public MultiBlockChangeManager_1_16_2(Player player)   {
        Object craftPlayer = ReflectUtils.runMethod(player, "getHandle");
        playerConnection = ReflectUtils.getField(craftPlayer, "playerConnection");
    }

    // Adds a new block to the HashMap
    public void addChange(Vector location, Object newType)  {
        // Add a hashmap for this chunk if one doesn't already exist
        SectionPosition section = new SectionPosition(location);
        if(changes.get(section) == null)  {
            changes.put(section, new HashMap<>());
        }

        changes.get(section).put(location, newType);
    }

    // Sends all the queued changes
    public void sendChanges()   {
        for(Map.Entry<SectionPosition, HashMap<Vector, Object>> entry : changes.entrySet())   {
            sendMultiBlockChange(entry.getValue(), entry.getKey());
        }
    }

    // Constructs a multiple block change packet from the given blocks, and sends it to the player
    // All the blocks MUST be in the same chunk section
    private void sendMultiBlockChange(Map<Vector, Object> blocks, SectionPosition section) {
        // Make a new PacketPlayOutMultiBlockChange
        Class<?> packetClass = ReflectUtils.getMcClass("PacketPlayOutMultiBlockChange");
        Object packet = ReflectUtils.newInstance(packetClass);
        // Set the SectionPosition
        ReflectUtils.setField(packet, "a", section.toNMS());

        Object dataArray = Array.newInstance(ReflectUtils.getMcClass("IBlockData"), blocks.size());
        Object shortArray = Array.newInstance(short.class, blocks.size());

        int i = 0;
        for(Map.Entry<Vector, Object> entry : blocks.entrySet())   {
            Vector loc = entry.getKey();
            // Find the chunk relative position
            int x = loc.getBlockX() & 0xF;
            int y = loc.getBlockY() & 0xF;
            int z = loc.getBlockZ() & 0xF;

            // Set the correct IBlockData and relative position as a short
            Array.set(dataArray, i, entry.getValue());
            Array.set(shortArray, i, (short) (x << 8 | z << 4 | y << 0));
            i++;
        }

        // Set it in the packet
        ReflectUtils.setField(packet, "b", shortArray);
        ReflectUtils.setField(packet, "c", dataArray);

        // Send the packet using more reflection stuff
        ReflectUtils.runMethod(playerConnection, "sendPacket", new Class[]{ReflectUtils.getMcClass("Packet")}, new Object[]{packet});
    }
}