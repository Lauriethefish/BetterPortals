import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionUtilTests {
    @Test
    public void greaterThanTest() {
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.0.4", "1.0.3"));
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.0.1", "1.0"));
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.13.0", "1.12.2"));
    }

    @Test
    public void equalToTest() {
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.13.2", "1.13.2"));
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.13.1", "1.13.1"));
        assertTrue(VersionUtil.isVersionGreaterOrEq("1.16.4", "1.16.4"));
    }

    @Test
    public void lessThanTest() {
        assertFalse(VersionUtil.isVersionGreaterOrEq("1.16.2", "1.16.3"));
        assertFalse(VersionUtil.isVersionGreaterOrEq("1.13.1", "1.13.2"));
        assertFalse(VersionUtil.isVersionGreaterOrEq("1.12", "1.12.2"));
    }
}
