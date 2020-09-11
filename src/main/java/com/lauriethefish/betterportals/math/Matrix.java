package com.lauriethefish.betterportals.math;

import org.bukkit.util.Vector;

// Represents a 4x4 matrix, used for portal transformations
public class Matrix {
    // 2D array representing the matrix
    public double[][] m;

    public Matrix(double[][] matrix)  {
        this.m = matrix;
    }

    // Constructs a zeroed matrix
    public Matrix() {
        this.m = new double[][]{
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
        };
    }

    // Makes a translation matrix with the effect of the given vector
    public static Matrix makeTranslation(Vector in)  {
        return new Matrix(new double[][]{
            {1, 0, 0, in.getX()},
            {0, 1, 0, in.getY()},
            {0, 0, 1, in.getZ()},
            {0, 0, 0, 1}
        });
    }

    // Makes an identity matrix, this should not transform a vector
    public static Matrix makeIdentity() {
        return new Matrix(new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
        });
    }

    // Makes a rotation matrix using the given rotation vector
    public static Matrix makeRotation(Vector from, Vector to) {
        double angle = from.angle(to);
        Vector axis = from.getCrossProduct(to);
        
        return makeRotation(axis, angle);
    }

    // Multiplies this matrix with another matrix, returning a new matrix (this does not mutate)
    public Matrix multiply(Matrix other)    {
        Matrix result = new Matrix();
        for(int x = 0; x < 4; x++)  {
            for(int y = 0; y < 4; y++)  {
                result.m[y][x] = m[y][0] * other.m[0][x] + m[y][1] * other.m[1][x] + m[y][2] * other.m[2][x] + m[y][3] * other.m[3][x];
            }
        }

        return result;
    }

    // Makes a rotation matrix around the given axis with the given angle
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

    // Transforms a 3D vector by this matrix
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
}
