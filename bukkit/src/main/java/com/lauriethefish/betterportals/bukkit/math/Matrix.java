package com.lauriethefish.betterportals.bukkit.math;

import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.api.PortalDirection;
import org.bukkit.util.Vector;

/**
 * Represents a 4x4 matrix, used for efficiently representing portal transformations.
 */
public class Matrix {
    public double[][] m;

    public Matrix(double[][] matrix)  {
        this.m = matrix;
    }

    /**
     * Constructs a zeroed matrix - one with each value set to 0.
     */
    private Matrix() {
        this.m = new double[][]{
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        };
    }

    /**
     * Creates a translation matrix that will move coordinates by <code>in</code>.
     * @param offset The offset to be moved
     * @return The translation matrix
     */
    public static Matrix makeTranslation(Vector offset)  {
        return new Matrix(new double[][]{
                {1, 0, 0, offset.getX()},
                {0, 1, 0, offset.getY()},
                {0, 0, 1, offset.getZ()},
                {0, 0, 0, 1}
        });
    }

    /**
     * Creates an identity matrix - one that does not transform a vector at all.
     * @return The identity matrix
     */
    public static Matrix makeIdentity() {
        return new Matrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });
    }

    /**
     * Makes a rotation matrix for a portal that will rotate vectors from <code>from</code> to <code>to</code>.
     * @param from The origin of the rotation
     * @param to The destination of the rotation
     * @return The rotation matrix
     */
    public static Matrix makeRotation(PortalDirection from, PortalDirection to) {
        Vector fromVec = from.toVector();
        Vector toVec = to.toVector();
        // If the two vectors are exactly in opposite directions, use the inversion rotation axis
        if(fromVec.equals(toVec.clone().multiply(-1.0))) {
            return makeRotation(from.getInversionRotationAxis(), Math.PI);
        }

        return makeRotation(fromVec, toVec);
    }

    /**
     * Creates a rotation matrix from <code>from</code> to <code>to</code>.
     * NOTE: this might not always return what you want due to axis locking issues.
     * It's usually best to create an axis-angle matrix manually.
     * @param from The origin of the rotation
     * @param to The destination of the rotation
     * @return The rotation matrix
     */
    public static Matrix makeRotation(Vector from, Vector to) {
        double angle = from.angle(to);
        Vector axis = from.getCrossProduct(to);

        return makeRotation(axis, angle);
    }

    /**
     * Multiplies this matrix with <code>other</code>.
     * This has the effect of combining the transformations of the matrices into one.
     * NOTE: Order matters with matrix multiplication
     * @param other The matrix to multiply by
     * @return A new matrix
     */
    public Matrix multiply(Matrix other)    {
        Matrix result = new Matrix();
        for(int x = 0; x < 4; x++)  {
            for(int y = 0; y < 4; y++)  {
                result.m[y][x] = m[y][0] * other.m[0][x] + m[y][1] * other.m[1][x] + m[y][2] * other.m[2][x] + m[y][3] * other.m[3][x];
            }
        }

        return result;
    }

    /**
     * Creates a rotation matrix around axis with the specified angle.
     * This matrix will never change the magnitude of a vector
     * @param axis The axis to rotate around. This can be used to specify yaw, pitch or roll.
     * @param angle The angle to rotate in radians.
     * @return The rotation matrix
     */
    public static Matrix makeRotation(Vector axis, double angle)    {
        double uX = axis.getX();
        double uY = axis.getY();
        double uZ = axis.getZ();
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);

        return new Matrix(new double[][]{
                {cosAngle + uX * uX * (1 - cosAngle), uX * uY * (1 - cosAngle) - uZ * sinAngle, uX * uZ * (1 - cosAngle) + uY * sinAngle, 0},
                {uY * uX * (1 - cosAngle) + uZ * sinAngle, cosAngle + uY * uY * (1 - cosAngle), uY * uZ * (1 - cosAngle) - uX * sinAngle, 0},
                {uZ * uX * (1 - cosAngle) - uY * sinAngle, uZ * uY * (1 - cosAngle) + uX * sinAngle, cosAngle + uZ * uZ * (1 - cosAngle), 0},
                {0, 0, 0, 1}
        });
    }

    /**
     * Transforms <code>in</code> by this matrix.
     * @param in The vector to be transformed
     * @return A new vector, transformed by this matrix.
     */
    public Vector transform(Vector in)   {
        double[] result = new double[4];
        for(int i = 0; i < 4; i++)  {
            result[i] = in.getX() * m[i][0] + in.getY() * m[i][1] + in.getZ() * m[i][2] + m[i][3];
        }

        return new Vector(
                result[0] / result[3],
                result[1] / result[3],
                result[2] / result[3]
        );
    }

    /**
     * Transforms <code>in</code> by this matrix.
     * This will avoid floating point precision errors by moving the vector to the center of the block before transformation.
     * @param in Vector to transform
     * @return Transformed vector
     */
    public IntVector transform(IntVector in) {
        float[] result = new float[4];
        for(int i = 0; i < 4; i++) {
            result[i] = (float) ((in.getX() + 0.5) * m[i][0] + (in.getY() + 0.5) * m[i][1] + (in.getZ() + 0.5) * m[i][2] + m[i][3]);
        }

        return new IntVector(
            (int) Math.floor(result[0] / result[3]),
            (int) Math.floor(result[1] / result[3]),
            (int) Math.floor(result[2] / result[3])
        );
    }
}
