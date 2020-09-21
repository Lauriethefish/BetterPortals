package com.lauriethefish.betterportals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.lauriethefish.betterportals.commands.MainCommand;
import com.lauriethefish.betterportals.events.ChunkUnload;
import com.lauriethefish.betterportals.events.EntityReplicationEvents;
import com.lauriethefish.betterportals.events.JoinLeave;
import com.lauriethefish.betterportals.events.PlayerPortal;
import com.lauriethefish.betterportals.events.PortalCreate;
import com.lauriethefish.betterportals.multiblockchange.ChunkCoordIntPair;
import com.lauriethefish.betterportals.portal.Portal;
import com.lauriethefish.betterportals.portal.PortalSpawnSystem;
import com.lauriethefish.betterportals.portal.PortalStorage;
import com.lauriethefish.betterportals.runnables.PlayerRayCast;

import org.bstats.bukkit.Metrics;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

// Main class for the plugin
public class BetterPortals extends JavaPlugin {
    // Plugin ID for bStats
    private static final int pluginId = 8669;
    public Metrics metrics;

    // Stores players previous portal positions
    public Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();

    public PortalSpawnSystem spawningSystem = new PortalSpawnSystem(this);
    public PlayerRayCast rayCastingSystem;
    public Config config;
    private PortalStorage storage;

    // Used if on a version where you can cancel ChunkUnloadEvent
    public Set<ChunkCoordIntPair> forceLoadedChunks = new HashSet<>();

    // This method is called once when our plugin is enabled
    @Override
    public void onEnable() {
        // If any errors occur while loading the config/portal data, we return from this function
        // This essentially terminates the plugin as the runnable will not start
        
        // Load the object used for storing portals to portals.yml
        try {
            storage = new PortalStorage(this);
        }   catch(Exception e)  {
            getLogger().warning(ChatColor.RED + "Error loading portal data file, this could be due to lack of read file access");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        // Load the config
        if(!loadConfig())   {
            getServer().getPluginManager().disablePlugin(this); return; // If loading failed, disable the plugin
        }

        registerCommands();
        registerEvents();

        // Add the PlayerData for every online player in order to support /reload
        addAllPlayerData();

        // Load all of the portals in portals.yml, then start the update loop
        try {
            Map<Location, Portal> portals = storage.loadPortals();
            rayCastingSystem = new PlayerRayCast(this, portals);
        }   catch(Exception e)  {
            getLogger().warning(ChatColor.RED + "Error parsing portal data file, this is likely because it is invalid yaml");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        initialiseStatistics();
    }

    // Initiailises bStats, and adds the various custom charts
    public void initialiseStatistics()  {
        metrics = new Metrics(this, pluginId); // Initialise bStats
        metrics.addCustomChart(new Metrics.SingleLineChart("portals_active", new Callable<Integer>()  {
            @Override
            public Integer call() throws Exception  {
                return rayCastingSystem.portals.size() / 2; // Divide by 2, since each portal is 2 list items
            }
        }));
        metrics.addCustomChart(new Metrics.SimplePie("render_distance_xz", new Callable<String>() {
            @Override
            public String call() throws Exception  {
                return String.valueOf(config.maxXZ);
            }
        }));
        metrics.addCustomChart(new Metrics.SimplePie("render_distance_y", new Callable<String>() {
            @Override
            public String call() throws Exception  {
                return String.valueOf(config.maxY);
            }
        }));
        metrics.addCustomChart(new Metrics.SimplePie("entities_enabled", new Callable<String>() {
            @Override
            public String call() throws Exception   {
                if(config.enableEntitySupport)  {
                    return "Entities";
                }   else    {
                    return "No Entities";
                }
            }
        }));
    }

    // This method is called when the plugin is disabled
    @Override
    public void onDisable() {
        for(PlayerData player : players.values())   {
            player.entityManipulator.resetAll(true);
            player.resetSurroundingBlockStates(true);
        }

        // Save all of the portals to disk
        try {
            storage.savePortals(rayCastingSystem.portals);
        }   catch(Exception e)    {
            getLogger().warning(ChatColor.RED + "Error saving portal data. This could be due to lack of write file access");
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        getCommand("betterportals").setExecutor(new MainCommand(this));
    }

    // Adds the PlayerData for every player online, in order to support /reload
    private void addAllPlayerData() {
        // For each online player
        for(Player player : getServer().getOnlinePlayers()) {
            // Add a new player data with the player's UUID
            players.put(player.getUniqueId(), new PlayerData(this, player));
        }
    }

    public boolean loadConfig()   {
        try {
            saveDefaultConfig(); // Make a new config file with the default settings if one does not exist
            config = new Config(this);
            return true;
        }   catch(Exception ex) {
            getLogger().warning("Failed to load config file. This may be because it is invalid YAML");
            ex.printStackTrace();
            return false;
        }
    }

    // Registers all of the events with spigot, so that they are fired correctly
    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        // If we are catching the ChunkUnloadEvent to forceload chunks, then register it
        if(!ReflectUtils.useNewChunkLoadingImpl)    {
            pm.registerEvents(new ChunkUnload(this), this);
        }
        pm.registerEvents(new EntityReplicationEvents(this), this);
        pm.registerEvents(new JoinLeave(this), this);
        pm.registerEvents(new PortalCreate(this), this);
        pm.registerEvents(new PlayerPortal(), this);
    }
}