package com.lauriethefish.betterportals.bukkit.block.rotation;

import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import org.bukkit.Axis;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AxisUtil {
    private static final Map<Vector, Axis> vectorToAxis = new HashMap<>();
    private static final Map<Axis, Vector> axisToVector = new HashMap<>();

    static {
        // Either direction on each axis works for converting a vector to an axis
        // Since rotating a block 180 degrees should return the same axis
        vectorToAxis.put(new Vector(1.0, 0.0, 0.0), Axis.X);
        vectorToAxis.put(new Vector(-1.0, 0.0, 0.0), Axis.X);
        vectorToAxis.put(new Vector(0.0, 1.0, 0.0), Axis.Y);
        vectorToAxis.put(new Vector(0.0, -1.0, 0.0), Axis.Y);
        vectorToAxis.put(new Vector(0.0, 0.0, 1.0), Axis.Z);
        vectorToAxis.put(new Vector(0.0, 0.0, -1.0), Axis.Z);

        // On the other way round, we just need one direction for each axis
        axisToVector.put(Axis.X, new Vector(1.0, 0.0, 0.0));
        axisToVector.put(Axis.Y, new Vector(0.0, 1.0, 0.0));
        axisToVector.put(Axis.Z, new Vector(0.0, 0.0, 1.0));
    }

    /**
     * Finds the best rotation of <code>axis</code> by <code>matrix</code>.
     * @param axis Axis to rotate
     * @param matrix Matrix to rotate by
     * @return The rotated axis, or null if there is none.
     */
    @Nullable
    public static Axis rotateAxis(Axis axis, Matrix matrix) {
        Vector oldRotation = axisToVector.get(axis);
        Vector newRotation = MathUtil.round(matrix.transform(oldRotation));
        return vectorToAxis.get(newRotation);
    }
}
