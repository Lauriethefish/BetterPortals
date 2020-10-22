package com.lauriethefish.betterportals;

import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.portal.Portal;
import com.lauriethefish.betterportals.selection.PortalSelection;

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
    
    // Setting this value will stop portals from being rendered for this player for the number of ticks
    @Setter private int disableTime = 0;
    
    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    private Portal lastActivePortal = null;
    // Store the surrouding blocks that have been sent to the player
    @Getter private Map<Vector, Object> surroundingPortalBlockStates = new HashMap<>();

    // Deals with hiding and showing entities
    @Getter private EntityManipulator entityManipulator;

    // Last position of the player recorded by PlayerRayCast, used to decide whether or not to re-render to portal view
    @Getter @Setter private Vector lastPosition = null;

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
    public void setPortal(Portal newPortal)    {
        // Return if the portal stayed the same
        if(newPortal == lastActivePortal)   {return;}

        // No need to send packets to reset block states if changing worlds
        boolean changedWorlds = lastActivePortal == null || lastActivePortal.getOriginPos().getWorld() != player.getWorld();
        resetSurroundingBlockStates(!changedWorlds);

        // Don't hide or recreate portal blocks for custom portals
        if(pl.config.hidePortalBlocks)  {
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

    // Ticks down the disabled time if it's greater than 0, and returns true if it is greater than 0
    public boolean checkIfDisabled()    {
        if(disableTime > 0) {
            disableTime--;
            return true;
        }   else    {
            return false;
        }
    }

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    public void resetSurroundingBlockStates(boolean sendPackets)   {
        if(sendPackets && lastActivePortal != null) {
            MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
            // Loop through all of the potential ghost blocks, and add to the change manager to change them back
            for(BlockRaycastData data : lastActivePortal.getCurrentBlocks())   {
                changeManager.addChange(data.getOriginVec(), data.getOriginData());
            }
            changeManager.sendChanges();
        }
        surroundingPortalBlockStates = new HashMap<>();
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
            player.sendMessage(pl.config.getChatMessage("setPosA"));
        }   else    {
            selection.setPositionB(location.toVector());
            player.sendMessage(pl.config.getChatMessage("setPosB"));
        }
    }
}