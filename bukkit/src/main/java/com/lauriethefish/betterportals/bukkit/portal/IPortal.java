package com.lauriethefish.betterportals.bukkit.portal;

import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformations;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents each direction of each portal - this is to allow one way portals and it generally makes stuff easier to handle.
 */
public interface IPortal {
    /**
     * @return The unique ID of this portal
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
    void setName(@Nullable String name);

    @NotNull PortalPosition getOriginPos();
    @NotNull PortalPosition getDestPos();
    @NotNull Vector getSize();

    /**
     * Called every tick while this portal is activated, regardless of if a player is actually viewing through it
     * Should process things like entity teleportation - must happen regardless of if a player is actually seeing through the porta;
     */
    void onUpdate();

    /**
     * Called every tick while a player is viewing this portal.
     * Updates block arrays, entity trackers, etc.
     */
    void onViewUpdate();


    void onActivate();
    void onDeactivate();


    /**
     * Called when at least one player is viewing through the portal
     */
    void onViewActivate();

    /**
     * Called when no players are now viewing through the portal
     */
    void onViewDeactivate();

    boolean isCrossServer();
    boolean isCustom();
    default boolean isNetherPortal() {
        return !isCustom();
    }

    /**
     * @return Transformations for moving positions relative to this portal.
     */
    @NotNull PortalTransformations getTransformations();

    /**
     * @return The current viewable block array of the portal. Used for rendering
     */
    @NotNull IViewableBlockArray getViewableBlocks();

    /**
     * @return The lists of entities nearby this portal at the origin and destination
     */
    @NotNull IPortalEntityList getEntityList();

    /**
     * Removes this portal, players will no longer be able to see through it or teleport.
     * @param removeOtherDirection Whether any portals from the destination of this portal will also be removed
     */
    void remove(boolean removeOtherDirection);

    /**
     * @return If this portal is registered and can be viewed/used by players
     */
    boolean isRegistered();
}
