package com.lauriethefish.betterportals.bukkit.chunk.chunkloading;

import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Allows different implementations of chunk loading to be used
 */
public interface IChunkLoader {
    void setForceLoaded(Chunk chunk);

    default void setForceLoaded(ChunkPosition chunk) {
        setForceLoaded(chunk.getChunk());
    }

    default void forceLoadAllPos(@NotNull Iterator<? extends ChunkPosition> iterator) {
        while(iterator.hasNext()) {
            setForceLoaded(iterator.next().getChunk());
        }
    }

    default void forceLoadAll(@NotNull Iterator<? extends Chunk> iterator) {
        while(iterator.hasNext()) {
            setForceLoaded(iterator.next());
        }
    }

    /**
     * Unforceloading uses the ChunkCoordIntPair since otherwise you'd be forced to get the chunk, reloading the chunk in the case that it were unloaded
     * @param chunk The chunk position to unforceload
     */
    void setNotForceLoaded(@NotNull ChunkPosition chunk);


    default void setNotForceLoaded(@NotNull Chunk chunk) {
        setNotForceLoaded(new ChunkPosition(chunk));
    }

    // Various methods for doing it with iterators
    default void unForceLoadAllPos(@NotNull Iterator<? extends ChunkPosition> iterator) {
        while(iterator.hasNext()) {
            setNotForceLoaded(iterator.next());
        }
    }

    default void unForceLoadAll(@NotNull Iterator<? extends Chunk> iterator) {
        while(iterator.hasNext()) {
            setNotForceLoaded(new ChunkPosition(iterator.next()));
        }
    }

    /**
     * Checks if the chunk is currently force loaded by this chunk loader.
     * @param chunk The chunk to check if force loaded
     * @return Whether or not it is force loaded
     */
    boolean isForceLoaded(@NotNull ChunkPosition chunk);

    default boolean isForceLoaded(@NotNull Chunk chunk) {
        return isForceLoaded(new ChunkPosition(chunk));
    }
}
