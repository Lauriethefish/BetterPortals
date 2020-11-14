package com.lauriethefish.betterportals.bukkit.portal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;

import lombok.Getter;

public class PortalPosition implements Serializable, ConfigurationSerializable {
    private static final long serialVersionUID = 7309245176857806033L;

    @Getter private PortalDirection direction;

    // Store the coordinates as X/Y/Z so we can serialize them
    private final double x;
    private final double y;
    private final double z;

    // We store the world ID *and* the world name. How this works is that we first
    // look up the world by ID, and if it doesn't exist, look it up by the name
    private transient UUID worldId = null; // Currently unused on cross server portals
    private String worldName;

    // Used to send this position to the correct server
    @Getter private transient String serverName = null;

    // Used if this PortalPosition is on a bungeecord server
    public PortalPosition(Vector location, PortalDirection direction, String server, String worldName) {
        this.direction = direction;
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        serverName = server;
        this.worldName = worldName;
    }

    public PortalPosition(Location location, PortalDirection direction) {
        this.direction = direction;
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        worldName = location.getWorld().getName();
        worldId = location.getWorld().getUID();
    }

    // Used for loading this position from a config section
    public PortalPosition(Map<String, Object> map) {
        // Check to see if there is a worldId in the config
        Object worldIdString = map.get("worldId");
        if(worldIdString != null) {
            worldId = UUID.fromString((String) worldIdString);
        }

        worldName = (String) map.get("worldName");
        x = (double) map.get("x");
        y = (double) map.get("y");
        z = (double) map.get("z");

        // Load the server name, if there is one
        Object configServerName = map.get("serverName");
        if(configServerName != null) {
            serverName = (String) configServerName;
        }
    }

    public World getWorld() {
        // Find the world via its ID (if we have one), or its name if a world with the ID doesn't eixst
        World world = null;
        if(worldId != null) {
            world = Bukkit.getWorld(worldId);
        }
        if (world == null) {
            world = Bukkit.getWorld(worldName);
        }
        return world;
    }

    public Location getLocation() {
        return new Location(getWorld(), x, y, z);
    }

    public Vector getVector() {
        return new Vector(x, y, z);
    }

    // Convenience function for getting the block at this location
    public Block getBlock() {
        return getLocation().getBlock();
    }

    // Saves this portal position to a config section
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if(worldId != null) {
            map.put("worldId", worldId);
        }
        map.put("worldName", worldName);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        if(serverName != null) {
            map.put("serverName", serverName);
        }
        return map;
    }
}