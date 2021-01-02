package com.lauriethefish.betterportals.bukkit.chunkloading;

import java.util.HashSet;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkPosition;

import org.bukkit.Chunk;

public class ModernChunkLoader implements ChunkLoader {
    // Used to only unforceload chunks if they are *loaded by the plugin*
    private Set<ChunkPosition> loadedChunks = new HashSet<>();

    @Override
    public void setForceLoaded(Chunk chunk) {
        chunk.setForceLoaded(true);
        loadedChunks.add(new ChunkPosition(chunk));
    }

    @Override
    public void setNotForceLoaded(ChunkPosition chunk) {
        if(loadedChunks.remove(chunk)) {
            // Do it this way to avoid loading the chunk by calling getChunk
            chunk.getWorld().setChunkForceLoaded(chunk.x, chunk.z, false);
        }
    }

    // Checks if the chunk is loaded by the plugin
    @Override
    public boolean isForceLoaded(ChunkPosition chunk) {
        return loadedChunks.contains(chunk);
    }   
}
