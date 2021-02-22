package com.lauriethefish.betterportals.bukkit.chunk.chunkloading;

import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import org.bukkit.Chunk;

import java.util.Iterator;

// Allows different implementations of chunk loading to be used
public interface IChunkLoader {
    // Makes the specified Chunk force loaded
    void setForceLoaded(Chunk chunk);

    default void setForceLoaded(ChunkPosition chunk) {
        setForceLoaded(chunk.getChunk());
    }

    // Various methods for doing it with iterators
    default void forceLoadAllPos(Iterator<? extends ChunkPosition> iterator) {
        while(iterator.hasNext()) {
            setForceLoaded(iterator.next().getChunk());
        }
    }

    default void forceLoadAll(Iterator<? extends Chunk> iterator) {
        while(iterator.hasNext()) {
            setForceLoaded(iterator.next());
        }
    }

    // Unforceloading uses the ChunkCoordIntPair since otherwise you'd be forced to get the chunk, reloading the chunk in the case that it were unloaded
    void setNotForceLoaded(ChunkPosition chunk);
    default void setNotForceLoaded(Chunk chunk) {
        setNotForceLoaded(new ChunkPosition(chunk));
    }

    // Various methods for doing it with iterators
    default void unForceLoadAllPos(Iterator<? extends ChunkPosition> iterator) {
        while(iterator.hasNext()) {
            setNotForceLoaded(iterator.next());
        }
    }

    default void unForceLoadAll(Iterator<? extends Chunk> iterator) {
        while(iterator.hasNext()) {
            setNotForceLoaded(new ChunkPosition(iterator.next()));
        }
    }

    // Checks if the chunk is currently force loaded
    boolean isForceLoaded(ChunkPosition chunk);
    default boolean isForceLoaded(Chunk chunk) {
        return isForceLoaded(new ChunkPosition(chunk));
    }
}
