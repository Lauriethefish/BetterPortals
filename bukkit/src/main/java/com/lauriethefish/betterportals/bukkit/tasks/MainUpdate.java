package com.lauriethefish.betterportals.bukkit.tasks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.PlayerData;
import com.lauriethefish.betterportals.bukkit.config.Config;
import com.lauriethefish.betterportals.bukkit.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.portal.Portal;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Brings everything else together - sends updates to the block processor for processing on other threads
// Also handles calling the methods in PortalUpdateManager for portal updating, activating and deactivating.
// Player teleportation is handled here
public class MainUpdate implements Runnable {
    private BetterPortals pl;

    private Config config;

    private BlockProcessor blockRenderer;
    private Set<Portal> activePortals = new HashSet<>();

    public MainUpdate(BetterPortals pl) {
        blockRenderer = new BlockProcessor(pl);
        this.pl = pl;
        this.config = pl.getLoadedConfig();

        // Set the task to run every tick
        pl.getServer().getScheduler().scheduleSyncRepeatingTask(pl, this, 0, 1);
    }


    // Finds the closest portal to the given player,
    // this also deletes portals if they have been broken amongst other things
    // Will return null if not portals can be found within the portal activation distance
    private Portal findClosestPortal(Player player)   {
        Predicate<Portal> isValidPortal = portal -> {
            // If the portal goes across servers, and we aren't connected to bungeecord, we can't activate it.
            return !portal.isCrossServer() || (pl.getNetworkClient() != null && pl.getNetworkClient().isConnected());
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
    // Returns true if the player was teleported, false otherwise
    public boolean performPlayerTeleport(PlayerData playerData, Portal portal, PlaneIntersectionChecker checker)  {
        if(!playerData.canBeTeleportedByPortal()) {return false;} // Enforce teleportation cooldown

        Player player = playerData.getPlayer();
        
        Vector lastPos = playerData.getLastPosition();
        Vector currentPos = player.getLocation().toVector();

        // If the player's position the previous tick was on the other side of the portal window, then we should teleport the player, otherwise return
        if(lastPos == null || !checker.checkIfVisibleThroughPortal(lastPos) || currentPos.equals(lastPos))   {
            return false;
        }

        pl.logDebug("Last Pos: %s", lastPos);
        pl.logDebug("Current pos: %s", player.getLocation().toVector());

        portal.teleportEntity(player);
        playerData.onPortalTeleport();
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
        if(!viewEntitiesThroughPortals)  {return;}

        EntityManipulator manipulator = playerData.getEntityManipulator();

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

            // Set the location back to the actual location
            entry.setValue(actualLocation);

            // If an entity is visible through the portal, then we hide it
            if(checker.checkIfVisibleThroughPortal(entity.getLocation().toVector()) && entity != playerData.getPlayer())  {
                hiddenEntities.add(entity);
            }
        }
        manipulator.swapHiddenEntities(hiddenEntities);

        // Cross server portals cannot process entities from the other side
        if(!portal.isCrossServer()) {
            Set<Entity> replicatedEntities = new HashSet<>();
            for (Entity entity : portal.getNearbyEntitiesDestination()) {
                // Don't replicate entities almost exactly in line
                if (portal.getDestPos().isInLine(entity.getLocation())) {
                    continue;
                }

                Vector originPos = portal.getLocTransformer().moveToOrigin(entity.getLocation().toVector());
                // If an entity is visible through the portal, then we replicate it
                if (checker.checkIfVisibleThroughPortal(originPos)) {
                    replicatedEntities.add(entity);
                }
            }

            manipulator.updateFakeEntities();
            manipulator.swapReplicatedEntities(replicatedEntities, portal);
        }
    }

    private void activatePortal(Portal portal) {
        // Verify that the portal isn't active already
        if(activePortals.contains(portal)) {
            throw new IllegalStateException("Tried to activate already active portal");
        }

        portal.getUpdateManager().onActivate();
        activePortals.add(portal);
    }

    private void deactivatePortal(Portal portal) {
        // Verify that the portal is actually active
        if(!activePortals.contains(portal)) {
            throw new IllegalStateException("Tried to deactivate non-active portal");
        }

        portal.getUpdateManager().onDeactivate();
        activePortals.remove(portal);
    }

    @Override
    public void run() {
        // Process any requests from the network that must go on the main thread
        pl.getBlockArrayProcessor().processPendingExternalUpdates();

        Set<Portal> nonUpdatedActivePortals = new HashSet<>(activePortals);

        // Loop through every online player
        for (Player player : pl.getServer().getOnlinePlayers()) {
            PlayerData playerData = pl.getPlayerData(player);
            if(playerData == null) {return;} // Happens just after a player has joined

            playerData.onTick();

            boolean canTeleport = playerData.canBeTeleportedByPortal();
            boolean canSeeThroughPortals = playerData.canSeeThroughPortals();

            // Find the closest portal to the player
            Portal portal = findClosestPortal(player);

            // If no portals were found, don't update anything
            if(portal != null) {
                // If a portal is gonna be used and wasn't active, call onActivate
                if(!activePortals.contains(portal)) {
                    activatePortal(portal);
                }
                nonUpdatedActivePortals.remove(portal); // Show that the portal has been used this tick and shouldn't be deactivated

                portal.getUpdateManager().playerUpdate();

                // If the block array hasn't been fetched yet, due to it being external and the worker still processing, or some other reason, don't use the portal
                if(portal.getCachedViewableBlocksArray().getBlocks() != null) {
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

            // Avoid repeated teleports by only setting the player's last position if they could've teleported this tick
            if(canTeleport) {playerData.setLastPosition(player.getLocation().toVector());}
        }

        // If an active portal was not removed from this set, then it must be deactivated
        for(Portal portal : nonUpdatedActivePortals) {
            deactivatePortal(portal);
        }
    }
}