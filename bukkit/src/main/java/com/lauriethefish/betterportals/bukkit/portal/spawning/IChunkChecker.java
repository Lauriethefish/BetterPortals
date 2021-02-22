package com.lauriethefish.betterportals.bukkit.portal.spawning;

import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks a chunk for either existing or new {@link PortalSpawnPosition}s.
 */
public interface IChunkChecker {
    /**
     * Finds the closest valid position in <code>chunk</code>.
     * @param chunk The chunk to check
     * @param context The preferred spawn position and size
     * @return The closest position in the chunk to the context's preferred position, or null if there is none.
     */
    @Nullable PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context);
}
