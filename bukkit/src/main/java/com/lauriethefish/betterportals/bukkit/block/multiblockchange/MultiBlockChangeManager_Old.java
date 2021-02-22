package com.lauriethefish.betterportals.bukkit.block.multiblockchange;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class MultiBlockChangeManager_Old implements IMultiBlockChangeManager   {
    private final Player player;
    private final Logger logger;
    private final HashMap<ChunkPosition, Map<Vector, WrappedBlockData>> changes = new HashMap<>();

    @Inject
    public MultiBlockChangeManager_Old(@Assisted Player player, Logger logger) {
        this.player = player;
        this.logger = logger;
    }

    @Override
    public void addChange(Vector position, WrappedBlockData newData) {
        ChunkPosition chunkPos = new ChunkPosition(position);

        Map<Vector, WrappedBlockData> existingList = changes.computeIfAbsent(chunkPos, k -> new HashMap<>());
        existingList.put(position, newData);
    }

    @Override
    public void sendChanges() {
        // Each chunk needs a different packet
        for(Map.Entry<ChunkPosition, Map<Vector, WrappedBlockData>> entry : changes.entrySet()) {
            logger.finest("Sending multi block change packet for chunk %s", entry.getKey());

            PacketContainer packet = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
            // Write the correct chunk section
            packet.getChunkCoordIntPairs().write(0, entry.getKey().toProtocolLib());

            // Add each changed block in the chunk
            int blockCount = entry.getValue().size();
            MultiBlockChangeInfo[] records = new MultiBlockChangeInfo[blockCount];
            int i = 0;
            for(Map.Entry<Vector, WrappedBlockData> blockEntry : entry.getValue().entrySet()) {
                records[i] = new MultiBlockChangeInfo(blockEntry.getKey().toLocation(null), blockEntry.getValue());
                i++;
            }

            packet.getMultiBlockChangeInfoArrays().write(0, records);
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }   catch(InvocationTargetException ex) {
                throw new RuntimeException("Error occurred while sending packet", ex);
            }
        }
    }
}