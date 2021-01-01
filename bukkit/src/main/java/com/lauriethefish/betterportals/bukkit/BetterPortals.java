package com.lauriethefish.betterportals.bukkit;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import com.lauriethefish.betterportals.bukkit.chunkloading.ChunkLoader;
import com.lauriethefish.betterportals.bukkit.commands.MainCommand;
import com.lauriethefish.betterportals.bukkit.config.Config;
import com.lauriethefish.betterportals.bukkit.events.ChunkUnload;
import com.lauriethefish.betterportals.bukkit.events.EntityPortal;
import com.lauriethefish.betterportals.bukkit.events.EntityReplicationEvents;
import com.lauriethefish.betterportals.bukkit.events.JoinLeave;
import com.lauriethefish.betterportals.bukkit.events.PlayerTeleport;
import com.lauriethefish.betterportals.bukkit.events.PortalCreate;
import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;
import com.lauriethefish.betterportals.bukkit.network.PortalClient;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.portal.PortalSpawnSystem;
import com.lauriethefish.betterportals.bukkit.portal.PortalStorage;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.PortalBlockArrayManager;
import com.lauriethefish.betterportals.bukkit.runnables.MainUpdate;
import com.lauriethefish.betterportals.bukkit.selection.WandInteract;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import lombok.Getter;
import lombok.Setter;

// Main class for the plugin
public class BetterPortals extends JavaPlugin {
    // All PlayerData is stored in this map
    private Map<UUID, PlayerData> players = new HashMap<>();
    private Map<Location, Portal> portals = new HashMap<>();

    @Getter private PortalSpawnSystem portalSpawnSystem;
    @Getter private PortalBlockArrayManager blockArrayProcessor;
    @Getter private ChunkLoader chunkLoader;

    @Getter private MainUpdate portalUpdator;
    @Getter private Config loadedConfig;
    private PortalStorage storage;

    // Item given to the player to select portals
    @Getter private ItemStack portalWand;

    // Used to connect to bungeecord/velocity for cross-server portals
    @Getter private PortalClient networkClient;

    // When a player is moved to this server when going through a portal, we need to teleport them to the destination of the portal when they join
    private Map<UUID, Location> teleportOnJoin = new HashMap<>();

    // Gives me cool info about the plugin on bstats
    private MetricsManager metrics;

    // This method is called once when our plugin is enabled
    @Override
    public void onEnable() {
        chunkLoader = ChunkLoader.newInstance(this);
        portalSpawnSystem = new PortalSpawnSystem(this);
        
        registerSerializableTypes();

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
        blockArrayProcessor = new PortalBlockArrayManager(this);

        createPortalWand();
        registerCommands();
        registerEvents();

        // Load all of the portals in portals.yml, then start the update loop
        try {
            portals = storage.loadPortals();
            portalUpdator = new MainUpdate(this);
        }   catch(Exception e)  {
            getLogger().warning(ChatColor.RED + "Error parsing portal data file, this is likely because it is invalid yaml");
            e.printStackTrace();
            disablePlugin(); return;
        }

        // Add all the currently online players
        for(Player player : getServer().getOnlinePlayers()) {
            addPlayer(player);
        }

        metrics = new MetricsManager(this);
        connectToProxy();
    }

    // Allows Portal and PortalPosition to be serialized and deserialized by Bukkit's API
    private void registerSerializableTypes() {
        ConfigurationSerialization.registerClass(Portal.class);
        ConfigurationSerialization.registerClass(PortalPosition.class);
        Portal.setSerializationInstance(this); // Used in the deserialize constructor, since we can't pass in the plugin directly there
    }

    private void createPortalWand() {
        // Make the portal wand item
        portalWand = new ItemStack(Material.BLAZE_ROD);

        // Set the name to the one in the config file
        ItemMeta meta = portalWand.getItemMeta();
        meta.setDisplayName(loadedConfig.getMessages().getPortalWandName());

        portalWand.setItemMeta(meta);
        // Add an NBT tag to help us identify the wand later
        portalWand = ReflectUtils.addItemNBTTag(portalWand, "betterportals_wand", "true");
    }

    // Starts a new connection to the proxy if it's enabled
    public void connectToProxy() {
        if(loadedConfig.getProxy().isEnabled()) {
            networkClient = new PortalClient(this); // Initialise the bungeecord connection if it's enabled
        }
    }

