package com.lauriethefish.betterportals.bukkit.chunkloading;

import java.util.HashSet;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;

import org.bukkit.Chunk;

public class ModernChunkLoader implements ChunkLoader {
    // Used to only unforceload chunks if they are *loaded by the plugin*
    private Set<ChunkCoordIntPair> loadedChunks = new HashSet<>();

    @Override
    public void setForceLoaded(Chunk chunk) {
        chunk.setForceLoaded(true);
        loadedChunks.add(new ChunkCoordIntPair(chunk));
    }

    @Override
    public void setNotForceLoaded(ChunkCoordIntPair chunk) {
        if(loadedChunks.remove(chunk)) {
            // Do it this way to avoid loading the chunk by calling getChunk
            chunk.getWorld().setChunkForceLoaded(chunk.x, chunk.z, false);
        }
    }

    // Checks if the chunk is loaded by the plugin
    @Override
    public boolean isForceLoaded(ChunkCoordIntPair chunk) {
        return loadedChunks.contains(chunk);
    }   
}
