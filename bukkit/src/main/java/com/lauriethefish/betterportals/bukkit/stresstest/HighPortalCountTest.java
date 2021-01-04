package com.lauriethefish.betterportals.bukkit.stresstest;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Random;

public class HighPortalCountTest implements StressTest  {
    private static final int PORTAL_COUNT = 10000; // How many portals to spawn
    private static final boolean STOP_AT_COUNT = true; // Whether to stop if there are already PORTAL_COUNT portals

    // Range from 0,0,0 in the overworld where the portals will be spawned - going to the nether at those coords/8
    // This is the X/Z range - Y coordinate is always the same (SPAWN_Y)
    private static final double PORTAL_SPAWN_RANGE = 1000;
    private static final double SPAWN_Y = 64.0;

    private static final PortalDirection PORTAL_DIRECTION = PortalDirection.NORTH; // Direction of the origin and destination of all generated portals
    private static final Vector PORTAL_SIZE = new Vector(3.0, 3.0, 0.0); // Size of all generated portals

    // Create the portals at random positions
    private Random random = new Random();
    private BetterPortals pl;

    @Override
    public void runTest(BetterPortals pl) {
        this.pl = pl;

        if(STOP_AT_COUNT) {
            createPortalsLimited();
        }   else    {
            createPortalsFully();
        }
    }

    // Creates exactly PORTAL_COUNT portals, ignoring existing ones
    public void createPortalsFully() {
        pl.logDebug("Creating %d portals", PORTAL_COUNT);
        for(int i = 0; i < PORTAL_COUNT; i++) {
            createPortalAtRandomPos();
        }
    }

    // Creates portals until there are PORTAL_COUNT portals loaded
    public void createPortalsLimited() {
        pl.logDebug("Creating portals until limit reached (%d)", PORTAL_COUNT);
        while(pl.getPortals().size() < PORTAL_COUNT) {
            createPortalAtRandomPos();
        }
    }

    public void createPortalAtRandomPos() {
        // Find the random overworld and nether position
        Location overworldPos = new Location(pl.getServer().getWorld("world"), getRandomCoordinate(), SPAWN_Y, getRandomCoordinate());
        Location netherPos = overworldPos.clone().multiply(1.0 / 8.0);
        netherPos.setWorld(pl.getServer().getWorld("world_nether"));
        netherPos.setY(SPAWN_Y); // Avoid the Y being divided by 8

        overworldPos.add(0.5, 0.5, 0.5);
        netherPos.add(0.5, 0.5, 0.5);

        // Create an unknown owner, anchored portal at the random positions
        Portal portal = new Portal(pl,
                new PortalPosition(overworldPos, PORTAL_DIRECTION),
                new PortalPosition(netherPos, PORTAL_DIRECTION),
                PORTAL_SIZE, true, null
        );

        pl.registerPortal(portal); // Register it
    }

    public double getRandomCoordinate() {
        return (random.nextDouble() - 0.5) * PORTAL_SPAWN_RANGE * 2; // *2 since we need both directions
    }
}
