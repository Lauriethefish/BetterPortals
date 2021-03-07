import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.api.PortalDirection;
import com.lauriethefish.betterportals.api.PortalPosition;
import com.lauriethefish.betterportals.bukkit.portal.*;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import implementations.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class PortalManagerTests {
    private ServerMock server;
    private WorldMock overworld;
    private WorldMock nether;
    private PortalFactory portalFactory;
    private PortalManager portalManager;
    private final TestPortalPredicateManager predicateManager = new TestPortalPredicateManager();

    @Before
    public void setup() {
        server = MockBukkit.mock();
        overworld = server.addSimpleWorld("world");
        nether = server.addSimpleWorld("world_nether");

        Injector injector = Guice.createInjector(
                new FactoryModuleBuilder().implement(IPortal.class, TestPortal.class).build(PortalFactory.class),
                new TestLoggerModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(IPortalPredicateManager.class).toInstance(predicateManager);
                        bind(IPortalActivityManager.class).to(TestPortalActivityManager.class);
                    }
                }
        );
        TestConfigHandler.prepareConfig(injector);

        portalFactory = injector.getInstance(PortalFactory.class);
        portalManager = injector.getInstance(PortalManager.class);
    }

    @After
    public void cleanUp() {
        MockBukkit.unmock();
    }

    @Test
    public void testRemoveExists() {
        IPortal portal = createTestPortal();

        portalManager.registerPortal(portal);

        assertNotNull(portalManager.getPortalAt(new Location(overworld, 0, 0, 0)));
        assertTrue(portalManager.removePortal(portal));
        assertNull(portalManager.getPortalAt(new Location(overworld, 0, 0, 0)));
    }

    @Test
    public void testRemoveNotExists() {
        assertNull(portalManager.getPortalAt(new Location(overworld, 0, 0, 0)));
        assertFalse(portalManager.removePortal(createTestPortal()));
    }

    // Tests that findClosest works fine with predicates
    @Test
    public void testFindClosestComplex() {
        PortalPosition portalOrigin = new PortalPosition(new Location(overworld, 0, 0, 0), PortalDirection.EAST);
        PortalPosition portalDestination = new PortalPosition(new Location(nether, 0, 0, 0), PortalDirection.EAST);
        IPortal portalA = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalOrigin = new PortalPosition(new Location(overworld, 10, 0, 0), PortalDirection.EAST);
        portalDestination = new PortalPosition(new Location(nether, 80, 0, 0), PortalDirection.EAST);
        IPortal portalB = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalOrigin = new PortalPosition(new Location(overworld, 20, 0, 0), PortalDirection.EAST);
        portalDestination = new PortalPosition(new Location(nether, 160, 0, 0), PortalDirection.EAST);
        IPortal portalC = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalManager.registerPortal(portalA);
        portalManager.registerPortal(portalB);
        portalManager.registerPortal(portalC);

        // Test that the predicate skips portal A
        Predicate<IPortal> predicate = (portal) -> portal != portalA;

        // Since A has been ruled out, and C is too far, we should get B
        IPortal closestPortal = portalManager.findClosestPortal(new Location(overworld, 0, 0, 0), 15, predicate);
        assertEquals(portalB, closestPortal);
    }

    private IPortal createTestPortal() {
        PortalPosition portalOrigin = new PortalPosition(new Location(overworld, 0, 0, 0), PortalDirection.EAST);
        PortalPosition portalDestination = new PortalPosition(new Location(nether, 0, 0, 0), PortalDirection.EAST);
        return portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);
    }

    // Tests that findClosest works fine without predicates
    @Test
    public void testFindClosestSimple() {
        PortalPosition portalOrigin = new PortalPosition(new Location(overworld, 0, 0, 0), PortalDirection.EAST);
        PortalPosition portalDestination = new PortalPosition(new Location(nether, 0, 0, 0), PortalDirection.EAST);
        IPortal portalA = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalOrigin = new PortalPosition(new Location(overworld, 10, 0, 0), PortalDirection.EAST);
        portalDestination = new PortalPosition(new Location(nether, 80, 0, 0), PortalDirection.EAST);
        IPortal portalB = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalOrigin = new PortalPosition(new Location(overworld, 20, 0, 0), PortalDirection.EAST);
        portalDestination = new PortalPosition(new Location(nether, 160, 0, 0), PortalDirection.EAST);
        IPortal portalC = portalFactory.create(portalOrigin, portalDestination, new Vector(2.0, 3.0, 0.0), true, UUID.randomUUID(), null, null);

        portalManager.registerPortal(portalA);
        portalManager.registerPortal(portalB);
        portalManager.registerPortal(portalC);

        IPortal closestPortal = portalManager.findClosestPortal(new Location(overworld, 0, 0, 0));
        assertEquals(portalA, closestPortal);
    }

    @Test
    public void testFindActivatable() {
        IPortal portalA = createTestPortal();
        IPortal portalB = createTestPortal();

        predicateManager.addActivatablePortal(portalA);
        portalManager.registerPortal(portalA);
        portalManager.registerPortal(portalB);

        PlayerMock player = server.addPlayer();
        Collection<IPortal> activatable = portalManager.findActivatablePortals(player);

        assertTrue(activatable.contains(portalA));
        assertFalse(activatable.contains(portalB));
    }

    @Test
    public void testGetById() {
        IPortal portal = createTestPortal();

        portalManager.registerPortal(portal);
        assertEquals(portal, portalManager.getPortalById(portal.getId()));
        portalManager.removePortal(portal);
        assertNull(portalManager.getPortalById(portal.getId()));
    }

    @Test
    public void testRemoveById() {
        IPortal portal = createTestPortal();

        portalManager.registerPortal(portal);
        assertEquals(portal, portalManager.getPortalById(portal.getId()));
        portalManager.removePortalById(portal.getId());
        assertNull(portalManager.getPortalById(portal.getId()));
    }
}
