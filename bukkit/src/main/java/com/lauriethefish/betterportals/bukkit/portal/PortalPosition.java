package com.lauriethefish.betterportals.bukkit.portal;

import com.lauriethefish.betterportals.bukkit.math.IntVector;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores the coordinates, world, server, and direction at one side of the portal.
 * This makes handling of cross-server portals more ergonomic.
 * <br>Some notes:
 * <li>Portal positions are at the <i>exact center</i> of the portal window. Not in the bottom left</li>
 */
public class PortalPosition implements Serializable, ConfigurationSerializable {
    private static final long serialVersionUID = 7309245176857806033L;

    @Getter private final PortalDirection direction;

    private final double x;
    private final double y;
    private final double z;

    // We store the world ID *and* the world name. How this works is that we first
    // look up the world by ID, and if it doesn't exist, look it up by the name
    @Getter private UUID worldId = null; // Currently unused on cross server portals
    @Getter private String worldName = null;

    @Getter @Setter private String serverName = null;

    // Since looking up the world of this portal is fairly expensive, we cache the location for later
    private transient Location locationCache = null;

    /**
     * Creates a new external portal position.
     * @param location Coordinates on the destination server, in the exact center of the portal window.
     * @param direction Direction on the destination server
     * @param server Name of the destination server
     * @param worldName World of the portal on the destination server
     */
    public PortalPosition(Vector location, PortalDirection direction, String server, String worldName) {
        this.direction = direction;
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        serverName = server;
        this.worldName = worldName;
    }

    /**
     * Creates a local portal position.
     * @param location Coordinates in the exact center of the portal window.
     * @param direction Direction out of the portal
     */
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

    public PortalPosition(Map<String, Object> map) {
        Object worldIdString = map.get("worldId");
        if(worldIdString != null) {
            worldId = UUID.fromString((String) worldIdString);
        }

        worldName = (String) map.get("worldName");
        x = (double) map.get("x");
        y = (double) map.get("y");
        z = (double) map.get("z");
        direction = PortalDirection.valueOf((String) map.get("direction"));

        Object configServerName = map.get("serverName");
        if(configServerName != null) {
            serverName = (String) configServerName;
        }
    }

    /**
     * @return World of this instance, null if external.
     */
   @Nullable
   public World getWorld() {
        // Find the world via its ID (if we have one), or its name if a world with the ID doesn't exist
        World world = null;
        if(worldId != null) {
            world = Bukkit.getWorld(worldId);
        }
        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }
        return world;
    }

    /**
     * @return The location represented by this instance. The Location's world will be null for cross-server portals.
     */
    @NotNull
    public Location getLocation() {
        if(locationCache == null) {
            locationCache = new Location(getWorld(), x, y, z);
        }

        return locationCache.clone();
    }

    /**
     * @return If this vector is in line <i>with the plane</i> of this portal position.
     */
    public boolean isInLine(IntVector vec) {
        return direction.swapVector(getVector()).getBlockZ() ==
                direction.swapVector(vec).getZ();
    }

    public Vector getVector() {
        return new Vector(x, y, z);
    }

    // Convenience function for getting the block at this location
    public Block getBlock() {
        if(isExternal()) {throw new IllegalStateException("Cannot get the block of an external position");}
        return getLocation().getBlock();
    }

    public boolean isExternal() {
        return serverName != null;
    }

    // Saves this portal position to a config section
    @Override
    public @NotNull Map<String, Object> serialize() {
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