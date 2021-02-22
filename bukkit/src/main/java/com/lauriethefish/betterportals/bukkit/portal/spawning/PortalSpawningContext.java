package com.lauriethefish.betterportals.bukkit.portal.spawning;

import com.lauriethefish.betterportals.bukkit.config.WorldLink;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Stores info about where we're currently looking for a portal
 */
@Getter
public class PortalSpawningContext {
    private final WorldLink worldLink;
    private final Location preferredLocation;
    private final Vector size;

    /**
     *
     * @param worldLink Contains info about how the two worlds are connected, e.g. min/max spawn height, coordinate rescaling
     * @param preferredLocation The location in the destination world where we want to spawn the portal
     * @param size The size of the origin portal window.
     */
    public PortalSpawningContext(WorldLink worldLink, Location preferredLocation, Vector size) {
        this.worldLink = worldLink;
        this.preferredLocation = preferredLocation;
        this.size = size;
    }
}
