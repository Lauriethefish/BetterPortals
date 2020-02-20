package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

// This class is currently unused, the portal system uses VisibilityChecker instead

// Handles intersecting a ray with blocks in the world
public class RayCast {
    // The maximum distance that the ray will travel before giving up and guessing that the block does not exist
    double maxDistance;
    // The amount that the ray steps forwards each iteration, lower values result in higher precision
    double increment;

    public RayCast(double maxDistance, double increment) {
        this.maxDistance = maxDistance;
        this.increment = increment;
    }

    // Checks if EVERY field of vector a is greater than vector b
    private boolean vectorGreaterThan(Vector a, Vector b) {
        return (a.getX() > b.getX()) && (a.getY() > b.getY()) && (a.getZ() > b.getZ());
    }

    // Alias for casting a ray without a box intersection
    public Location castRay(Location origin) {
        return castRayIntersectBoxOffset(origin, null, null, null);
    }

    // Alias for casting a box intersection ray with no offset
    public Location castRayIntersectBox(Location origin, Vector bLBox, Vector tRBox) {
        return castRayIntersectBoxOffset(origin, bLBox, tRBox, new Vector(0.0, 0.0, 0.0));
    }

    // Sends a ray from the specified location (contains both the position and direction)
    // If bLBox == null then it will not calculate a box intersection
    // Will return null if it found no block within the maxDistance or the ray did not intersect the box
    // NOTE: The x, y and x of bLBox must all be less than in tRBox or else it will not work
    // The offset is added to the position when the box is intersected - it acts like a portal
    public Location castRayIntersectBoxOffset(Location origin, Vector bLBox, Vector tRBox, Vector offset) {
        Vector originPosition = origin.toVector();
        // Stores the current position, gets update every iteration
        Vector currentPosition = originPosition.clone();
        // The vector that will be added to currentPosition each iteration
        Vector direction = origin.getDirection().multiply(increment);
        // Wheather we have intersected with the box yet
        // This defaults to true if we are not calculating an intersection
        boolean intersectedBox = (bLBox == null);

        // Keep iterating while we are within the maxDistance
        while(currentPosition.distance(originPosition) < maxDistance) {
            currentPosition = currentPosition.add(direction); // Change our current position by the direction

            // Check if we are calculating a box intersection
            if(bLBox != null) {
                // If we are intersecting the box
                if(vectorGreaterThan(currentPosition, bLBox) && vectorGreaterThan(tRBox, currentPosition)) {
                    if(intersectedBox == false) {
                        intersectedBox = true;
                        currentPosition = currentPosition.add(offset);
                    }
                    continue;
                }
            }

            // Otherwise convert our vector into a location
            Location currentLocation = new Location(origin.getWorld(), currentPosition.getBlockX(), currentPosition.getBlockY(), currentPosition.getBlockZ());

            // Check if it contains an opaque block
            if(currentLocation.getBlock().getType() != Material.AIR) {
                // If it does, return the currentLocation, but only if the box has been intersected
                if(intersectedBox) {
                    return currentLocation;
                }   else    {
                    return null;
                }
            }
            
        }

        // If no block was intersected with, return null
        return null;
    }
}