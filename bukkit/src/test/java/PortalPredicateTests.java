import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.lauriethefish.betterportals.bukkit.config.ConfigManager;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.portal.predicate.ActivationDistance;
import com.lauriethefish.betterportals.bukkit.portal.predicate.ViewPermissions;
import implementations.TestConfigModule;
import implementations.TestLoggerModule;
import implementations.TestPortal;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PortalPredicateTests {
    private Injector injector;
    private PlayerMock player;
    private TestPortal portal;
    private WorldMock overworld;
    private WorldMock nether;

    @Before
    public void setup() {
        ServerMock server = MockBukkit.mock();
        overworld = server.addSimpleWorld("world");
        nether = server.addSimpleWorld("world_nether");

        injector = Guice.createInjector(
                new TestConfigModule(),
                new TestLoggerModule()
        );
        injector.getInstance(ConfigManager.class).loadValues();

        player = server.addPlayer();

        PortalPosition originPos = new PortalPosition(new Location(overworld, 0, 64, 0), PortalDirection.EAST);
        PortalPosition destPos = new PortalPosition(new Location(nether, 1000, 64, 0), PortalDirection.EAST);

        portal = new TestPortal(originPos, destPos, new Vector(3.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testViewDistance() {
        ActivationDistance predicate = injector.getInstance(ActivationDistance.class);

        // Portal activation distance is 20 blocks, make sure that only portals inside that are activated
        player.setLocation(new Location(overworld, 0, 64, 0));
        assertTrue(predicate.test(portal, player));

        player.setLocation(new Location(overworld, 21, 64, 0));
        assertFalse(predicate.test(portal, player));

        // Positions in other worlds should always return false, regardless of if the absolute position is closer
        player.setLocation(new Location(nether, 0, 64, 0));
        assertFalse(predicate.test(portal, player));
    }

    @Test
    public void testViewPermissions() {
        // This is done with operator status for now, until I can figure out how to make MockBukkit add/revoke permissions.
        ViewPermissions predicate = injector.getInstance(ViewPermissions.class);
        player.setOp(true);
        assertTrue(predicate.test(portal, player));
        player.setOp(false);
        assertFalse(predicate.test(portal, player));
    }
}
