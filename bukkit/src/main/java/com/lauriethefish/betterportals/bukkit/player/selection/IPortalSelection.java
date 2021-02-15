package com.lauriethefish.betterportals.bukkit.player.selection;

import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the selection of one end of a portal (origin or destination), by the player
 */
public interface IPortalSelection extends Cloneable {
    void setPositionA(@NotNull Location pos);
    void setPositionB(@NotNull Location pos);

    /**
     * @return The portal position, or null if no valid selection has been made
     */
    @Nullable PortalPosition getPortalPosition();

    /**
     * @return The portal size, or null if no valid selection has been made
     */
    @Nullable Vector getPortalSize();

    /**
     * Changes the direction of this selection to the opposite of what it currently is.
     * So west goes to east, north to south, up to down, etc.
     */
    void invertDirection();

    /**
     * @return True if this selection is at least 1x1 blocks in size, and is aligned along a plane
     */
    boolean isValid();

    IPortalSelection clone();
}
