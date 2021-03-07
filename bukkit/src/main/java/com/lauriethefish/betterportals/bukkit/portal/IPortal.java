package com.lauriethefish.betterportals.bukkit.portal;

import com.lauriethefish.betterportals.api.BetterPortal;
import com.lauriethefish.betterportals.api.PortalPosition;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformations;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents each direction of each portal - this is to allow one way portals and it generally makes stuff easier to handle.
 */
public interface IPortal extends BetterPortal {
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
     * Gets the end of the permissions for this portal.
     * e.g. <code>nether.world_nether</code> or <code>custom.myPortal</code>
     * @return The portals permission path, without the prefix
     */
    String getPermissionPath();

    /**
     * @return If this portal is registered and can be viewed/used by players
     */
    boolean isRegistered();
}
