package com.lauriethefish.betterportals.bukkit.portal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    @Getter private UUID worldId = null; // Currently unused on cross server portals
    @Getter private String worldName = null;

    // Used to send this position to the correct server
    @Getter private String serverName = null;

    // Since looking up the world of this portal is fairly expensive, we cache the location for later
    private transient Location locationCache = null;

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
        // A location with a null world should just make the world fields in this class null, not throw NullPointerException
        if(location.getWorld() != null) { 
            worldName = location.getWorld().getName();
            worldId = location.getWorld().getUID();
        }
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
        direction = PortalDirection.valueOf((String) map.get("direction"));

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
        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }
        return world;
    }

    // Returns the actual location represented by this PortalPosition. This will return a location with a null world if the portal is external
    public Location getLocation() {
        // Find the location from getWorld if we need to
        if(locationCache == null) {
            locationCache = new Location(getWorld(), x, y, z);
        }

        return locationCache.clone();
    }

    // Returns if this location is in line with the plane of this portal position
    public boolean isInLine(Location location) {
        return direction.swapVector(getVector()).getBlockZ() ==
            direction.swapLocation(location).getBlockZ();
    }

    public Vector getVector() {
        return new Vector(x, y, z);
    }

    // Convenience function for getting the block at this location
    public Block getBlock() {
        if(isExternal()) {throw new IllegalStateException("Cannot get the block of an external position");}
        return getLocation().getBlock();
    }

    // Returns true if this PortalPosition is actually on another server
    public boolean isExternal() {
        return serverName != null;
    }

    // Returns true if the chunk at this position is currently loaded
    public boolean isChunkLoaded() {
        if(isExternal()) {throw new IllegalStateException("Cannot check if an external position is loaded");}
        return getWorld().isChunkLoaded((int) x >> 4, (int) z >> 4);    
    }


    public void unload() {
        if(isExternal()) {throw new IllegalStateException("Cannot check if an external position is loaded");}
        getWorld().unloadChunk((int) x >> 4, (int) z >> 4);    
    }

    // Saves this portal position to a config section
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        if(worldId != null) {
            map.put("worldId", worldId.toString());
        }
        map.put("worldName", worldName);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("direction", direction.toString());
        if(serverName != null) {
            map.put("serverName", serverName);
        }
        return map;
    }
    
    @Override
    public String toString() {
        return String.format("x: %.02f, y: %.02f, z: %.02f, worldName: %s", x, y, z, worldName);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {return true;}
        if(obj == null) {return false;}
        if(!(obj instanceof PortalPosition)) {return false;}
        PortalPosition other = (PortalPosition) obj;
        
        return  other.direction == direction &&
                other.x == x &&
                other.y == y &&
                other.z == z &&
                (other.worldId == worldId || other.worldId.equals(worldId)) &&
                (other.worldName == worldName || other.worldName.equals(worldName)) &&
                (other.serverName == serverName || other.serverName.equals(serverName));
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction, x, y, z, worldId, worldName, serverName);
    }
}