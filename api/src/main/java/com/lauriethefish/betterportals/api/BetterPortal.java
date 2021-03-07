package com.lauriethefish.betterportals.api;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a portal created by the BetterPortals plugin, or through the API
 */
public interface BetterPortal {
    /**
     * @return The unique ID of this portal. This is persistent throughout server restarts.
     */
    @NotNull UUID getId();

    /**
     * @return The unique ID of this portal's owner. Null if nobody owns it, or it is a nether portal.
     */
    @Nullable UUID getOwnerId();

    /**
     * @return The name of this portal, null if unnamed.
     */
    @Nullable String getName();

    /**
     * Sets the name of this portal
     * @param name The new name, or <code>null</code> in order to remove the name
     * @throws IllegalStateException if this portal is a nether portal.
     */
    void setName(@Nullable String name);

    /**
     * @return The origin position of this portal
     */
    @NotNull PortalPosition getOriginPos();

    /**
     * @return The destination position of this portal
     */
    @NotNull PortalPosition getDestPos();

    /**
     * Gets the size of the portal.
     * The size does <i>not</i> include the frame. e.g. a default nether portal would be (2, 3, 0).
     * The Z coordinate is always zero
     *
     * @return The size of this portal.
     */
    @NotNull Vector getSize();

    /**
     * Finds if this portal is a cross-server (bungeecord) portal.
     * @return If this portal is cross-server
     */
    boolean isCrossServer();

    /**
     * Finds if this portal is a custom portal.
     * Custom portals behave differently since they are not automatically removed if the portal blocks are missing.
     * Custom portals can also have a name set, while nether portals cannot.
     * @return If this portal is a custom portal
     */
    boolean isCustom();

    /**
     * Finds if this portal is a nether portal.
     * Nether portals are automatically unregistered if the portal blocks at the origin or destination are missing.
     * @return If this portal is a nether portal
     */
    default boolean isNetherPortal() {
        return !isCustom();
    }

    /**
     * Removes this portal, players will no longer be able to see through it or teleport.
     * @param removeOtherDirection Whether any portals that go from the destination of this portal will also be removed.
     */
    void remove(boolean removeOtherDirection);
}
