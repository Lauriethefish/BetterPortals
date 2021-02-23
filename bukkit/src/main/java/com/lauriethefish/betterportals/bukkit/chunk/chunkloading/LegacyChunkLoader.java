package com.lauriethefish.betterportals.bukkit.chunk.chunkloading;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.bukkit.events.IEventRegistrar;
import org.bukkit.Chunk;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class LegacyChunkLoader implements IChunkLoader, Listener {
    private final Set<ChunkPosition> loadedChunks = new HashSet<>();

    @Inject
    public LegacyChunkLoader(IEventRegistrar eventRegistrar) {
        eventRegistrar.register(this);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkPosition chunkPos = new ChunkPosition(event.getChunk());
        if(loadedChunks.contains(chunkPos)) {
            ((Cancellable) event).setCancelled(true); // This is cancellable on 1.12 and 1.13
        }
    }

    @Override
    public void setForceLoaded(Chunk chunk) {
        loadedChunks.add(new ChunkPosition(chunk));
        chunk.load();
    }

    @Override
    public void setNotForceLoaded(@NotNull ChunkPosition chunk) {
    loadedChunks.remove(chunk);
    }

    @Override
    public boolean isForceLoaded(@NotNull ChunkPosition chunk) {
        return loadedChunks.contains(chunk);
    }
}
