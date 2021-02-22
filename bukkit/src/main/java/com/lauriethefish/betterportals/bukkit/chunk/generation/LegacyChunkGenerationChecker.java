package com.lauriethefish.betterportals.bukkit.chunk.generation;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

@Singleton
public class LegacyChunkGenerationChecker implements IChunkGenerationChecker    {
    // In older versions, we just have to use loadChunk
    @Override
    public boolean isChunkGenerated(@NotNull World world, int x, int z) {
        return world.loadChunk(x, z, false);
    }
}