    // Finds if the given item is usable as the portal wand
    public boolean isPortalWand(ItemStack item) {
        return "true".equals(ReflectUtils.getItemNbtTag(item, "betterportals_wand"));
    }

    // Adds a new portal
    public void registerPortal(Portal portal)   {
        portals.put(portal.getOriginPos().getLocation(), portal);
    }

    // Removes a portal
    public void unregisterPortal(Portal portal) {
        unregisterPortal(portal.getOriginPos());
    }

    public void unregisterPortal(PortalPosition position) {
        unregisterPortal(position.getLocation());
    }

    public void unregisterPortal(Location originPos)    {
        Portal removedPortal = portals.remove(originPos);
        logDebug("Unregistering portal at origin pos %s", originPos);
        if(removedPortal != null && removedPortal.getUpdateManager().isActivatedByPlayer()) {
            removedPortal.getUpdateManager().onDeactivate();
        }
    }

    public Collection<Portal> getPortals()  {
        return portals.values();
    }

    public Portal getPortal(PortalPosition originPos) {
        return getPortal(originPos.getLocation());
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

    public void addPlayer(Player player)  {
        players.put(player.getUniqueId(), new PlayerData(this, player));
    }

    public void removePlayer(Player player)  {
        players.remove(player.getUniqueId());
    }

    // Finds the closest portal to the given location that is closer than minimumDistance
    // If no portals exist in that area, null is returned
    public Portal findClosestPortal(Location loc, double minimumDistance, Predicate<Portal> predicate)    {
        double closestDistance = minimumDistance;
        Portal closestPortal = null;
        // Loop through each portal
        for(Portal portal : getPortals())   {
            Location portalLoc = portal.getOriginPos().getLocation();
            // Only check portals in the correct world
            if(portalLoc.getWorld() != loc.getWorld())  {continue;}

            // Set the portal if it is closer than the current one
            double distance = portalLoc.distance(loc);
            if(distance < closestDistance && (predicate == null || predicate.test(portal)))  { // Test against the predicate
                closestDistance = distance;
                closestPortal = portal;
            }
        }
        return closestPortal;
    }

    public Portal findClosestPortal(Location loc, double minimumDistance) {
        return findClosestPortal(loc, minimumDistance, null);
    }

    public Portal findClosestPortal(Location loc, Predicate<Portal> predicate)   {
        return findClosestPortal(loc, Double.POSITIVE_INFINITY, predicate);
    }

    public Portal findClosestPortal(Location loc) {
        return findClosestPortal(loc, null);
    }

    public void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    // This method is called when the plugin is disabled
    @Override
    public void onDisable() {
        if(loadedConfig.getProxy().isEnabled()) { // Don't shut down the server if the proxy is disabled
            networkClient.shutdown();
        }

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

    public boolean loadConfig()   {
        try {
            saveDefaultConfig(); // Make a new config file with the default settings if one does not exist
            loadedConfig = new Config(this);
            return true;
        }   catch(Exception ex) {
            getLogger().warning("Failed to load config file. This may be because it is invalid YAML");
            ex.printStackTrace();
            return false;
        }
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        // If we are catching the ChunkUnloadEvent to forceload chunks, then register it
        if(!ReflectUtils.useNewChunkLoadingImpl)    {
            pm.registerEvents(new ChunkUnload(this), this);
        }
        pm.registerEvents(new EntityReplicationEvents(this), this);
        pm.registerEvents(new JoinLeave(this), this);
        pm.registerEvents(new PortalCreate(this), this);
        pm.registerEvents(new EntityPortal(this), this);
        pm.registerEvents(new WandInteract(this), this);
        pm.registerEvents(new PlayerTeleport(this), this);
    }

    // Methods for conveniently logging debug messages
    public void logDebug(String format, Object... args) {        
        logDebug(String.format(format, args));
    }

    public void logDebug(String message) {
        if(loadedConfig.isEntitySupportEnabled())   { // Make sure debug logging is enabled first
            getLogger().info("[DEBUG] " + message);
        }
    }

    // Functions for manipulating the teleportOnJoin map
    public void setToTeleportOnJoin(UUID playerId, Location location) {
        teleportOnJoin.put(playerId, location);
    }

    // Remove the teleport pos from the map, since the player has now been teleported
    public Location getTeleportPosOnJoin(Player player) {
        return teleportOnJoin.remove(player.getUniqueId());
    }
}