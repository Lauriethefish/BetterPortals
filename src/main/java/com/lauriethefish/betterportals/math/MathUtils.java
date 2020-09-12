package com.lauriethefish.betterportals.math;

import org.bukkit.util.Vector;

// Useful functions for dealing with vectors
public class MathUtils {
    public static Vector round(Vector vec)  {
        return new Vector(Math.round(vec.getX()), Math.round(vec.getY()), Math.round(vec.getZ()));
    }

    public static Vector abs(Vector vec)    {
        return new Vector(Math.abs(vec.getX()), Math.abs(vec.getY()), Math.abs(vec.getZ()));
    }

    public static boolean greaterThanEq(Vector a, Vector b)   {
        return a.getX() >= b.getX() && a.getY() >= b.getY() && a.getZ() >= b.getZ();
    }

    public static boolean lessThanEq(Vector a, Vector b)   {
        return a.getX() <= b.getX() && a.getY() <= b.getY() && a.getZ() <= b.getZ();
    }
}
