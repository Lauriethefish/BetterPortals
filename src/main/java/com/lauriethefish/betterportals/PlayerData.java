package com.lauriethefish.betterportals;

import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.entitymanipulation.EntityManipulator;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// This class stores information about the player, required by the plugin
public class PlayerData {
    private BetterPortals pl;
    @Getter private Player player;
    
    // Used to disable sending entity packets after a world load
    private boolean loadedWorldLastTick = true;

    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    private Portal lastActivePortal = null;
    // Store the surrouding blocks that have been sent to the player
    @Getter private Map<Vector, Object> surroundingPortalBlockStates = new HashMap<>();

    // Deals with hiding and showing entities
    @Getter private EntityManipulator entityManipulator;

    // Last position of the player recorded by PlayerRayCast, used to decide whether or not to re-render to portal view
    @Getter @Setter private Vector lastPosition = null;

    public PlayerData(BetterPortals pl, Player player) {
        this.pl = pl;
        this.player = player;
        entityManipulator = new EntityManipulator(pl, this);
    }

    public boolean getIfLoadedWorldLastTick()   {return loadedWorldLastTick;}
    public void setLoadedWorldLastTick()   {loadedWorldLastTick = true;}
    public void unsetLoadedWorldLastTick()   {loadedWorldLastTick = false;}

    // Called every tick while the player is focused on a particular portal
    // If newPortal is null, that means that there is no longer an active portal
    public void setPortal(Portal newPortal)    {
        // Return if the portal stayed the same
        if(newPortal == lastActivePortal)   {return;}

        // No need to send packets to reset block states if changing worlds
        boolean changedWorlds = lastActivePortal == null || lastActivePortal.getOriginPos().getWorld() != player.getWorld();
        resetSurroundingBlockStates(!changedWorlds);

        if(pl.config.hidePortalBlocks)  {
            // If we're not in the same world as our last portal, there's no point recreating the portal blocks
            if(!changedWorlds)   {
                lastActivePortal.recreatePortalBlocks(player);
            }
            if(newPortal != null)  {
                newPortal.removePortalBlocks(player);
            }
        }

        // Destroy any fake entities and recreate any hidden ones
        entityManipulator.resetAll(!changedWorlds);
        lastActivePortal = newPortal;
        lastPosition = null;
    }

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    public void resetSurroundingBlockStates(boolean sendPackets)   {
        if(sendPackets && lastActivePortal != null) {
            MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
            // Loop through all of the potential ghost blocks, and add to the change manager to change them back
            for(BlockRaycastData data : lastActivePortal.getCurrentBlocks())   {
                if(!surroundingPortalBlockStates.get(data.getOriginVec()).equals(data.getOriginData()))    {
                    changeManager.addChange(data.getOriginVec(), data.getOriginData());
                }
            }
            changeManager.sendChanges();
        }
        surroundingPortalBlockStates = new HashMap<>();
    }
}