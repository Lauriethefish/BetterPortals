package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents each fake entity for each portal, may be shared between multiple players.
 */
public interface IEntityTracker {
    /**
     * Shows the replicated entity to <code>player</code>.
     * @param player The entity to send spawn/update packets to.
     */
    void addTracking(@NotNull Player player);

    /**
     * @return The tracking info for this entity
     */
    @NotNull EntityInfo getEntityInfo();

    /**
     * @return The portal that this tracker is tracking for
     */
    @NotNull IPortal getPortal();

    /**
     * Removes the replicated entity for <code>player</code>.
     * @param player The player to remove the entity for
     * @param sendPackets Whether to send a hide packet to remove the entity from the player's perspective.
     */
    void removeTracking(@NotNull Player player, boolean sendPackets);

    /**
     * @return The number of players currently tracking this entity.
     */
    int getTrackingPlayerCount();

    /**
     * Sends packets to update the movement, equipment, etc. of the entity.
     */
    void update();

    /**
     * Called whenever the {@link IEntityTrackingManager} gets an event fired that requires an animation to be shown.
     * @param animationType The animation to show
     */
    void onAnimation(@NotNull AnimationType animationType);

    /**
     * Called whenever the {@link IEntityTrackingManager} detects that an item that is also tracked was picked up.
     * @param pickedUp The info of the entity that got picked up
     */
    void onPickup(@NotNull EntityInfo pickedUp);

    interface Factory {
        IEntityTracker create(Entity entity, IPortal portal);
    }
}
