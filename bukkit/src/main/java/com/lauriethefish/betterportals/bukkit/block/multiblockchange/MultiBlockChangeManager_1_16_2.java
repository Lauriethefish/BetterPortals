package com.lauriethefish.betterportals.bukkit.block.multiblockchange;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class MultiBlockChangeManager_1_16_2 implements IMultiBlockChangeManager   {
    private final Player player;
    // Section positions have to be done with BlockPositions for now in ProtocolLib
    private final HashMap<BlockPosition, Map<Vector, WrappedBlockData>> changes = new HashMap<>();

    @Inject
    public MultiBlockChangeManager_1_16_2(@Assisted Player player) {
        this.player = player;
    }

    @Override
    public void addChange(Vector position, WrappedBlockData newData) {
        BlockPosition sectionPosition = new BlockPosition(
                position.getBlockX() >> 4,
                position.getBlockY() >> 4,
                position.getBlockZ() >> 4
        );

        // Create/get the list for this chunk section
        Map<Vector, WrappedBlockData> existingList = changes.computeIfAbsent(sectionPosition, k -> new HashMap<>());
        existingList.put(position, newData);
    }

    private short getShortLocation(Vector vec) {
        int x = vec.getBlockX() & 0xF;
        int y = vec.getBlockY() & 0xF;
        int z = vec.getBlockZ() & 0xF;

        return (short) (x << 8 | z << 4 | y);
    }

    @Override
    public void sendChanges() {
        // Each chunk position needs a different packet
        for(Map.Entry<BlockPosition, Map<Vector, WrappedBlockData>> entry : changes.entrySet()) {
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            // Write the correct chunk section
            packet.getSectionPositions().write(0, entry.getKey());

            // Add each changed block in the chunk
            int blockCount = entry.getValue().size();
            WrappedBlockData[] data = new WrappedBlockData[blockCount];
            short[] positions = new short[blockCount];
            int i = 0;
            for(Map.Entry<Vector, WrappedBlockData> blockEntry : entry.getValue().entrySet()) {
                positions[i] = getShortLocation(blockEntry.getKey());
                data[i] = blockEntry.getValue();
                i++;
            }

            packet.getBlockDataArrays().writeSafely(0, data);
            packet.getShortArrays().writeSafely(0, positions);

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }   catch(InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
    }
}
