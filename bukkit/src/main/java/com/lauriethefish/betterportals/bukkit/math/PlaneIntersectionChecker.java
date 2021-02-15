package com.lauriethefish.betterportals.bukkit.math;

import org.bukkit.util.Vector;

/**
 * Handles checking if a ray intersects a specific plane
 * Used for portal view checking, since the plane can be the portal's view plane
 */
public class PlaneIntersectionChecker {
    private final Vector planeCenter;
    private final Vector planeNormal;
    private final Vector maxDev;
    private final Vector rayOrigin;

    /**
     * Creates a new {@link PlaneIntersectionChecker} with the specified options.
     * @param planeCenter The center position of the plane.
     * @param planeNormal The direction of the plane, this should be normalised.
     * @param rayOrigin Where the rays that this instance checks start
     * @param maxDev Represents the size of the plane. This can be treated like a radius
     */
    public PlaneIntersectionChecker(Vector planeCenter, Vector planeNormal, Vector rayOrigin, Vector maxDev)   {
        this.planeCenter = planeCenter;
        this.planeNormal = planeNormal;
        this.rayOrigin = rayOrigin;
        this.maxDev = maxDev;
    }

    /**
     * Finds if the line from <code>pos</code> to {@link PlaneIntersectionChecker#rayOrigin} intersects the plane.
     * @param pos The destination of the ray
     * @return Whether the ray intersects
     */
    public boolean checkIfIntersects(Vector pos)    {
        // Find the direction to this position from the player's location
        Vector direction = pos.clone().subtract(rayOrigin).normalize();

        // Find if we intersect the plane, and where
        double denominator = planeNormal.dot(direction);
        if(Math.abs(denominator) > MathUtil.EPSILON) {
            Vector difference = planeCenter.clone().subtract(rayOrigin);
            double t = difference.dot(planeNormal) / denominator;
            // If the block was before the portal, return false
            if(rayOrigin.distance(pos) < t)    {
                return false;
            }

            if(t > MathUtil.EPSILON) {
                Vector portalIntersectPoint = rayOrigin.clone().add(direction.multiply(t));
                Vector distCenter = portalIntersectPoint.subtract(planeCenter);

                // Return true if the intersection point was close enough to the portal window
                return Math.abs(distCenter.getX()) <= maxDev.getX() && Math.abs(distCenter.getY()) <= maxDev.getY() && Math.abs(distCenter.getZ()) <= Math.abs(maxDev.getZ());
            }
        }

        return false;
    }
}