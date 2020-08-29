package com.lauriethefish.betterportals;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.lauriethefish.betterportals.events.PlayerJoin;
import com.lauriethefish.betterportals.events.PlayerPortal;
import com.lauriethefish.betterportals.events.PortalCreate;
import com.lauriethefish.betterportals.runnables.PlayerRayCast;

import org.bstats.bukkit.Metrics;
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
    public HashMap<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();

    public PortalSpawnSystem spawningSystem = new PortalSpawnSystem(this);
    public PlayerRayCast rayCastingSystem;
    public Config config;
    private PortalStorage storage;

    // This method is called once when our plugin is enabled
    @Override
    public void onEnable() {
        metrics = new Metrics(this, pluginId); // Initialise bStats
        metrics.addCustomChart(new Metrics.SingleLineChart("portals_active", new Callable<Integer>()  {
            @Override
            public Integer call() throws Exception  {
                return rayCastingSystem.portals.size() / 2; // Divide by 2, since each portal is 2 list items
            }
        }));

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

        // Start the PlayerRayCast task, which is run every tick
        rayCastingSystem = new PlayerRayCast(this);

        // Load all of the portals in portals.yml
        try {
            rayCastingSystem.portals = storage.loadPortals();
        }   catch(Exception e)  {
            getLogger().warning(ChatColor.RED + "Error parsing portal data file, this is likely because it is invalid yaml");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // This method is called when the plugin is disabled
    @Override
    public void onDisable() {
        // Save all of the portals to disk
        try {
            storage.savePortals(rayCastingSystem.portals);
        }   catch(Exception e)    {
            getLogger().warning(ChatColor.RED + "Error saving portal data. This could be due to lack of write file access");
            e.printStackTrace();
        }
    }

    // This function is currently empty, as we have no commands
    private void registerCommands() {

    }

    // Adds the PlayerData for every player online, in order to support /reload
    private void addAllPlayerData() {
        // For each online player
        for(Player player : getServer().getOnlinePlayers()) {
            // Add a new player data with the player's UUID
            players.put(player.getUniqueId(), new PlayerData(player));
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

        pm.registerEvents(new PortalCreate(this), this);        
        pm.registerEvents(new PlayerJoin(this), this);
        pm.registerEvents(new PlayerPortal(), this);
    }
}