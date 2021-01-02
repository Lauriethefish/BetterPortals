package com.lauriethefish.betterportals.bukkit.chunkloading;

import java.util.Iterator;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import com.lauriethefish.betterportals.bukkit.chunkloading.chunkpos.ChunkPosition;

import org.bukkit.Chunk;

// Allows different implementations of chunk loading to be used
public interface ChunkLoader {
    static ChunkLoader newInstance(BetterPortals pl) {
        if(ReflectUtils.isLegacy) {
            return new LegacyChunkLoader(pl);
        }   else    {
            return new ModernChunkLoader();
        }
    }

    // Makes the specified Chunk force loaded
    void setForceLoaded(Chunk chunk);

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
