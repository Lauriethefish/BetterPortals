package com.lauriethefish.betterportals.bukkit.portal.spawning;

import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Stores all info about where we're spawning a portal, not just the position.
 */
@Getter
public class PortalSpawnPosition {
    private final Location position;
    private final Vector size;
    private final PortalDirection direction;

    /**
     * Creates a new {@link PortalSpawnPosition}.
     * @param position The location on the bottom left frame block.
     * @param size The portal's window size in blocks (e.g 2x3 for a default nether portal)
     * @param direction Direction of the portal
     */
    public PortalSpawnPosition(Location position, Vector size, PortalDirection direction) {
        this.position = position;
        this.size = size;
        this.direction = direction;
    }

    /**
     * Converts this spawn position into an actual portal position, by moving it from the bottom-left frame block to the exact center.
     * @return The converted {@link PortalPosition}.
     */
    public PortalPosition toPortalPosition() {
        Location centerPos = position.clone();

        centerPos.add(direction.swapVector(new Vector(1.0, 1.0, 0.5)));
        centerPos.add(direction.swapVector(size.clone().multiply(0.5)));

        return new PortalPosition(centerPos, direction);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d, %s, Size=%s)", position.getBlockX(), position.getBlockY(), position.getBlockZ(), direction, size);
    }
}
