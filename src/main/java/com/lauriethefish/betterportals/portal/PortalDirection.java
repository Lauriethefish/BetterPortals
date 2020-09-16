package com.lauriethefish.betterportals.portal;

import org.bukkit.Location;
import org.bukkit.util.Vector;

// Two directions of a portal, used for more ergonomic handling
public enum PortalDirection {
    UP(new Vector(0.0, 1.0, 0.0), new Vector(1.0, 0.0, 0.0)),
    DOWN(new Vector(0.0, -1.0, 0.0), new Vector(1.0, 0.0, 0.0)),
    NORTH(new Vector(0.0, 0.0, 1.0), new Vector(0.0, 1.0, 0.0)),
    SOUTH(new Vector(0.0, 0.0, -1.0), new Vector(0.0, 1.0, 0.0)),
    EAST(new Vector(1.0, 0.0, 0.0), new Vector(0.0, 1.0, 0.0)),
    WEST(new Vector(-1.0, 0.0, 0.0), new Vector(0.0, 1.0, 0.0));

    // The direction vector is a normal facing out of the portal
    private final Vector direction;
    // This vector is used if we need to rotate the portal view round 180 degrees because the origin and destination are in exact opposite direction
    private final Vector inversionRotationAxis;
    private PortalDirection(Vector direction, Vector inversionRotationAxis)   {
        this.direction = direction;
        this.inversionRotationAxis = inversionRotationAxis;
    }

    // Gets a PortalDirection from the given string, and converts the old EAST_WEST and NORTH_SOUTH directions into the new variants
    public static PortalDirection fromStorage(String string) {
        switch(string) {
            case "EAST_WEST":
                return NORTH;
            case "NORTH_SOUTH":
                return EAST;
            default:
                return valueOf(string);
        }
    }

    public Vector toVector()    {
        return direction;
    }

    public Vector getInversionRotationAxis()    {
        return inversionRotationAxis;
    }

    // Swaps the coordinates of the given vector if this portal is on the NORTH_SOUTH or UP_DOWN axis
    // This is used when changing blocks around portals, since we cannot just use the coordinates regularly
    public Vector swapVector(Vector vec)    {
        switch(this)    {
            case EAST:
                return new Vector(vec.getZ(), vec.getY(), vec.getX());
            case UP:
                return new Vector(vec.getX(), vec.getZ(), vec.getY());
            case NORTH:
                return vec.clone();
            case WEST:
                return new Vector(vec.getZ(), vec.getY(), vec.getX());
            case DOWN:
                return new Vector(vec.getX(), vec.getZ(), vec.getY());
            case SOUTH:
                return vec.clone();
        }
        return null;
    }

    public Location swapLocation(Location loc)  {
        return swapVector(loc.toVector()).toLocation(loc.getWorld());
    }
}