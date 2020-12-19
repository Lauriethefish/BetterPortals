package com.lauriethefish.betterportals.bukkit.runnables;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.PlayerData;
import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import com.lauriethefish.betterportals.bukkit.config.Config;
import com.lauriethefish.betterportals.bukkit.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;
import com.lauriethefish.betterportals.bukkit.portal.Portal;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Casts a ray from each player every tick
// If it passes through the portal, set the end of it to a redstone block
public class MainUpdate implements Runnable {
    private BetterPortals pl;

    private int currentTick = 0;
    private Config config;

    private Set<ChunkCoordIntPair> newForceLoadedChunks = new HashSet<>();

    private BlockProcessor blockRenderer;
    public MainUpdate(BetterPortals pl) {
        blockRenderer = new BlockProcessor(pl);
        this.pl = pl;
        this.config = pl.getLoadedConfig();

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
        Predicate<Portal> isValidPortal = new Predicate<Portal>() {
            @Override
            public boolean test(Portal portal) {
                // If the portal goes across servers, and we aren't connected to bungeecord, we can't activate it.
                if(portal.isCrossServer() && !pl.getNetworkClient().isConnected()) {return false;}
                return true;
            }
        };

        // Loop through all active portals and find the closest one to be activated
        // This is for performance - only one portal can be active at a time
        Portal closestPortal = pl.findClosestPortal(player.getLocation(), config.getPortalActivationDistance(), isValidPortal);

        // If a portal has been destroyed, we can't use it.
        if(closestPortal != null && !closestPortal.checkOriginAndDestination()) {return null;}

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
        
        portal.teleportEntity(player);
        return true;
    }
    
    // This function is responsible for iterating over all of the blocks surrounding the portal,
    // and performing a raycast on each of them to check if they should be visible
    public void updatePortal(PlayerData playerData, Portal portal, PlaneIntersectionChecker checker) {        
        // Optimisation: Check if the player has moved before re-rendering the view
        Vector currentLoc = playerData.getPlayer().getLocation().toVector();
        if(currentLoc.equals(playerData.getLastPosition()))  {return;}
        // Queue an update to happen on the async task
        blockRenderer.queueUpdate(playerData, checker, portal);
    }

    private void updateEntities(PlayerData playerData, Portal portal, PlaneIntersectionChecker checker, boolean viewEntitiesThroughPortals)  {
        // Entity processing, be that teleportation through portals or viewing entities through them, is not supported with portals across servers
        if(portal.isCrossServer()) {return;}

        EntityManipulator manipulator = playerData.getEntityManipulator();

        // We need to loop through the entities at the origin regardless of if entities are enabled, since we also need to teleport those going through portals
        Set<Entity> hiddenEntities = new HashSet<>();

        Iterator<Map.Entry<Entity, Vector>> iter = portal.getNearbyEntitiesOrigin().entrySet().iterator();
        while(iter.hasNext())   {
            Map.Entry<Entity, Vector> entry = iter.next();

            Entity entity = entry.getKey();
            Vector lastKnownLocation = entry.getValue();

            // If the entity isn't in the same world, we skip it
            if(entity.getWorld() != portal.getOriginPos().getWorld())   {
                iter.remove();
                continue;
            }

            Vector actualLocation = entity.getLocation().toVector();
            // Teleport the entity if it walked through a portal
            PlaneIntersectionChecker teleportChecker = new PlaneIntersectionChecker(actualLocation, portal);
            if(!(entity instanceof Player) && lastKnownLocation != null && teleportChecker.checkIfVisibleThroughPortal(lastKnownLocation))  {
                portal.teleportEntity(entity);
                portal.getNearbyEntitiesDestination().add(entity);
                iter.remove();
            }

            // Set the location back to the actual location
            entry.setValue(actualLocation);

            // If an entity is visible through the portal, then we hide it
            if(viewEntitiesThroughPortals && checker.checkIfVisibleThroughPortal(entity.getLocation().toVector()) && entity != playerData.getPlayer())  {
                hiddenEntities.add(entity);
            }
        }

        if(!viewEntitiesThroughPortals)  {return;}

        Set<Entity> replicatedEntities = new HashSet<>();
        for(Entity entity : portal.getNearbyEntitiesDestination())   {
            // Don't replicate entities almost exactly in line 
            if(portal.getDestPos().isInLine(entity.getLocation())) {
                continue;
            }

            Vector originPos = portal.getLocTransformer().moveToOrigin(entity.getLocation().toVector());
            // If an entity is visible through the portal, then we replicate it
            if(checker.checkIfVisibleThroughPortal(originPos))  {
                replicatedEntities.add(entity);
            }
        }

        manipulator.updateFakeEntities();
        manipulator.swapHiddenEntities(hiddenEntities);
        manipulator.swapReplicatedEntities(replicatedEntities, portal);
    }

    @Override
    public void run() {
        // Process any requests from the network that must go on the main thread
        pl.getBlockArrayProcessor().processPendingExternalUpdates();

        // Loop through every online player
        for (Player player : pl.getServer().getOnlinePlayers()) {
            PlayerData playerData = pl.getPlayerData(player);
            if(playerData == null) {return;} // Happens just after a player has joined

            // If we changed worlds in the last tick, we wait to avoid chunks not being loaded while sending updates
            if(playerData.checkIfDisabled())    {
                continue;
            }

            boolean canSeeThroughPortals = playerData.getPlayer().hasPermission("betterportals.see");

            // Find the closest portal to the player
            Portal portal = findClosestPortal(player);

            // If no portals were found, don't update anything
            if(portal != null) {
                // Create the portal's block state array if necessary
                portal.update(currentTick);

                if(pl.getBlockArrayProcessor().getBlockDataArray(portal) != null) { // Make sure that we don't run the update if fetching the data array failed
                    // Set the player to be viewing the portal if they can see through portals
                    playerData.setViewingPortal(canSeeThroughPortals ? portal : null);

                    PlaneIntersectionChecker intersectionChecker = new PlaneIntersectionChecker(player, portal);

                    updateEntities(playerData, portal, intersectionChecker, canSeeThroughPortals && config.isEntitySupportEnabled());
                    if(canSeeThroughPortals) {updatePortal(playerData, portal, intersectionChecker);}

                    // Teleport the player if they cross through a portal
                    performPlayerTeleport(playerData, portal, intersectionChecker);
                }   else    {
                    playerData.setViewingPortal(null);
                }
            }   else    {
                playerData.setViewingPortal(null);
            }
            
            playerData.setLastPosition(player.getLocation().toVector());
        }

        currentTick++;

        // If we are using the force loading method, unforceload any chunks that are no longer loaded by portals
        if(ReflectUtils.useNewChunkLoadingImpl) {
            for(ChunkCoordIntPair chunk : pl.getForceLoadedChunks()) {
                if(!newForceLoadedChunks.contains(chunk))   {
                    chunk.getChunk().setForceLoaded(false);
                }
            }
        }

        pl.setForceLoadedChunks(newForceLoadedChunks);
        newForceLoadedChunks = new HashSet<>();
    }
}