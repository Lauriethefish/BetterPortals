package com.lauriethefish.betterportals.runnables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.BlockRaycastData;
import com.lauriethefish.betterportals.Config;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.portal.PortalPos;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Casts a ray from each player every tick
// If it passes through the portal, set the end of it to a redstone block
public class PlayerRayCast implements Runnable {
    private Config config;

    private BetterPortals pl;
    // Store a map of all of the currently active portals
    public Map<Location, PortalPos> portals;

    private BlockingQueue<PortalUpdateData> updateQueue = new LinkedBlockingQueue<>();
    public Thread renderThread;
    private class PortalUpdateData  { // Class to store data that is sent to another thread that handles the portal updates, since we cannot get this through bukkit if not on the main thread
        public PlayerData playerData;
        public PlaneIntersectionChecker checker;
        public PortalPos portal;
        public PortalUpdateData(PlayerData playerData, PlaneIntersectionChecker checker, PortalPos portal)   {
            this.playerData = playerData; this.portal = portal; this.checker = checker;
        }
    }

    public PlayerRayCast(BetterPortals pl, Map<Location, PortalPos> portals) {
        this.pl = pl;
        this.config = pl.config;
        this.portals = portals;

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
    // Will return null if not portals can be found within the portal activation distance
    private PortalPos findClosestPortal(Player player)   {
        // Loop through all active portals and find the closest one to activate
        // This is for performance - only one portal can be active at a time
        PortalPos closestPortal = null;

        // Set the current closest distance to the portalActivationDistance so that no portals
        // further than it will be activated
        double closestDistance = config.portalActivationDistance;

        Iterator<PortalPos> iter = portals.values().iterator();
        while(iter.hasNext())   {
            PortalPos portal = iter.next();
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

        // Check if the portal or it's detination has any missing blocks
        if(closestPortal != null && !closestPortal.checkOriginAndDestination())    {
            return null;
        }

        // Return the closest portal
        return closestPortal;
    }

    // Teleports the player using the given portal if the player is within the portal
    // Returns wheather the player was in the teleport area, not necessarily wheather a teleport was performed
    public boolean performPlayerTeleport(PlayerData playerData, PortalPos portal, PlaneIntersectionChecker checker)  {
        Player player = playerData.player;
        
        // If the player's position the previous tick was on the other side of the portal window, then we should teleport the player, otherwise return
        if(playerData.lastPosition == null || !checker.checkIfVisibleThroughPortal(playerData.lastPosition))   {
            return false;
        }
        
        // Save their velocity for later
        Vector playerVelocity = player.getVelocity().clone();
        // Move them to the other portal
        Location newLoc = portal.moveOriginToDestination(player.getLocation());
        newLoc.setDirection(portal.rotateToOrigin(player.getLocation().getDirection()));
 
        player.teleport(newLoc);
        
        // Set their velocity back to what it was
        player.setVelocity(playerVelocity);
        return true;
    }
    
    // This function is responsible for iterating over all of the blocks surrounding the portal,
    // and performing a raycast on each of them to check if they should be visible
    public void updatePortal(PlayerData playerData, PortalPos portal, PlaneIntersectionChecker checker) {
        // If we loaded a world last tick, we skip rendering, since the chunks might not be loaded yet
        if(playerData.loadedWorldLastTick)   {
            return;
        }
        
        // We need to update the fake entities every tick, regardless of if the player moved
        if(pl.config.enableEntitySupport)   {
            playerData.entityManipulator.updateFakeEntities();
            portal.updateNearbyEntities();

            Set<Entity> replicatedEntities = new HashSet<>();
            Vector locationOffset = portal.portalPosition.toVector().subtract(portal.destinationPosition.toVector());
            for(Entity entity : portal.nearbyEntitiesDestination)   {
                // If the entity is in a different world, or is on the same line as the portal destination, skip it
                if(entity.getWorld() != portal.destinationPosition.getWorld() || portal.positionInlineWithDestination(entity.getLocation())) {
                    continue;
                }

                // If an entity is visible through the portal, then we replicate it
                if(checker.checkIfVisibleThroughPortal(entity.getLocation().toVector().add(locationOffset)))  {
                    replicatedEntities.add(entity);
                }
            }

            Set<Entity> hiddenEntities = new HashSet<>();
            for(Entity entity : portal.nearbyEntitiesOrigin)   {
                // If the entity isn't in the same world, we skip it
                // We also skip entities directly in line with the portal window, since they generally get hidden and reshown glitchily
                if(entity.getWorld() != portal.portalPosition.getWorld() || portal.positionInlineWithOrigin(entity.getLocation())) {
                    continue;
                }

                // If an entity is visible through the portal, then we hide it
                if(checker.checkIfVisibleThroughPortal(entity.getLocation().toVector()))  {
                    hiddenEntities.add(entity);
                }
            }

            playerData.entityManipulator.swapHiddenEntities(hiddenEntities);
            playerData.entityManipulator.swapReplicatedEntities(replicatedEntities, portal);
        }

        // Optimisation: Check if the player has moved before re-rendering the view
        Vector currentLoc = playerData.player.getLocation().toVector();
        if(currentLoc.equals(playerData.lastPosition))  {return;}

        updateQueue.add(new PortalUpdateData(playerData, checker, portal));
    }

    private void handleUpdate(PortalUpdateData data)    {
        ArrayList<BlockRaycastData> currentBlocks = data.portal.currentBlocks; // Store the current blocks incase they change while being processed
        MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(data.playerData.player);
        for(BlockRaycastData raycastData : currentBlocks)    {                    
            // Check if the block is visible
            boolean visible = data.checker.checkIfVisibleThroughPortal(raycastData.originVec);

            Object oldState = data.playerData.surroundingPortalBlockStates.get(raycastData.originVec); // Find if it was visible last tick
            Object newState = visible ? raycastData.destData : raycastData.originData;

            // If we are overwriting the block, change it in the player's block array and send them a block update
            if(!newState.equals(oldState)) {
                data.playerData.surroundingPortalBlockStates.put(raycastData.originVec, newState);
                changeManager.addChange(raycastData.originVec, newState);
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

            // If we changed worlds in the last tick, we wait to avoid chunks not being loaded while sending updates
            if(playerData.loadedWorldLastTick)  {
                playerData.loadedWorldLastTick = false;
                continue;
            }

            // Find the closest portal to the player
            PortalPos portal = findClosestPortal(player);

            // If the portal that is currently active is different to the one that was active before,
            // We reset the surrounding blocks from the previous portal so that the player does not see blocks
            // where they shouldn't be
            if(playerData.lastActivePortal != portal)    {
                playerData.resetSurroundingBlockStates();

                if(pl.config.hidePortalBlocks)  {
                    // If we're not in the same world as our last portal, there's no point recreating the portal blocks
                    if(playerData.lastActivePortal != null && playerData.lastActivePortal.portalPosition.getWorld() == player.getWorld())   {
                        playerData.lastActivePortal.recreatePortalBlocks(player);
                    }
                    if(portal != null)  {
                        portal.removePortalBlocks(player);
                    }
                }

                // Do not send the packets to destroy and recreate entities if we loaded into a world last tick
                playerData.entityManipulator.resetAll(!playerData.loadedWorldLastTick);
                playerData.lastActivePortal = portal;
                playerData.lastPosition = null;
            }

            // If no portals were found, don't update anything
            if(portal == null) {continue;}

            // Create the portal's block state array if necessary
            portal.findCurrentBlocks();

            PlaneIntersectionChecker intersectionChecker = new PlaneIntersectionChecker(
                    playerData.player, portal);
            // Queue the update to happen on another thread
            updatePortal(playerData, portal, intersectionChecker);

            // Teleport the player if they cross through a portal
            if(performPlayerTeleport(playerData, portal, intersectionChecker))    {
                playerData.loadedWorldLastTick = true;
                continue;
            }

            playerData.lastPosition = player.getLocation().toVector();
        }
    }
}