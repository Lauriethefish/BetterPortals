package com.lauriethefish.betterportals.bukkit.math;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Some useful functions for manipulating vectors
 */
public class MathUtil {
    public static final double EPSILON = 0.0001;

    /**
     * Rounds each coordinate of <code>vec</code>.
     * @param vec The vector to be rounded
     * @return A new vector with each coordinate rounded
     */
    public static Vector round(Vector vec)  {
        return new Vector(Math.round(vec.getX()), Math.round(vec.getY()), Math.round(vec.getZ()));
    }

    /**
     * Finds the absolute of each coordinate of <code>vec</code>.
     * @param vec The vector to find the absolute of
     * @return A new vector with the absolute of each coordinate
     */
    public static Vector abs(Vector vec)    {
        return new Vector(Math.abs(vec.getX()), Math.abs(vec.getY()), Math.abs(vec.getZ()));
    }

    /**
     * Finds the floor of each component of <code>vec</code>.
     * @param vec The vector to be floored
     * @return A new vector with each coordinate floored
     */
    public static Vector floor(Vector vec)  {
        return new Vector(Math.floor(vec.getX()), Math.floor(vec.getY()), Math.floor(vec.getZ()));
    }

    /**
     * Floors the <i>coordinates</i> of <code>loc</code>.
     * @param loc The location to be floored
     * @return A new location with each coordinate floored - note: this does not preserve the direction
     */
    public static Location floor(Location loc) {
        return floor(loc.toVector()).toLocation(loc.getWorld());
    }

    /**
     * Finds the ceiling of the spacial components of <code>vec</code>.
     * @param vec The vector to find the ceiling of
     * @return A new vector with the ceiling of each coordinate
     */
    public static Vector ceil(Vector vec)  {
        return new Vector(Math.ceil(vec.getX()), Math.ceil(vec.getY()), Math.ceil(vec.getZ()));
    }

    /**
     * Finds if every component of <code>a</code> is greater than or equal to <code>b</code>.
     * @param a Vector A
     * @param b Vector B
     * @return Whether every component is greater than or equal
     */
    public static boolean greaterThanEq(Vector a, Vector b)   {
        return a.getX() >= b.getX() && a.getY() >= b.getY() && a.getZ() >= b.getZ();
    }

    /**
     * Finds if every component of <code>a</code> is less than or equal to <code>b</code>.
     * @param a Vector A
     * @param b Vector B
     * @return Whether every component is less than or equal
     */
    public static boolean lessThanEq(Vector a, Vector b)   {
        return a.getX() <= b.getX() && a.getY() <= b.getY() && a.getZ() <= b.getZ();
    }

    /**
     * Moves every component of <code>vec</code> to the centre of the nearest block.
     * This means that each coordinate will have .5 at the end.
     * For example, if you pass <code>(4.3, 4.5, 2.3)</code> this will return <code>(4.5, 4.5, 2.5)</code>
     * @param vec The vector to be moved to the center of the block
     * @return A new vector at the center of the block
     */
    public static Vector moveToCenterOfBlock(Vector vec)  {
        return new Vector(Math.floor(vec.getX()) + 0.5, Math.floor(vec.getY()) + 0.5, Math.floor(vec.getZ()) + 0.5);
    }

    /**
     * Moves every component of <code>loc</code> to the centre of the nearest block.
     * This means that each coordinate will have .5 at the end.
     * For example, if you pass <code>(4.3, 4.5, 2.3)</code> this will return <code>(4.5, 4.5, 2.5)</code>
     * @param loc The location to be moved to the center of the block
     * @return A new location at the center of the block
     */
    public static Location moveToCenterOfBlock(Location loc)  {
        return new Location(loc.getWorld(), Math.floor(loc.getX()) + 0.5, Math.floor(loc.getY()) + 0.5, Math.floor(loc.getZ()) + 0.5);
    }

    /**
     * Finds the minimum for each coordinate of <code>a</code> and <code>b</code>.
     * @param a Vector A
     * @param b Vector B
     * @return A new vector with the minimum coordinate of <code>a</code> and <code>b</code>.
     */
    public static Vector min(Vector a, Vector b)    {
        return new Vector(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    /**
     * Finds the minimum for each coordinate of <code>a</code> and <code>b</code>.
     * @param a Location A
     * @param b Location B
     * @return A new location with the minimum coordinates of <code>a</code> and <code>b</code>.
     */
    public static Location min(Location a, Location b) {
        return min(a.toVector(), b.toVector()).toLocation(a.getWorld());
    }

    /**
     * Finds the maximum for each coordinate of <code>a</code> and <code>b</code>.
     * @param a Vector A
     * @param b Vector B
     * @return A new vector with the maximum coordinate of <code>a</code> and <code>b</code>.
     */
    public static Vector max(Vector a, Vector b)    {
        return new Vector(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    /**
     * Finds the maximum for each coordinate of <code>a</code> and <code>b</code>.
     * @param a Location A
     * @param b Location B
     * @return A new location with the maximum coordinates of <code>a</code> and <code>b</code>.
     */
    public static Location max(Location a, Location b) {
        return max(a.toVector(), b.toVector()).toLocation(a.getWorld());
    }
}