package com.lauriethefish.betterportals.math;

import com.lauriethefish.betterportals.PortalPos;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PlaneIntersectionChecker {
    // This value is arbitrarily small
    private static double EPSILON = 0.000001;

    private Vector planeCenter;
    private Vector planeNormal;
    private Vector rayOrigin;

    private double xDev = EPSILON;
    private double yDev = 2.0;
    private double zDev = 1.5;

    public PlaneIntersectionChecker(Player player, PortalPos portal)    {
        this.planeCenter = portal.portalPosition.toVector();
        this.planeNormal = portal.portalDirection.getDirection();

        this.rayOrigin = player.getEyeLocation().toVector();       
    }

    // Contrustor used for testing
    public PlaneIntersectionChecker(Vector planeCenter, Vector planeNormal, Vector planeOrigin)   {
        this.planeCenter = planeCenter;
        this.planeNormal = planeNormal;
        this.rayOrigin = planeOrigin;
    }

    // Checks if the given ray intersects this plane
    public boolean checkIfVisibleThroughPortal(Vector pos)    {
        // Find the direction to this position from the player's location
        Vector direction = pos.clone().subtract(rayOrigin).normalize();

        // Find if we intersect the plane, and where
        double denominator = planeNormal.dot(direction);
        if(Math.abs(denominator) > EPSILON) {
            Vector difference = planeCenter.clone().subtract(rayOrigin);
            double t = difference.dot(planeNormal) / denominator;
            // If the block was before the portal, return false
            if(rayOrigin.distance(pos) < t)    {
                return false;
            }    

            if(t > EPSILON) {
                Vector portalIntersectPoint = rayOrigin.clone().add(direction.multiply(t));
                Vector distCenter = portalIntersectPoint.subtract(planeCenter);
                // Return true if the intersection point was close enough to the portal window
                return Math.abs(distCenter.getX()) <= xDev && Math.abs(distCenter.getY()) <= yDev && Math.abs(distCenter.getZ()) <= zDev;
            }
        }

        return false;
    }
}
