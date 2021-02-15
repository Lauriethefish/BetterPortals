package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
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
}
