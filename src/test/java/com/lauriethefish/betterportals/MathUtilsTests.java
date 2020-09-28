package com.lauriethefish.betterportals;

import static org.junit.jupiter.api.Assertions.*;

import com.lauriethefish.betterportals.math.MathUtils;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

// Tests for the MathUtils class
public class MathUtilsTests {
    @Test
    void roundingTest() {
        Vector input = new Vector(1.5, 2.6, 1.1);
        Vector rounded = MathUtils.round(input);

        assertEquals(rounded, new Vector(2.0, 3.0, 1.0));
    }

    @Test
    void absoluteTest() {
        Vector input = new Vector(-1.5, 2.6, -1.1);
        Vector absolute = MathUtils.abs(input);

        assertEquals(absolute, new Vector(1.5, 2.6, 1.1));
    }

    @Test
    void floorTest()    {
        Vector input = new Vector(2.5, 1.5, -1.1);
        Vector floored = MathUtils.floor(input);

        assertEquals(floored, new Vector(2.0, 1.0, -2.0));
    }

    @Test
    void ceilTest() {
        Vector input = new Vector(2.5, 1.5, -1.1);
        Vector ceil = MathUtils.ceil(input);

        assertEquals(ceil, new Vector(3.0, 2.0, -1.0));
    }

    @Test
    void greaterThanEqTest()    {
        assertTrue(MathUtils.greaterThanEq(new Vector(3.1, 5.1, 2.0), new Vector(3.0, 5.0, 2.0)));
        assertFalse(MathUtils.greaterThanEq(new Vector(3.0, 5.0, 2.0), new Vector(3.1, 5.1, 2.0)));
    }

    @Test
    void lessThanEqTest()    {
        assertTrue(MathUtils.lessThanEq(new Vector(3.0, 5.0, 2.0), new Vector(3.1, 5.1, 2.0)));
        assertFalse(MathUtils.lessThanEq(new Vector(3.1, 5.1, 2.0), new Vector(3.0, 5.0, 2.0)));
    }
}
