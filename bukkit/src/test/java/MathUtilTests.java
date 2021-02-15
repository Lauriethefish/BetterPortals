import static org.junit.jupiter.api.Assertions.*;

import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.Test;

// Tests for the MathUtil class
public class MathUtilTests {
    @Test
    public void roundingTest() {
        Vector input = new Vector(1.5, 2.6, 1.1);
        Vector rounded = MathUtil.round(input);

        assertEquals(new Vector(2.0, 3.0, 1.0), rounded);
    }

    @Test
    public void absoluteTest() {
        Vector input = new Vector(-1.5, 2.6, -1.1);
        Vector absolute = MathUtil.abs(input);

        assertEquals(new Vector(1.5, 2.6, 1.1), absolute);
    }

    @Test
    public void floorTest()    {
        Vector input = new Vector(2.5, 1.5, -1.1);
        Vector floored = MathUtil.floor(input);

        assertEquals(new Vector(2.0, 1.0, -2.0), floored);
    }

    @Test
    public void ceilTest() {
        Vector input = new Vector(2.5, 1.5, -1.1);
        Vector ceil = MathUtil.ceil(input);

        assertEquals(new Vector(3.0, 2.0, -1.0), ceil);
    }

    @Test
    public void greaterThanEqTest()    {
        assertTrue(MathUtil.greaterThanEq(new Vector(3.1, 5.1, 2.0), new Vector(3.0, 5.0, 2.0)));
        assertFalse(MathUtil.greaterThanEq(new Vector(3.0, 5.0, 2.0), new Vector(3.1, 5.1, 2.0)));
    }

    @Test
    public void lessThanEqTest()    {
        assertTrue(MathUtil.lessThanEq(new Vector(3.0, 5.0, 2.0), new Vector(3.1, 5.1, 2.0)));
        assertFalse(MathUtil.lessThanEq(new Vector(3.1, 5.1, 2.0), new Vector(3.0, 5.0, 2.0)));
    }

    @Test
    public void moveToCenterOfBlockTestVec() {
        Vector input = new Vector(2.45, 5.5, -4.3);
        Vector atCenter = MathUtil.moveToCenterOfBlock(input);

        assertEquals(atCenter, new Vector(2.5, 5.5, -4.5));
    }

    @Test
    public void moveToCenterOfBlockTestWorld() {
        Location input = new Location(null, 2.45, 5.5, -4.3);
        Location atCenter = MathUtil.moveToCenterOfBlock(input);

        assertEquals(atCenter, new Location(null, 2.5, 5.5, -4.5));
    }

    @Test
    public void minTest() {
        Vector a = new Vector(0.5, 4.5, 3.5);
        Vector b = new Vector(4.5, 5.5, -4.5);
        assertEquals(MathUtil.min(a, b), new Vector(0.5, 4.5, -4.5));
    }

    @Test
    public void maxTest() {
        Vector a = new Vector(0.5, 4.5, 3.5);
        Vector b = new Vector(4.5, 5.5, -4.5);
        assertEquals(MathUtil.max(a, b), new Vector(4.5, 5.5, 3.5));
    }
}