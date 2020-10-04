package com.lauriethefish.betterportals.events;

import java.util.List;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.math.MathUtils;
import com.lauriethefish.betterportals.portal.PortalDirection;
import com.lauriethefish.betterportals.portal.PortalSpawnSystem;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.util.Vector;
// This event is called whenever a portal is created, either by a player,
// or other source
// This class deals with creating the portal in the BetterPortals format
public class PortalCreate implements Listener {
    private BetterPortals pl;
    public PortalCreate(BetterPortals pl)   {
        this.pl = pl;
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        // If the portal was created by the vanilla portal generator (due to a player manually generating one)
        // then return from this event. This should not happen, but this is here in case it does
        // Also don't check the portal if this world is disabled
        if(pl.config.isWorldDisabled(event.getWorld()) || event.getReason() != CreateReason.FIRE)   {
            return;
        }

        // Get all of the blocks associated with the portal. This includes some of the obsidian and all of the portal blocks
        List<?> blocks = (List<?>) ReflectUtils.runMethod(event, "getBlocks"); // This has no type because it will be BlockState in 1.14 and up, and Block in 1.13, we will do the conversion later

        // Find the portal block closest to the bottom left and top right, this is used for positioning the portal
        Vector largestLocation = null;
        Vector smallestLocation = null;

        // Loop through all of the associated blocks
        for(Object obj : blocks)   {
            // Needed as in versions 1.13 and under, it is a list of Blocks instead of BlockStates
            Block block = null;
            if(obj instanceof BlockState)   {
                block = ((BlockState) obj).getBlock();
            }   else    {
                block = (Block) obj;
            }

            // If the block is obsidian, skip it as we only care about portal blocks
            if(block.getType() == Material.OBSIDIAN)  {continue;}

            // Get the position of the portal as a block vector, so that all of the coodinates are rounded down
            Vector blockLoc = block.getLocation().toVector();

            // Update the bottom left block
            if(smallestLocation == null || MathUtils.greaterThanEq(smallestLocation, blockLoc)) {
                smallestLocation = blockLoc;
            }
            if(largestLocation == null || MathUtils.lessThanEq(largestLocation, blockLoc))   {
                largestLocation = blockLoc;
            }
        }

        // Get the direction of the portal, based on wheather the blocks are on the same z coordinate
        PortalDirection direction = largestLocation.getZ() == smallestLocation.getZ() ? PortalDirection.NORTH : PortalDirection.EAST;

        // Get the location of the bottom left of the portal blocks
        Location location = smallestLocation.toLocation(event.getWorld());

        // Get the size of the portal on the x and y coordinates, this requires flipping them if the portal faces north/south
        Vector portalSize = direction.swapVector(largestLocation.clone().subtract(smallestLocation))
            .add(new Vector(1.0, 1.0, 0.0));

        // Check that the portal is smaller than the max size
        Vector maxPortalSize = pl.config.maxPortalSize;
        if(portalSize.getX() > maxPortalSize.getX() || portalSize.getY() > maxPortalSize.getY())    {
            event.setCancelled(true);
            return;
        }

        // Subtract 1 from the x and y of the location to get the location relative to the bottom left block of obisidan
        // This changes to z and y if the portal is oriented north/south
        location.subtract(direction.swapVector(new Vector(1.0, 1.0, 0.0)));

        PortalSpawnSystem spawnSystem = pl.getPortalSpawnSystem();
        // Find a suitable location for spawning the portal
        Location spawnLocation = spawnSystem.findSuitablePortalLocation(location, direction, portalSize);
        
        // If no location found - due to no link existing with this world,
        // cancel the event and return
        if(spawnLocation == null)   {
            event.setCancelled(true);
            return;
        }

        // Spawn a portal in the opposite world and the right location
        spawnSystem.spawnPortal(spawnLocation, direction, portalSize);

        // Fill in any missing corners of the current portal with stone,
        // because lack of corners can break the illusion
        spawnSystem.fixPortalCorners(location.clone(), direction, portalSize);

        // Add to the portals position, as the PlayerRayCast requires coordinates to be at
        // the absolute center of the portal
        // Swap around the x and z offsets if the portal is facing a different direction
        Vector portalAddAmount = direction.swapVector(portalSize.clone().multiply(0.5).add(new Vector(1.0, 1.0, 0.5)));
        location.add(portalAddAmount);
        spawnLocation.add(portalAddAmount);

        // Add the two new ends of the portal to the rayCastingSystem,
        // so that the portal effect can be active!
        pl.registerPortal(new Portal(pl,
            location, direction,
            spawnLocation, direction, portalSize, false
        ));
        pl.registerPortal(new Portal(pl,
            spawnLocation, direction,
            location, direction, portalSize, false
        ));
    }    
}