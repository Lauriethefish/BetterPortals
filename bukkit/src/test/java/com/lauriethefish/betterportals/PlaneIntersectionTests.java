package com.lauriethefish.betterportals;

import static org.junit.jupiter.api.Assertions.*;

import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

public class PlaneIntersectionTests {
    @Test
    void simpleTestVisible()    {
        // This plane will point directly upwards
        PlaneIntersectionChecker checker = new PlaneIntersectionChecker(
                        new Vector(0.0, 0.0, 0.0), new Vector(1.0, 0.0, 0.0), new Vector(10.0, 0.0, 0.0));

        Vector blockPos = new Vector(-5.0, 0.0, 0.0);
        assertTrue(checker.checkIfVisibleThroughPortal(blockPos), "Block returned as not visible when it was");
    }

    @Test
    void simpleTestNotVisible()    {
        // This plane will point directly upwards
        PlaneIntersectionChecker checker = new PlaneIntersectionChecker(
                        new Vector(0.0, 0.0, 0.0), new Vector(1.0, 0.0, 0.0), new Vector(10.0, 0.0, -10.0));

        Vector blockPos = new Vector(-5.0, 0.0, 20.0);
        assertFalse(checker.checkIfVisibleThroughPortal(blockPos), "Block returned as visible when it wasn't");
    }
}
