package com.lauriethefish.betterportals.runnables;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.BlockRaycastData;
import com.lauriethefish.betterportals.Config;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.PortalDirection;
import com.lauriethefish.betterportals.PortalPos;
import com.lauriethefish.betterportals.VisibilityChecker;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Casts a ray from each player every tick
// If it passes through the portal, set the end of it to a redstone block
public class PlayerRayCast implements Runnable {
    private Config config;

    private BetterPortals pl;
    // Store a list of all of the currently active portals
    public List<PortalPos> portals = new ArrayList<>();

    // List to store the destinations of any removed portals, these portals will be
    // removed next tick
    public List<Location> removedDestinations = new ArrayList<>();

    private BlockingQueue<PortalUpdateData> updateQueue = new LinkedBlockingQueue<>();
    public Thread renderThread;
    private class PortalUpdateData  { // Class to store data that is sent to another thread that handles the portal updates, since we cannot get this through bukkit if not on the main thread
        public PlayerData playerData;
        public VisibilityChecker checker;
        public PortalPos portal;
        public PortalUpdateData(PlayerData playerData, VisibilityChecker checker, PortalPos portal)   {
            this.playerData = playerData; this.portal = portal; this.checker = checker;
        }
    }

    public PlayerRayCast(BetterPortals pl) {
        this.pl = pl;
        this.config = pl.config;

        // Spawn a new thread to handle updating the portal
        new Thread(() -> {
            while(pl.isEnabled()) { // Make sure the thread stops when the plugin is disabled
                try {
                    handleUpdate(updateQueue.take());
                }   catch(InterruptedException ex)  {
                    ex.printStackTrace();
                }
            }
        }).start();

        // Set the task to run every tick
        pl.getServer().getScheduler().scheduleSyncRepeatingTask(pl, this, 0, 1);
    }

    public static Vector moveVectorToCenterOfBlock(Vector vec)  {
        return new Vector(Math.floor(vec.getX()) + 0.5, Math.floor(vec.getY()) + 0.5, Math.floor(vec.getZ()) + 0.5);
    }

    // Finds the closest portal to the given player,
    // this also deletes portals if they have been broken amongst other things
    // Will return null if not portals can be found within vthe portal activation distance
    private PortalPos findClosestPortal(Player player)   {
        // Loop through all active portals and find the closest one to activate
        // This is for performance - only one portal can be active at a time
        PortalPos closestPortal = null;

        // Set the current closest distance to the portalActivationDistance so that no portals
        // further than it will be activated
        double closestDistance = config.portalActivationDistance;
        // List to store the destinations of any removed portals
        List<Location> newRemovedDestionations = new ArrayList<>();

        // Iterate through the portals
        Iterator<PortalPos> iter = portals.iterator();
        while(iter.hasNext())   {
            // Advance our iterator
            PortalPos portal = iter.next();
            // Check if the portal has any missing blocks
            // If it does, remove it and add its destination to the list
            // of portals that need to be removed.
            if(!portal.checkIfStillActive())    {
                portal.portalPosition.getBlock().setType(Material.AIR);
                newRemovedDestionations.add(portal.destinationPosition);
                iter.remove();
                continue;
            }
            // Check if this portal has a destination portal that has been destroyed
            // If it has, break the portal and remove it from the list
            if(removedDestinations.contains(portal.portalPosition)) {
                portal.portalPosition.getBlock().setType(Material.AIR);
                iter.remove();
                continue;
            }
            // Check that the portal is in the right world
            if(portal.portalPosition.getWorld() != player.getWorld())  {
                continue;
            }

            // Check if the portal is closer that any portal so far
            double distance = portal.portalPosition.distance(player.getLocation());
            if(distance < closestDistance) {
                closestPortal = portal;
                closestDistance = distance;
            }
        }

        // Set the removed destinations to the new ones so that they can be removed next tick
        removedDestinations = newRemovedDestionations;

        // Return the closest portal
        return closestPortal;
    }

