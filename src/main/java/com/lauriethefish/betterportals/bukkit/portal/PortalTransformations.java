package com.lauriethefish.betterportals.bukkit.portal;

import com.lauriethefish.betterportals.bukkit.math.Matrix;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

// Class for handling moving block positions from the origin of a portal to its destination, and vice versa
public class PortalTransformations {
    private Matrix originToDestination; // Translates a vector from the origin of the portal to its destination
    private Matrix rotateToDestination; // Rotates a unit vector to the destination

    // Opposite of the above
    private Matrix destinationToOrigin;
    private Matrix rotateToOrigin;

    // Worlds used for transforming locations with the above matrices
    private World originWorld;
    private World destinationWorld;

    public PortalTransformations(PortalPosition originPos, PortalPosition destPos) {
        // Find the rotation matrices first
        rotateToDestination = Matrix.makeRotation(originPos.getDirection(), destPos.getDirection());
        rotateToOrigin = Matrix.makeRotation(destPos.getDirection(), originPos.getDirection());

        // Then find the two translation + rotation matrices
        originToDestination = Matrix.makeTranslation(destPos.getVector())
                                .multiply(rotateToDestination)
                                .multiply(Matrix.makeTranslation(originPos.getVector().multiply(-1.0)));
        
        destinationToOrigin = Matrix.makeTranslation(originPos.getVector())
                                .multiply(rotateToOrigin)
                                .multiply(Matrix.makeTranslation(destPos.getVector().multiply(-1.0)));
    }

    // Helper functions for transforming locations and vectors with the matrices

    // Change the world of the Location to the destination or the origin
    public Location moveToDestination(Location loc) {
        return originToDestination.transform(loc.toVector()).toLocation(destinationWorld);
    }

    public Location moveToOrigin(Location loc) {
        return destinationToOrigin.transform(loc.toVector()).toLocation(originWorld);
    }

    public Vector rotateToDestination(Vector vec) {
        return rotateToDestination.transform(vec);
    }

    public Vector rotateToOrigin(Vector vec) {
        return rotateToOrigin.transform(vec);
    }
}
