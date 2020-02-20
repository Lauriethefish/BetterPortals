package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.util.Vector;

// Deals with checking wheather a block is visible through a portal
// This is used by PlayerRayCast to make the portal see through effect

public class VisibilityChecker {
    // Position of origin for checking wheather blocks are visible
    // Should be the player's eye position
    private Location position;
    // The amount that the ray moves forwards each iteration
    private double positionIncrement;
    // The maximum distance that the ray will travel before assuming
    // that it will never pass through the portal
    private double maxDistance;

    public VisibilityChecker(Location position, double positionIncrement, double maxDistance) {
        this.position = position;
        this.positionIncrement = positionIncrement;
        this.maxDistance = maxDistance;
    }

    // Checks if EVERY field of vector a is greater than vector b
    public static boolean vectorGreaterThan(Vector a, Vector b) {
        return (a.getX() >= b.getX()) && (a.getY() >= b.getY()) && (a.getZ() >= b.getZ());
    }

    // Swaps around the X and Z coordinates if the direction is north/south
    public static Vector orientVector(PortalDirection direction, Vector vec)   {
        return direction == PortalDirection.EAST_WEST ? vec : new Vector(vec.getZ(), vec.getY(), vec.getX());
    }

    // Casts ray from the origin location to the destination. Returns true if it passes through the box
    public boolean checkIfBlockVisible(Vector destination, Vector boxBL, Vector boxTR) {
        Vector currentPos = position.toVector();
        // Direction is calculated as the difference between the two positions, multiplied by the positionIncrement.
        // This is added to the ray's position each iteration.
        Vector direction = destination.clone().subtract(currentPos).normalize().multiply(positionIncrement);
        
        // Our current distance from the origin position
        double currentDistance = 0.0; 

        boolean intersectedPortal = false;
        
        // Keep iterating until we have surpassed the maximum distance
        while(currentDistance < maxDistance) {
            // Advance our ray in the right direction
            currentPos.add(direction);

            currentDistance += positionIncrement;

            // Check if the ray has hit the destination block
            if(currentPos.toBlockVector().equals(destination.toBlockVector())) {
                break;
            }

            // Check if we are intersecting the portal
            if(vectorGreaterThan(currentPos, boxBL) && vectorGreaterThan(boxTR, currentPos)) {
                intersectedPortal = true;
                break;
            }
        }
        
        // If we both intersected the portal and hit the block, return true
        return intersectedPortal;
    }
}