    // Teleports the player using the given portal if the player is within the portal
    // Returns wheather the player was in the teleport area, not necessarily wheather a teleport was performed
    public boolean performPlayerTeleport(PlayerData playerData, PortalPos portal)  {
        Player player = playerData.player;

        // Get the players position as a 3D vector
        Vector playerVec = player.getLocation().toVector();

        // Check if they are inside the portal
        if(VisibilityChecker.vectorGreaterThan(playerVec, portal.portalBL)
         && VisibilityChecker.vectorGreaterThan(portal.portalTR, playerVec)) {
            if(playerData.lastUsedPortal == null) {
                // Save their velocity for later
                Vector playerVelocity = player.getVelocity().clone();
                // Move them to the other portal
                playerVec.subtract(portal.portalPosition.toVector());
                playerVec = portal.applyTransformationsDestination(playerVec);

                // Convert the vector to a location
                Location newLoc = playerVec.toLocation(portal.destinationPosition.getWorld());
                newLoc.setDirection(player.getLocation().getDirection());

                // Rotate the player if the exit portal faces a different direction
                if(portal.portalDirection != portal.destinationDirection) {
                    if(portal.portalDirection == PortalDirection.EAST_WEST)  {
                        newLoc.setYaw(newLoc.getYaw() + 90.0f);
                    }   else    {
                        newLoc.setYaw(newLoc.getYaw() - 90.0f);
                    }
                }      
                // Teleport them to the modified vector
                player.teleport(newLoc);
                
                // Set their velocity back to what it was
                player.setVelocity(playerVelocity);
                playerData.lastUsedPortal = portal.destinationPosition;
                playerData.resetSurroundingBlockStates();
            }
            return true;
        }   else    {
            playerData.lastUsedPortal = null;
        }
        return false;
    }
    
    // This function is responsible for iterating over all of the blocks surrounding the portal,
    // and performing a raycast on each of them to check if they should be visible
    public void queueUpdate(PlayerData playerData, PortalPos portal) {
        // Optimisation: Check if the player has moved before re-rendering the view
        Vector currentLoc = playerData.player.getLocation().toVector();
        if(currentLoc.equals(playerData.lastPosition))  {return;}
        playerData.lastPosition = currentLoc;
        
        // Class to check if a block is visible through the portal
        VisibilityChecker checker = new VisibilityChecker(playerData.player.getEyeLocation(), config.rayCastIncrement, config.maxRayCastDistance);

        updateQueue.add(new PortalUpdateData(playerData, checker, portal));
    }

    private void handleUpdate(PortalUpdateData data)    {
        ArrayList<BlockRaycastData> currentBlocks = data.portal.currentBlocks; // Store the current blocks incase they change while being processed
        MultiBlockChangeManager changeManager = new MultiBlockChangeManager(data.playerData.player);
        for(int i = 0; i < currentBlocks.size(); i++)    {
            BlockRaycastData raycastData = currentBlocks.get(i);
            
            // Convert it to a location for use later
            Vector aRelativePos = raycastData.originVec;
        
            // Check if the block is visible
            boolean visible = data.checker.checkIfBlockVisible(raycastData.originVec, data.portal.portalBL, data.portal.portalTR);

            Object oldState = data.playerData.surroundingPortalBlockStates.get(raycastData.originVec); // Find if it was visible last tick
            Object newState = visible ? raycastData.destData : raycastData.originData;

            // If we are overwriting the block, change it in the player's block array and send them a block update
            if(!newState.equals(oldState)) {
                data.playerData.surroundingPortalBlockStates.put(raycastData.originVec, newState);
                changeManager.addChange(aRelativePos, newState);
            }
        }

        // Send all the block changes
        changeManager.sendChanges();
    }

    @Override
    public void run() {
        // Verify that all of the updates from the last tick have finished processing
        // Generally this shouldn't stop the thread
        while(!updateQueue.isEmpty())   {
            try {
                Thread.sleep(5);
            }   catch(InterruptedException ex)  {ex.printStackTrace();}
        }

        // Loop through every online player
        for (Player player : pl.getServer().getOnlinePlayers()) {
            PlayerData playerData = pl.players.get(player.getUniqueId());

            // Find the closest portal to the player
            PortalPos portal = findClosestPortal(player);

            // If the portal that is currently active is different to the one that was active before,
            // We reset the surrounding blocks from the previous portal so that the player does not see blocks
            // where they shouldn't be

            if(playerData.lastActivePortal != portal)    {
                playerData.resetSurroundingBlockStates();
                playerData.lastActivePortal = portal;
            }

            // If no portals were found, skip the rest of the loop
            if(portal == null) {
                continue;
            }

            // Create the player's block state array if necessary
            portal.findCurrentBlocks();

            // Teleport the player if they are inside a portal
            // If the player teleported, exit the loop as they are no longer near the target portal
            if(performPlayerTeleport(playerData, portal))    {
                continue;
            }

            // Queue the update to happen on another thread
            queueUpdate(playerData, portal);
        }
    }
}