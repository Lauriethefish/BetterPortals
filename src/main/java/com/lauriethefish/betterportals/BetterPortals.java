package com.lauriethefish.betterportals;

import java.util.Collection;
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
import com.lauriethefish.betterportals.runnables.MainUpdate;
import com.lauriethefish.betterportals.selection.WandInteract;

import org.bstats.bukkit.Metrics;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;

// Main class for the plugin
public class BetterPortals extends JavaPlugin {
    @Getter private final String chatPrefix = ChatColor.GRAY + "[" + ChatColor.GREEN + "BetterPortals" + ChatColor.GRAY + "]"
    + ChatColor.GREEN + " ";

    // Plugin ID for bStats
    private static final int pluginId = 8669;
    private Metrics metrics;

    // All PlayerData is stored in this map
    private Map<UUID, PlayerData> players = new HashMap<UUID, PlayerData>();
    private Map<Location, Portal> portals;

    @Getter private PortalSpawnSystem portalSpawnSystem = new PortalSpawnSystem(this);
    @Getter private MainUpdate portalUpdator;
    public Config config;
    private PortalStorage storage;

    // Item given to the player to select portals
    @Getter private ItemStack portalWand;

    // Used if on a version where you can cancel ChunkUnloadEvent
    @Getter @Setter private Set<ChunkCoordIntPair> forceLoadedChunks = new HashSet<>();

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
            disablePlugin(); return;
        }
        // Load the config
        if(!loadConfig())   {
            disablePlugin(); return; // If loading failed, disable the plugin
        }

        createPortalWand();
        registerCommands();
        registerEvents();

        // Add the PlayerData for every online player in order to support /reload
        addAllPlayerData();

        // Load all of the portals in portals.yml, then start the update loop
        try {
            portals = storage.loadPortals();
            portalUpdator = new MainUpdate(this);
        }   catch(Exception e)  {
            getLogger().warning(ChatColor.RED + "Error parsing portal data file, this is likely because it is invalid yaml");
            e.printStackTrace();
            disablePlugin(); return;
        }

        initialiseStatistics();
    }

    private void createPortalWand() {
        // Make the portal wand item
        portalWand = new ItemStack(Material.BLAZE_ROD);

        // Set the name
        ItemMeta meta = portalWand.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Portal Wand");
        portalWand.setItemMeta(meta);
        // Add an NBT tag to help us identify the wand later
        portalWand = ReflectUtils.addItemNBTTag(portalWand, "betterportals_wand", "true");
    }

    // Finds if the given item is usable as the portal wand
    public boolean isPortalWand(ItemStack item) {
        return "true".equals(ReflectUtils.getItemNbtTag(item, "betterportals_wand"));
    }

    // Adds a new portal
    public void registerPortal(Portal portal)   {
        portals.put(portal.getOriginPos(), portal);
    }

    // Removes a portal
    public void unregisterPortal(Portal portal) {
        unregisterPortal(portal.getOriginPos());
    }

    public void unregisterPortal(Location originPos)    {
        portals.remove(originPos);
    }

    public Collection<Portal> getPortals()  {
        return portals.values();
    }

    public Portal getPortal(Location originPos) {
        return portals.get(originPos);
    }

    public PlayerData getPlayerData(Player player)  {
        return players.get(player.getUniqueId());
    }

    public Collection<PlayerData> getPlayers()  {
        return players.values();
    }

    public boolean isChunkForceLoaded(Chunk chunk)  {
        return forceLoadedChunks.contains(new ChunkCoordIntPair(chunk));
    }

    public void addPlayer(Player player)  {
        players.put(player.getUniqueId(), new PlayerData(this, player));
    }

    public void removePlayer(Player player)  {
        players.remove(player.getUniqueId());
    }

    // Finds the closest portal to the given location that is closer than minimumDistance
    // If no portals exist in that area, null is returned
    public Portal findClosestPortal(Location loc, double minimumDistance)    {
        double closestDistance = minimumDistance;
        Portal closestPortal = null;
        // Loop through each portal
        for(Portal portal : getPortals())   {
            Location portalLoc = portal.getOriginPos();
            // Only check portals in the correct world
            if(portalLoc.getWorld() != loc.getWorld())  {continue;}

            // Set the portal if it is closer than the current one
            double distance = portalLoc.distance(loc);
            if(distance < closestDistance)  {
                closestDistance = distance;
                closestPortal = portal;
            }
        }
        return closestPortal;
    }

    public Portal findClosestPortal(Location loc)   {
        return findClosestPortal(loc, Double.POSITIVE_INFINITY);
    }

    public void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    // Initiailises bStats, and adds the various custom charts
    public void initialiseStatistics()  {
        metrics = new Metrics(this, pluginId); // Initialise bStats
        metrics.addCustomChart(new Metrics.SingleLineChart("portals_active", new Callable<Integer>()  {
            @Override
            public Integer call() throws Exception  {
                return portals.size() / 2; // Divide by 2, since each portal is 2 list items
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
            player.getEntityManipulator().resetAll(true);
            player.resetSurroundingBlockStates(true);
        }

        // Save all of the portals to disk
        try {
            storage.savePortals(portals);
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
        pm.registerEvents(new WandInteract(this), this);
    }
}