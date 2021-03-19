import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.block.ViewableBlockInfo;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.IMultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.player.view.block.IPlayerBlockStates;
import com.lauriethefish.betterportals.bukkit.player.view.block.PlayerBlockStates;
import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import implementations.TestLoggerModule;
import implementations.TestMultiBlockChangeManager;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerBlockStatesTests {
    private IPlayerBlockStates blockView;
    private final Vector position = new Vector(0.0, 1.0, 0.0);

    @Before
    public void setUp() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new FactoryModuleBuilder().implement(
                        IMultiBlockChangeManager.class, TestMultiBlockChangeManager.class)
                        .build(IMultiBlockChangeManager.Factory.class));

                install(new FactoryModuleBuilder().implement(
                        IPlayerBlockStates.class, PlayerBlockStates.class)
                        .build(IPlayerBlockStates.Factory.class));
                install(new TestLoggerModule());
            }
        });

        ServerMock server = MockBukkit.mock();
        PlayerMock player = server.addPlayer();

        blockView = injector.getInstance(IPlayerBlockStates.Factory.class).create(player);
    }

    @After
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setViewableTest() {
        ViewableBlockInfo blockInfo = new ViewableBlockInfo();

        assertTrue(blockView.setViewable(position, blockInfo));

        Map<Vector, ViewableBlockInfo> viewedStates = (Map<Vector, ViewableBlockInfo>) ReflectionUtil.getField(blockView, "viewedStates");
        assertEquals(viewedStates.get(position), blockInfo);

        // Setting it to viewable twice shouldn't return true multiple times
        assertFalse(blockView.setViewable(position, blockInfo));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setNotViewableTest() {
        ViewableBlockInfo blockInfo = new ViewableBlockInfo();

        // Make it viewable, and then non-viewable again to test that it is removed from the map correctly
        assertTrue(blockView.setViewable(position, blockInfo));
        assertTrue(blockView.setNonViewable(position, blockInfo));

        Map<Vector, ViewableBlockInfo> viewedStates = (Map<Vector, ViewableBlockInfo>) ReflectionUtil.getField(blockView, "viewedStates");
        assertNull(viewedStates.get(position));

        // Setting it not viewable twice shouldn't return true multiple times
        assertFalse(blockView.setNonViewable(position, blockInfo));
    }
}
