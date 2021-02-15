import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.PortalFactory;
import com.lauriethefish.betterportals.bukkit.portal.PortalActivityManager;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import implementations.TestLoggerModule;
import implementations.TestPortal;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PortalActivityManagerTests {
    private PortalActivityManager portalActivityManager;
    private PortalFactory portalFactory;

    @Before
    public void setup() {
        Injector injector = Guice.createInjector(
                new FactoryModuleBuilder().implement(IPortal.class, TestPortal.class).build(PortalFactory.class),
                new TestLoggerModule()
        );

        portalFactory = injector.getInstance(PortalFactory.class);
        portalActivityManager = injector.getInstance(PortalActivityManager.class);
    }

    private TestPortal createTestPortal() {
        PortalPosition portalOrigin = new PortalPosition(new Location(null, 0, 0, 0), PortalDirection.EAST);
        PortalPosition portalDestination = new PortalPosition(new Location(null, 0, 0, 0), PortalDirection.EAST);
        return (TestPortal) portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);
    }

    // Tests that the portal update manager calls the regular update methods of portals correctly
    @Test
    public void testUpdates() {
        TestPortal portal = createTestPortal();


        // Multiple calls to onPortalActivatedThisTick should only call update on the portal once
        portalActivityManager.onPortalActivatedThisTick(portal);
        assertEquals(1, portal.getUpdateCallCount());
        assertEquals(1, portal.getActivateCallCount());
        portalActivityManager.onPortalActivatedThisTick(portal);
        assertEquals(1, portal.getUpdateCallCount());

        portalActivityManager.postUpdate();
        // Activate should only be called once until the portal is deactivated
        portalActivityManager.onPortalActivatedThisTick(portal);
        assertEquals(1, portal.getActivateCallCount());

        portalActivityManager.postUpdate();
        // Not active this tick
        portalActivityManager.postUpdate();
        // Deactivate should be called once after the portal doesn't get activated in a tick
        assertEquals(1, portal.getDeactivateCallCount());
        portalActivityManager.postUpdate();
        assertEquals(1, portal.getDeactivateCallCount());
    }

    // Tests that the portal update manager calls the view update methods of portals correctly
    @Test
    public void testViewUpdates() {
        TestPortal portal = createTestPortal();


        // Multiple calls to onPortalViewedThisTick should only call update on the portal once
        portalActivityManager.onPortalViewedThisTick(portal);
        assertEquals(1, portal.getViewUpdateCallCount());
        assertEquals(1, portal.getViewActivateCallCount());
        portalActivityManager.onPortalViewedThisTick(portal);
        assertEquals(1, portal.getViewUpdateCallCount());

        portalActivityManager.postUpdate();
        // Activate should only be called once until the portal is deactivated
        portalActivityManager.onPortalViewedThisTick(portal);
        assertEquals(1, portal.getViewActivateCallCount());

        portalActivityManager.postUpdate();
        // Not viewed this tick
        portalActivityManager.postUpdate();
        // Deactivate should be called once after the portal doesn't get viewed in a tick
        assertEquals(1, portal.getViewDeactivateCallCount());
        portalActivityManager.postUpdate();
        assertEquals(1, portal.getViewDeactivateCallCount());
    }
}
