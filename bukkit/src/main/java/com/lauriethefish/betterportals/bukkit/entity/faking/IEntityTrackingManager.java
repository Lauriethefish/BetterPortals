package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Handles updating, creating and removing {@link EntityTracker}s based on when a player is viewing an entity, and sending animations based on events.
 */
public interface IEntityTrackingManager {
    /**
     * Replicates <code>entity</code> to <code>player</code>, as though it were projected through the portal.
     * @param entity Entity to replicate
     * @param portal Portal to replicate through
     * @param player Player to show the replicated entity to
     */
    void setTracking(Entity entity, IPortal portal, Player player);

    /**
     * Removes the replicated entity from <code>player</code>'s view, and stops sending update packets.
     * @param entity Entity to no longer be replicated
     * @param portal Portal that the entity was replicated through
     * @param player Player to stop replicating the entity for
     * @param sendPackets Whether or not to actually hide the entity for the player
     */
    void setNoLongerTracking(Entity entity, IPortal portal, Player player, boolean sendPackets);

    /**
     * Updates all currently replicated entities
     */
    void update();

    /**
     * Returns the tracker of <code>entity</code> on <code>portal</code>, or null if there is none.
     * @param portal The portal to check for trackers
     * @param entity The entity being tracked
     * @return The tracker of the entity, or null if there is none.
     */
    @Nullable IEntityTracker getTracker(IPortal portal, Entity entity);
}
