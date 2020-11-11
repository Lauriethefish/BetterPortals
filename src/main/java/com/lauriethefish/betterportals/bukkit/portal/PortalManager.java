package com.lauriethefish.betterportals.bukkit.portal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

// Structure for storing portals efficiently
public class PortalManager implements Listener {

    private PortalChunkMap allPortals;
    private PortalChunkMap loadedPortals;

    private Set<ChunkCoordIntPair> forceLoadedChunks = new HashSet<>(); // All chunks on the other side of portals that are forceloaded (this isn't used on 1.13 and up)

    public void registerPortal(Portal portal) {
        allPortals.addPortal(portal);

        // Load the portal if it is in a loaded chunk
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(portal.getOriginPos());
        if(chunk.isLoaded()) {
            onPortalLoad(portal, true);
            loadedPortals.addPortal(portal);
        }
    }

    // Removes the portal from the all and loaded portal maps
    public void unregisterPortal(Portal portal) {
        unregisterPortal(portal.getOriginPos());
    }

    public void unregisterPortal(Location loc) {
        loadedPortals.removePortal(loc);
        allPortals.removePortal(loc);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // Find if there are any portals in this chunk
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(event.getChunk());
        Map<Location, Portal> chunkPortals = allPortals.getPortalChunkMap(chunkPos);
        if(chunkPortals == null) {return;}

        boolean forceLoadChunks = !(event.getChunk().isForceLoaded() || forceLoadedChunks.contains(chunkPos));
        for(Portal portal : chunkPortals.values()) {
            onPortalLoad(portal, forceLoadChunks);
        }

        // If there are, add it to the active portals map
        loadedPortals.addPortalChunkMap(chunkPos, chunkPortals);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(event.getChunk());
        // Cancel the event if the chunk is force loaded
        if(forceLoadedChunks.contains(chunkPos)) {
            event.setCancelled(true);
            return;
        }

        // Remove portals in this chunk from the active portals map
        Map<Location, Portal> unloadedPortals = loadedPortals.removePortalChunkMap(chunkPos);
        if(unloadedPortals == null) {return;}

        // Unforceload the destinations of any portals that are now unloaded
        for(Portal portal : unloadedPortals.values()) {
            onPortalUnload(portal);
        }
    }

    private void onPortalLoad(Portal portal, boolean forceLoadChunks) {
        if(forceLoadChunks) {
            for(ChunkCoordIntPair chunk : portal.getLoadedDestinationChunks()) {
                // Forceload the portal's destination chunks depending on the chunk loading method
                if(ReflectUtils.useNewChunkLoadingImpl) {
                    chunk.getChunk().setForceLoaded(true);
                }   else    {
                    forceLoadedChunks.add(chunk);
                    chunk.getChunk().load();
                }
            }
        }

        portal.onLoaded(); // Initialise info about the portal
    }

    private void onPortalUnload(Portal portal) {
        // Stop forceloading the destination chunks using the correct method
        for(ChunkCoordIntPair chunk : portal.getLoadedDestinationChunks()) {
            if(ReflectUtils.useNewChunkLoadingImpl) {
                chunk.getChunk().setForceLoaded(false);
            }   else    {
                forceLoadedChunks.remove(chunk);
            }
        }
    }
}
