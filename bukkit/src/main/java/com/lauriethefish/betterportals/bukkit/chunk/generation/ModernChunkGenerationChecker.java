package com.lauriethefish.betterportals.bukkit.chunk.generation;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;

@Singleton
public class ModernChunkGenerationChecker implements IChunkGenerationChecker    {
    // In newer versions we can just call isChunkGenerated to check this
    @Override
    public boolean isChunkGenerated(@NotNull World world, int x, int z) {
        return world.isChunkGenerated(x, z);
    }
}
