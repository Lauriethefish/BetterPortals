package com.lauriethefish.betterportals;

import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.entitymanipulation.PlayerEntityManipulator;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// This class stores information about the player, required by the plugin
public class PlayerData {
    @Getter private Player player;
    
    // Used to disable sending entity packets after a world load
    // TODO: reimplement stuff using these in this class
    private boolean loadedWorldLastTick = true;

    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    @Getter @Setter private Portal lastActivePortal = null;
    // Store the surrouding blocks that have been sent to the player
    @Getter private Map<Vector, Object> surroundingPortalBlockStates = new HashMap<>();

    // Deals with hiding and showing entities
    @Getter private PlayerEntityManipulator entityManipulator;

    // Last position of the player recorded by PlayerRayCast, used to decide whether or not to re-render to portal view
    @Getter @Setter private Vector lastPosition = null;

    public PlayerData(BetterPortals pl, Player player) {
        this.player = player;
        entityManipulator = new PlayerEntityManipulator(pl, this);
    }

    public boolean getIfLoadedWorldLastTick()   {return loadedWorldLastTick;}
    public void setLoadedWorldLastTick()   {loadedWorldLastTick = true;}
    public void unsetLoadedWorldLastTick()   {loadedWorldLastTick = false;}

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    public void resetSurroundingBlockStates(boolean sendPackets)   {
        if(sendPackets && lastActivePortal != null) {
            MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
            // Loop through all of the potential ghost blocks, and add to the change manager to change them back
            for(BlockRaycastData data : lastActivePortal.getCurrentBlocks())   {
                if(!surroundingPortalBlockStates.get(data.originVec).equals(data.originData))    {
                    changeManager.addChange(data.originVec, data.originData);
                }
            }
            changeManager.sendChanges();
        }
        surroundingPortalBlockStates = new HashMap<>();
    }
}