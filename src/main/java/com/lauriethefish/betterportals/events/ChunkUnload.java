package com.lauriethefish.betterportals.events;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.multiblockchange.ChunkCoordIntPair;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkUnload implements Listener {
    private BetterPortals pl;
    public ChunkUnload(BetterPortals pl)    {
        this.pl = pl;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event)   {
        // If the chunk is force loaded as it is an active portal destination, cancel the event
        if(pl.forceLoadedChunks.contains(new ChunkCoordIntPair(event.getChunk()))) {
            event.setCancelled(true);
        }
    }
}
