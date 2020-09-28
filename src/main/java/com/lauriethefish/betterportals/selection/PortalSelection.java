package com.lauriethefish.betterportals.selection;

import com.lauriethefish.betterportals.math.MathUtils;
import com.lauriethefish.betterportals.portal.PortalDirection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import lombok.Getter;

public class PortalSelection {
    @Getter private World world;
    private Vector selectionA;
    private Vector selectionB;

    private Vector a;
    private Vector b;

    @Getter private Location portalPosition;
    @Getter private PortalDirection portalDirection;
    @Getter private Vector portalSize;

    public PortalSelection(World world) {
        this.world = world;
    }

    // Returns true if this selection contains both A and B positions
    public boolean hasBothPoints()  {
        return selectionA != null && selectionB != null;
    }

    // Setters that find the coordinates used for creating a portal if both positions have been set
    public void setPositionA(Vector pos)  {
        selectionA = pos;
        if(selectionB != null) {calculatePortalInfo();}
    }

    public void setPositionB(Vector pos)  {
        selectionB = pos;
        if(selectionA != null) {calculatePortalInfo();}
    }

    private void calculatePortalInfo()   {
        fixCoords();
        findPortalDirection();
        // If the portal direction was null, due to the two positions not being in a portal's shape, return
        if(portalDirection == null) {return;}
        findPortalSize();
        findPortalPosition();
    }

    // Makes it so that A is the exact bottom left of the portal window, while B is the exact top right
    private void fixCoords()    {
        a = MathUtils.min(selectionA, selectionB);
        b = MathUtils.max(selectionA, selectionB);
    }

    // Finds whether the portal is facing north or east
    public void findPortalDirection()    {
        if(a.getZ() == b.getZ()) {
            portalDirection = PortalDirection.NORTH;
        }   else if(a.getX() == b.getX())    {
            portalDirection =  PortalDirection.EAST;
        }
    }

    // Finds the size of the portal window, in blocks
    public void findPortalSize() {
        portalSize = portalDirection.swapVector(b.clone().subtract(a));
        portalSize.subtract(new Vector(1.0, 1.0, 0.0)); // Subtract it by 1 so it is the size of the gateway, not the frame
    }

    // Checks if this selection is actually a valid portal
    public boolean isValid()    {
        return MathUtils.greaterThanEq(portalSize, new Vector(1.0, 1.0, 0.0)) && portalDirection != null;
    }

    // Finds the center position of this portal
    public void findPortalPosition() {
        portalPosition = a.clone().add(b).add(new Vector(1.0, 1.0, 1.0)).multiply(0.5).toLocation(world);
    }

    // Swaps the direction of this portal
    public void invertDirection()   {
        portalDirection = portalDirection.getOpposite();
    }
}
