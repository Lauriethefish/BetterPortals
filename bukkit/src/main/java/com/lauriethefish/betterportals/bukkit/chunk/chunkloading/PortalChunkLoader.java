package com.lauriethefish.betterportals.bukkit.chunk.chunkloading;

import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.SquareChunkAreaIterator;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Used to load the destination chunks of a portal, this is done to make entities move
 */
@Singleton
public class PortalChunkLoader implements IPortalChunkLoader   {
    private final RenderConfig config;
    private final IChunkLoader chunkLoader;

    @Inject
    public PortalChunkLoader(RenderConfig config, IChunkLoader chunkLoader) {
        this.config = config;
        this.chunkLoader = chunkLoader;
    }

    private SquareChunkAreaIterator getAreaIterator(PortalPosition destPosition) {
        Location destLoc = destPosition.getLocation();
        // Return an iterator over the chunks within the box that is rendered to the player
        return new SquareChunkAreaIterator(destLoc.clone().subtract(config.getHalfFullSize().toVector()), destLoc.clone().add(config.getHalfFullSize().toVector()));
    }

    @Override
    public void forceloadPortalChunks(@NotNull PortalPosition destPosition) {
        if(destPosition.isExternal()) {return;}

        chunkLoader.forceLoadAllPos(getAreaIterator(destPosition));
    }

    @Override
    public void unforceloadPortalChunks(@NotNull PortalPosition destPosition) {
        if(destPosition.isExternal()) {return;}

        chunkLoader.unForceLoadAllPos(getAreaIterator(destPosition));
    }
}
