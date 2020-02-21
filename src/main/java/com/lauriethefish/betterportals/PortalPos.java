package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

// Stores all of the attributes required for one direction of a portal
// Two of these should be created per portal, one for the effect on each side
public class PortalPos {
    // Corners of the portals collision box
    public Vector portalBL;
    public Vector portalTR;

    // The origin position and orientation of the portal
    public Location portalPosition;
    public PortalDirection portalDirection;

    // The destination position and orientation of the portal
    public Location destinationPosition;
    public PortalDirection destinationDirection;

    // The size of the portal's gateway in blocks
    public Vector portalSize;

    // Collision box of all portals, relative to the absolute center
    // Will soon be configurable
    public static final Vector collisionBox = new Vector(0.38, 0.35, 0.25);

    // The two directions are used to make sure that the portal effect
    // is generated in the right direction

    // Constructor to generate the collision box for a given portal
    // NOTE: The portalPosition must be the EXACT center of the portal on the x, y and z
    public PortalPos(Location portalPosition, PortalDirection portalDirection, 
                    Location destinationPosition, PortalDirection destinationDirection, Vector portalSize) {
        Vector portalPosVec = portalPosition.toVector();

        this.portalPosition = portalPosition;
        this.portalDirection = portalDirection;
        this.destinationPosition = destinationPosition;
        this.destinationDirection = destinationDirection;
        this.portalSize = portalSize;
        
        // Divide the size by 2 so it is the correct amount to subtract from the center to reach each corner
        // Then orient it so that is on the z if the portal is north/south
        Vector orientedSize = VisibilityChecker.orientVector(portalDirection, portalSize.clone().multiply(0.5));

        // Get the bottom left and top right blocks by subtracting our modified size
        Vector bottomLeftBlock = portalPosVec.clone().subtract(orientedSize);
        Vector topRightBlock = portalPosVec.clone().add(orientedSize);

        // Set the portals collision box. NOTE: The east/west collision box is different to the north/south one
        portalBL = bottomLeftBlock.clone().subtract(VisibilityChecker.orientVector(portalDirection, collisionBox));
        portalTR = topRightBlock.clone().add(VisibilityChecker.orientVector(portalDirection, collisionBox));
    }

    // Transforms the vector input, which should be relative to the center of the portal,
    // to world coordinates at the destination of the portal
    public Vector applyTransformationsDestination(Vector input) {
        // If the two portals are facing different ways then the x and z coordinates have to be flipped
        // and one of them must be inverted. This is equivilient to a rotation about the origin of 90 degrees
        if(portalDirection != destinationDirection) {
            if(portalDirection == PortalDirection.EAST_WEST)    {
                input = new Vector(input.getZ() * -1.0, input.getY(), input.getX());
            }   else    {
                input = new Vector(input.getZ(), input.getY(), input.getX() * -1.0);
            }
        }

        // Add the destinationPosition to the input to transform the vector
        return input.add(destinationPosition.toVector());
    }

    // Transforms the vector input, which should be relative to the center of the portal
    // to world coordinates at the origin of the portal
    public Vector applyTransformationsOrigin(Vector input) {
        return input.add(portalPosition.toVector());
    }

    // Checks if the portal has been broken
    // This is used to remove the portal from the plugins list of active portals
    public boolean checkIfStillActive() {
        // Check if the block at the centre of the portal is a portal block
        return portalPosition.getBlock().getType() == Material.PORTAL;
    }
}