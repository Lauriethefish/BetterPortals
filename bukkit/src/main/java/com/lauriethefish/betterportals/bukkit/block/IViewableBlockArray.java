package com.lauriethefish.betterportals.bukkit.block;

import com.comphenix.protocol.events.PacketContainer;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentMap;

/**
 * Creates a map of the blocks around the portal within the view distance.
 * Implements skipping of blocks that are fully covered by opaque blocks.
 * Implements skipping of blocks that are the same at the origin and destination.
 */
public interface IViewableBlockArray {
    /**
     * Updates the block array, and with the interval defined in {@link RenderConfig}
     * The first time this is called, it takes quite a bit longer than afterwards
     * @param ticksSinceActivated Ticks since the portal became viewable to one player/
     */
    void update(int ticksSinceActivated);

    /**
     * Gets the current map of viewable states
     * The {@link Vector} is the position at the origin of the portal, and is always in the center of the block.
     * The map must be safe to iterate over by another thread.
     * @return The current map of viewable states.
     */
    ConcurrentMap<IntVector, ViewableBlockInfo> getViewableStates();


    /**
     * Finds if the origin block stored at <code>position</code> is mapped as a tile entity.
     * If it is, the pre-fetched (on the main thread) packet used to set the data of this entity is returned.
     * Otherwise, <code>null</code> is returned
     * @param position The absolute position of the tile entity
     * @return The packet used to update.
     */
    @Nullable PacketContainer getOriginTileEntityPacket(@NotNull IntVector position);

    /**
     * Finds if the destination block stored at <code>position</code> is mapped as a tile entity.
     * If it is, the pre-fetched (on the main thread) packet used to set the data of this entity is returned.
     * Otherwise, <code>null</code> is returned
     * @param position The absolute position of the tile entity
     * @return The packet used to update.
     */
    @Nullable PacketContainer getDestinationTileEntityPacket(@NotNull IntVector position);


    /**
     * Clears the currently rendered array to save memory.
     * Called on portal deactivation.
     * Next time {@link IViewableBlockArray#update(int)} is called, another initial update will be done, which takes longer.
     */
    void reset();

    interface Factory {
        IViewableBlockArray create(IPortal portal);
    }
}
