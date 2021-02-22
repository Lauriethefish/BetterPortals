package com.lauriethefish.betterportals.bukkit.block.rotation;

import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import org.bukkit.block.data.Rail;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Rails use another, entirely separate method of rotation for some reason. (but only in modern versions)
 * This handles rotating that easily.
 * It's a bit of a mess to do it properly by a matrix unfortunately.
 */
public class RailUtil {
    private static final Map<Rail.Shape, RailShapeSet> shapeToRotationSet = new HashMap<>();

    private static class RailShapeSet {
        Map<Vector, Rail.Shape> directionToShape;
        Map<Rail.Shape, Vector> shapeToDirection = new HashMap<>();

        public RailShapeSet(Map<Vector, Rail.Shape> directionToShape) {
            this.directionToShape = directionToShape;
            directionToShape.forEach((direction, shape) -> {
                shapeToDirection.put(shape, direction);
                shapeToRotationSet.put(shape, this);
            });

        }
    }

    static {
        Map<Vector, Rail.Shape> straightSet = new HashMap<>();
        straightSet.put(new Vector(1.0, 0.0, 0.0), Rail.Shape.EAST_WEST);
        straightSet.put(new Vector(-1.0, 0.0, 0.0), Rail.Shape.EAST_WEST);
        straightSet.put(new Vector(0.0, 0.0, 1.0), Rail.Shape.NORTH_SOUTH);
        straightSet.put(new Vector(0.0, 0.0, -1.0), Rail.Shape.NORTH_SOUTH);
        new RailShapeSet(straightSet);

        Map<Vector, Rail.Shape> curvedSet = new HashMap<>();
        curvedSet.put(new Vector(0.0, 0.0, -1.0), Rail.Shape.NORTH_EAST);
        curvedSet.put(new Vector(-1.0, 0.0, 0.0), Rail.Shape.NORTH_WEST);
        curvedSet.put(new Vector(0.0, 0.0, 1.0), Rail.Shape.SOUTH_WEST);
        curvedSet.put(new Vector(1.0, 0.0, 0.0), Rail.Shape.SOUTH_EAST);
        new RailShapeSet(curvedSet);

        Map<Vector, Rail.Shape> ascendingSet = new HashMap<>();
        ascendingSet.put(new Vector(1.0, 0.0, 0.0), Rail.Shape.ASCENDING_EAST);
        ascendingSet.put(new Vector(-1.0, 0.0, 0.0), Rail.Shape.ASCENDING_WEST);
        ascendingSet.put(new Vector(0.0, 0.0, 1.0), Rail.Shape.ASCENDING_SOUTH);
        ascendingSet.put(new Vector(0.0, 0.0, -1.0), Rail.Shape.ASCENDING_NORTH);
        new RailShapeSet(ascendingSet);
    }

    /**
     * Rotates the rail shape <code>shape</code> by <code>matrix</code>.
     * @param shape The shape to rotate
     * @param matrix The matrix to rotate by
     * @return The rotated shape, or null if there is none
     */
    @Nullable
    public static Rail.Shape rotateRailShape(Rail.Shape shape, Matrix matrix) {
        RailShapeSet set = shapeToRotationSet.get(shape);

        Vector direction = set.shapeToDirection.get(shape);
        Vector rotatedDir = MathUtil.round(matrix.transform(direction));

        Rail.Shape rotated = set.directionToShape.get(rotatedDir);
        return rotated == null ? shape : rotated;
    }
}
