package com.lauriethefish.betterportals.bukkit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lauriethefish.betterportals.bukkit.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.selection.PortalSelection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// This class stores information about the player, required by the plugin
public class PlayerData {
    private BetterPortals pl;
    @Getter private Player player;
    
    private int ticksSinceTeleport = 0;
    
    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    private Portal lastActivePortal = null;
    // Store the original states of the surrounding blocks that have been sent to the player
    @Getter private Map<Vector, Object> surroundingPortalBlockStates = new ConcurrentHashMap<>();

    // Deals with hiding and showing entities
    @Getter private EntityManipulator entityManipulator;

    // Last position of the player recorded by PlayerRayCast, used to decide whether or not to re-render to portal view
    @Getter private Vector lastPosition = null;

    @Getter @Setter private PortalSelection selection;
    @Getter @Setter private PortalSelection originSelection;
    @Getter @Setter private PortalSelection destinationSelection;

    public PlayerData(BetterPortals pl, Player player) {
        this.pl = pl;
        this.player = player;
        entityManipulator = new EntityManipulator(pl, this);
    }

    // Called every tick while the player is focused on a particular portal
    // If newPortal is null, that means that there is no longer an active portal
    public void setViewingPortal(Portal newPortal)    {
        // Return if the portal stayed the same
        if(newPortal == lastActivePortal)   {return;}

        // No need to send packets to reset block states if changing worlds
        boolean changedWorlds = lastActivePortal == null || lastActivePortal.getOriginPos().getWorld() != player.getWorld();
        resetSurroundingBlockStates(!changedWorlds);

        // Don't hide or recreate portal blocks for custom portals
        if(pl.getLoadedConfig().getRendering().isPortalBlocksHidden() && player.hasPermission("betterportals.see"))  {
            // If we're not in the same world as our last portal, there's no point recreating the portal blocks
            if(!changedWorlds && !lastActivePortal.isCustom())   {
                lastActivePortal.recreatePortalBlocks(player);
            }
            if(newPortal != null && !newPortal.isCustom())  {
                newPortal.removePortalBlocks(player);
            }
        }

        // Destroy any fake entities and recreate any hidden ones
        entityManipulator.resetAll(!changedWorlds);
        lastActivePortal = newPortal;
        lastPosition = null;
    }

    // Increments the number of ticks since the player last teleported through a portal
    public void onTick()    {
        ticksSinceTeleport++;
    }

    public void onPortalTeleport() {
        ticksSinceTeleport = 0;
        // Set the last position to avoid getting stuck between the portals
        updateLastPosition();
    }

    // Sets the players last position to their current position
    public void updateLastPosition() {
        lastPosition = player.getLocation().toVector();
    }

    // Enforce the rendering delay and permission
    public boolean canSeeThroughPortals() {
        return ticksSinceTeleport >= pl.getLoadedConfig().getRendering().getWorldSwitchWaitTime() && player.hasPermission("betterportals.see");
    }

    // Enforce the teleportation delay and permission
    public boolean canBeTeleportedByPortal() {
        return ticksSinceTeleport >= pl.getLoadedConfig().getTeleportCooldown();
    }

    // Resets all of the ghost block updates that have been set to the player
    public void resetSurroundingBlockStates(boolean sendPackets)   {
        if(sendPackets && lastActivePortal != null) {
            MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);

            // Change all blocks back to their original state
            for(Map.Entry<Vector, Object> entry : surroundingPortalBlockStates.entrySet()) {
                changeManager.addChange(entry.getKey(), entry.getValue());
            }

            changeManager.sendChanges();
        }
        surroundingPortalBlockStates = new ConcurrentHashMap<>();
    }

    public void makeSelection(Location location, Action hand)  {
        World world = location.getWorld();
        // Make a new selection if switching worlds
        if(selection == null || selection.getWorld() != world)  {
            selection = new PortalSelection(world);
        }

        // Set either position A or B
        if(hand == Action.LEFT_CLICK_BLOCK)  {
            selection.setPositionA(location.toVector());
            player.sendMessage(pl.getLoadedConfig().getMessages().getChatMessage("setPosA"));
        }   else    {
            selection.setPositionB(location.toVector());
            player.sendMessage(pl.getLoadedConfig().getMessages().getChatMessage("setPosB"));
        }
    }
}