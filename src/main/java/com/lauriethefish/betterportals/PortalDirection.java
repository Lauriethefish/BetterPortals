package com.lauriethefish.betterportals;

import org.bukkit.util.Vector;

// Two directions of a portal, used for more ergonomic handling
public enum PortalDirection {
    NORTH_SOUTH(new Vector(1.0, 0.0, 0.0)), // Along the z axis
    UP_DOWN(new Vector(0.0, 1.0, 0.0)), // Along the y axis
    EAST_WEST(new Vector(0.0, 0.0, 1.0)); // Along the x axis

    // The direction vector is a normal facing out of the portal
    private final Vector direction;
    private PortalDirection(Vector direction)   {
        this.direction = direction;
    }

    public Vector toVector()    {
        return direction;
    }

    // Swaps the coordinates of the given vector if this portal is on the NORTH_SOUTH or UP_DOWN axis
    // This is used when changing blocks around portals, since we cannot just use the coordinates regularly
    public Vector swapVector(Vector vec)    {
        switch(this)    {
            case NORTH_SOUTH:
                return new Vector(vec.getZ(), vec.getY(), vec.getX());
            case UP_DOWN:
                return new Vector(vec.getX(), vec.getZ(), vec.getY());
            case EAST_WEST:
                return vec.clone();
        }
        return null;
    }
}