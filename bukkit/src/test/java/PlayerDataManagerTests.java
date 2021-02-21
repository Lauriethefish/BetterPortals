import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.PlayerDataFactory;
import com.lauriethefish.betterportals.bukkit.player.PlayerDataManager;
import implementations.TestLoggerModule;
import implementations.TestPlayerData;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.After;
import org.junit.Before;

public class PlayerDataManagerTests {
    private PlayerDataManager playerDataManager;
    private ServerMock serverMock;

    @Before
    public void setup() {
        serverMock = MockBukkit.mock();
        JavaPlugin mockPlugin = MockBukkit.createMockPlugin();

        Injector injector = Guice.createInjector(new TestLoggerModule(),
                new FactoryModuleBuilder().implement(IPlayerData.class, TestPlayerData.class).build(PlayerDataFactory.class),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(JavaPlugin.class).toInstance(mockPlugin);
                    }
                }
        );

        playerDataManager = injector.getInstance(PlayerDataManager.class);
    }

    @After
    public void cleanup() {
        MockBukkit.unmock();
    }

    /*
        Disabled since MockBukkit hashes players using a bunch of factors instead of the Bukkit way of just using the player UUID
        This breaks the player data manager code, so no tests for now :(

    @Test
    public void testPlayerJoin() {
        Player player = serverMock.addPlayer();

        IPlayerData playerData = playerDataManager.getPlayerData(player);
        assertNotNull(playerDataManager.getPlayerData(player));
    }

    @Test
    public void testPlayerJoinAndLeave() {
        PlayerMock player = serverMock.addPlayer();

        IPlayerData playerData = playerDataManager.getPlayerData(player);
        assertNotNull(playerDataManager.getPlayerData(player));
        serverMock.getPluginManager().callEvent(new PlayerQuitEvent(player, null));
        assertNull(playerDataManager.getPlayerData(player));
    }*/

}
