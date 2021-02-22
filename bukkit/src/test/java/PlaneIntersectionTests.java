import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import org.bukkit.util.Vector;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlaneIntersectionTests {
    private static final Vector MAX_DEV = new Vector(1.5, 2.5, 0.5);

    @Test
    public void testVisible()    {
        // This plane will point directly upwards
        PlaneIntersectionChecker checker = new PlaneIntersectionChecker(
                new Vector(0.0, 0.0, 0.0), new Vector(1.0, 0.0, 0.0), new Vector(10.0, 0.0, 0.0), MAX_DEV);

        Vector blockPos = new Vector(-5.0, 0.0, 0.0);
        assertTrue(checker.checkIfIntersects(blockPos), "Block returned as not visible when it was");
    }

    @Test
    public void testNotVisible()    {
        // This plane will point directly upwards
        PlaneIntersectionChecker checker = new PlaneIntersectionChecker(
                new Vector(0.0, 0.0, 0.0), new Vector(1.0, 0.0, 0.0), new Vector(10.0, 0.0, -10.0), MAX_DEV);

        Vector blockPos = new Vector(-5.0, 0.0, 20.0);
        assertFalse(checker.checkIfIntersects(blockPos), "Block returned as visible when it wasn't");
    }
}