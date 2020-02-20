package com.lauriethefish.betterportals.events;

import java.util.List;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PortalDirection;
import com.lauriethefish.betterportals.PortalPos;
import com.lauriethefish.betterportals.VisibilityChecker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.PortalCreateEvent.CreateReason;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

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
        if(event.getReason() == CreateReason.OBC_DESTINATION)   {
            return;
        }

        // Get all of the blocks associated with the portal. This includes some of the obsidian and all of the portal blocks
        List<Block> blocks = event.getBlocks();
        // If there are not 14 blocks, then the portal is not 2x3 in size.
        // We print this out in chat and cancel the event
        // Note that this is broadcasted to the whole server, this is because the event does not have a field
        // of the player who triggered it, because portals can be made by other sources such as ghast fireballs.
        if(blocks.size() != 14)  {
            pl.getServer().broadcastMessage(ChatColor.RED + "Error creating portal: " + 
                        "Due to performance limitations BetterPortals only works with portals that are 2x3 in size");
            event.setCancelled(true);
            return;
        }

        // Wheather all of the blocks are on the same z coordinate. If this is true then the portal is
        // oriented east/west, otherwise it is north/south
        boolean allSameZ = true;
        double currentZ = Double.NaN; // Set our current z to NaN so that no values are the same as it
        // Find the portal block closest to the bottom left, this is used for positioning the portal
        Vector smallestLocation = null;

        // Loop through all of the associated blocks
        for(Block block : blocks)   {
            // If the block is obsidian, skip it as we only care about portal blocks
            if(block.getType() == Material.OBSIDIAN)  {continue;}

            // Get the position of the portal as a block vector, so that all of the coodinates are rounded down
            Vector blockLoc = block.getLocation().toVector().toBlockVector();

            // Update the bottom left block
            if(smallestLocation == null || VisibilityChecker.vectorGreaterThan(smallestLocation, blockLoc)) {
                smallestLocation = blockLoc;
            }

            // Update the currentZ and allSameZ
            if(blockLoc.getZ() != currentZ) {
                if(!Double.isNaN(currentZ))    {
                    allSameZ = false;
                    break;
                }
                currentZ = blockLoc.getZ();
            }

        }

        // Get the direction of the portal, based on wheather the blocks are on the same z coordinate
        PortalDirection direction = allSameZ ? PortalDirection.EAST_WEST : PortalDirection.NORTH_SOUTH;
        // Get the location of the bottom left of the portal blocks
        Location location = smallestLocation.toLocation(event.getWorld());

        // Subtract 1 from the x and y of the location to get the location relative to the bottom left block of obisidan
        // This changes to z and y if the portal is oriented north/south
        if(direction == PortalDirection.EAST_WEST)  {
            location.subtract(1.0, 1.0, 0.0);
        }   else    {
            location.subtract(0.0, 1.0, 1.0);
        }

        // Find a suitable location for spawning the portal
        Location spawnLocation = pl.spawningSystem.findSuitablePortalLocation(location, direction);
        
        // If no location found - due to no link existing with this world,
        // cancel the event and return
        if(spawnLocation == null)   {
            event.setCancelled(true);
            return;
        }

        // Spawn a portal in the opposite world and the right location
        pl.spawningSystem.spawnPortal(spawnLocation, direction);
        // Fill in any missing corners of the current portal with stone,
        // because lack of corners can break the illusion
        pl.spawningSystem.fixPortalCorners(location.clone(), direction);

        // Add to the portals position, as the PlayerRayCast requires coordinates to be at
        // the absolute center of the portal
        // Swap around the x and z offsets if the portal is facing a different direction
        Vector portalAddAmount = new Vector(2.0, 2.5, 0.5);
        if(direction == PortalDirection.NORTH_SOUTH) {
            portalAddAmount = new Vector(0.5, 2.5, 2.0);
        }
        location.add(portalAddAmount);
        spawnLocation.add(portalAddAmount);

        // Add the two new ends of the portal to the rayCastingSystem,
        // so that the portal effect can be active!
        pl.rayCastingSystem.portals.add(new PortalPos(
            location.clone(), direction,
            spawnLocation.clone(), direction
        ));
        pl.rayCastingSystem.portals.add(new PortalPos(
            spawnLocation, direction,
            location, direction
        ));
    }    
}