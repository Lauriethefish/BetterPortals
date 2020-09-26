package com.lauriethefish.betterportals.runnables;

import java.util.HashSet;
import java.util.Set;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.Config;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.multiblockchange.ChunkCoordIntPair;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Casts a ray from each player every tick
// If it passes through the portal, set the end of it to a redstone block
public class PlayerRayCast implements Runnable {
    private BetterPortals pl;

    private int currentTick = 0;
    private Config config;

    private Set<ChunkCoordIntPair> newForceLoadedChunks = new HashSet<>();

    private BlockProcessor blockRenderer;
    public PlayerRayCast(BetterPortals pl) {
        blockRenderer = new BlockProcessor(pl);
        this.pl = pl;
        this.config = pl.config;

        // Set the task to run every tick
        pl.getServer().getScheduler().scheduleSyncRepeatingTask(pl, this, 0, 1);
    }

    // Called by portals while they are active to keep chunks loaded
    public void keepChunksForceLoaded(Set<ChunkCoordIntPair> chunks)  {
        newForceLoadedChunks.addAll(chunks);
    }

    // Finds the closest portal to the given player,
    // this also deletes portals if they have been broken amongst other things
    // Will return null if not portals can be found within the portal activation distance
    private Portal findClosestPortal(Player player)   {
        // Loop through all active portals and find the closest one to activate
        // This is for performance - only one portal can be active at a time
        Portal closestPortal = null;

        // Set the current closest distance to the portalActivationDistance so that no portals
        // further than it will be activated
        double closestDistance = config.portalActivationDistance;

        for(Portal portal : pl.getPortals())    {
            if(portal.getOriginPos().getWorld() != player.getWorld())  {
                continue;
            }

            // Check if the portal is closer that any portal so far
            double distance = portal.getOriginPos().distance(player.getLocation());
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
    public boolean performPlayerTeleport(PlayerData playerData, Portal portal, PlaneIntersectionChecker checker)  {
        Player player = playerData.getPlayer();
        
        Vector lastPos = playerData.getLastPosition();
        // If the player's position the previous tick was on the other side of the portal window, then we should teleport the player, otherwise return
        if(lastPos == null || !checker.checkIfVisibleThroughPortal(lastPos))   {
            return false;
        }
        
        // Save their velocity for later
        Vector playerVelocity = player.getVelocity().clone();
        // Move them to the other portal
        Location newLoc = portal.moveOriginToDestination(player.getLocation());
        newLoc.setDirection(portal.rotateToDestination(player.getLocation().getDirection()));

        player.teleport(newLoc);
        // Set the player's last position to null, since otherwise portals that they moved through while teleporting will move them again
        playerData.setLastPosition(null);
        
        // Set their velocity back to what it was
        player.setVelocity(playerVelocity);
        return true;
    }
    
    // This function is responsible for iterating over all of the blocks surrounding the portal,
    // and performing a raycast on each of them to check if they should be visible
    public void updatePortal(PlayerData playerData, Portal portal, PlaneIntersectionChecker checker) {        
        // We need to update the fake entities every tick, regardless of if the player moved
        if(pl.config.enableEntitySupport)   {
            EntityManipulator manipulator = playerData.getEntityManipulator();
            manipulator.updateFakeEntities();

            Set<Entity> replicatedEntities = new HashSet<>();
            for(Entity entity : portal.getNearbyEntitiesDestination())   {
                // If the entity is in a different world, or is on the same line as the portal destination, skip it
                if(entity.getWorld() != portal.getDestPos().getWorld() || portal.positionInlineWithDestination(entity.getLocation())) {
                    continue;
                }

                Vector originPos = portal.moveDestinationToOrigin(entity.getLocation().toVector());
                // If an entity is visible through the portal, then we replicate it
                if(checker.checkIfVisibleThroughPortal(originPos))  {
                    replicatedEntities.add(entity);
                }
            }

            Set<Entity> hiddenEntities = new HashSet<>();
            for(Entity entity : portal.getNearbyEntitiesOrigin())   {
                // If the entity isn't in the same world, we skip it
                // We also skip entities directly in line with the portal window, since they generally get hidden and reshown glitchily
                if(entity.getWorld() != portal.getOriginPos().getWorld() || portal.positionInlineWithOrigin(entity.getLocation())) {
                    continue;
                }

                // If an entity is visible through the portal, then we hide it
                if(checker.checkIfVisibleThroughPortal(entity.getLocation().toVector()))  {
                    hiddenEntities.add(entity);
                }
            }

            manipulator.swapHiddenEntities(hiddenEntities);
            manipulator.swapReplicatedEntities(replicatedEntities, portal);
        }

        // Optimisation: Check if the player has moved before re-rendering the view
        Vector currentLoc = playerData.getPlayer().getLocation().toVector();
        if(currentLoc.equals(playerData.getLastPosition()))  {return;}
        // Queue an update to happen on the async task
        blockRenderer.queueUpdate(playerData, checker, portal);
    }

    @Override
    public void run() {
        // Loop through every online player
        for (Player player : pl.getServer().getOnlinePlayers()) {
            PlayerData playerData = pl.players.get(player.getUniqueId());

            // If we changed worlds in the last tick, we wait to avoid chunks not being loaded while sending updates
            if(playerData.getIfLoadedWorldLastTick())  {
                playerData.unsetLoadedWorldLastTick();
                continue;
            }

            // Find the closest portal to the player
            Portal portal = findClosestPortal(player);

            playerData.setPortal(portal);
            // If no portals were found, don't update anything
            if(portal == null) {continue;}

            // Create the portal's block state array if necessary
            portal.update(currentTick);

            PlaneIntersectionChecker intersectionChecker = new PlaneIntersectionChecker(player, portal);
            // Queue the update to happen on another thread
            updatePortal(playerData, portal, intersectionChecker);

            // Teleport the player if they cross through a portal
            if(performPlayerTeleport(playerData, portal, intersectionChecker))    {
                playerData.setLoadedWorldLastTick();
                continue;
            }

            playerData.setLastPosition(player.getLocation().toVector());
        }

        currentTick++;

        // If we are using the force loading method, unforceload any chunks that are no longer loaded by portals
        if(ReflectUtils.useNewChunkLoadingImpl) {
            for(ChunkCoordIntPair chunk : pl.forceLoadedChunks) {
                if(!newForceLoadedChunks.contains(chunk))   {
                    chunk.getChunk().setForceLoaded(false);
                }
            }
        }

        pl.forceLoadedChunks = newForceLoadedChunks;
        newForceLoadedChunks = new HashSet<>();
    }
}