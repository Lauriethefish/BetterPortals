package com.lauriethefish.betterportals;

import static org.junit.jupiter.api.Assertions.*;

import com.lauriethefish.betterportals.math.Matrix;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

public class MatrixTests {
    // Checks that the identity matrix keeps all coordinates of a vector the same
    @Test
    void identityTest()  {
        Matrix identity = Matrix.makeIdentity();

        Vector input = new Vector(1.0, 2.0, 3.0);
        Vector output = identity.transform(input);

        assertTrue(input.equals(output), "Identity matrix transformed vector");
    }

    // Checks that the translation matrix transformed a vector the correct amount
    @Test
    void translationTest()  {
        // These are arbitrary, and can be changed to whatever
        Vector translation = new Vector(1.0, 2.0, 3.0);
        Vector originalPos = new Vector(-5.0, 2.0, 3.0);

        // Make a matrix to transform our vector
        Matrix translationMat = Matrix.makeTranslation(translation);

        // If the result != the expected position, then fail
        Vector expected = originalPos.clone().add(translation);
        Vector actual = translationMat.transform(originalPos);
        assertEquals(expected, actual, "Translation matrix did not translate to correct position");
    }

    @Test
    void rotationTest() {
        Vector originalPos = new Vector(5.0, 3.0, -2.0);

        // This rotation matrix should rotate the vector on the X up onto the Y axis
        Matrix rotationMat = Matrix.makeRotation(new Vector(1.0, 0.0, 0.0), new Vector(0.0, 1.0, 0.0));

        Vector expected = new Vector(-3.0, 5.0, -2.0);
        Vector actual = rotationMat.transform(originalPos);
        assertEquals(expected, actual, "Rotation matrix did not rotate to correct position");
    }

    @Test
    void multiplicationTest()   {
        // Make two matrices, find their product which should be the same as applying matB before matA
        Matrix matA = Matrix.makeTranslation(new Vector(0.0, 2.0, 0.0));
        Matrix matB = Matrix.makeRotation(new Vector(5.0, 0.0, -6.0), 5.0);
        Matrix product = matA.multiply(matB);

        // Check that the product works correctly
        Vector input = new Vector();
        Vector expected = matA.transform(matB.transform(input));
        Vector actual = product.transform(input);
        assertEquals(expected, actual, "Multiplied matrices did not transform the same as transforming separately");
    }
}
