package com.lauriethefish.betterportals;

import java.util.HashMap;

import com.lauriethefish.betterportals.entitymanipulation.PlayerEntityManipulator;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// This class stores information about the player, required by the plugin
public class PlayerData {
    // Reference to the player
    public Player player;
    
    // Used to disable sending entity packets after a world load
    public boolean loadedWorldLastTick = false;

    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    public PortalPos lastActivePortal = null;
    // Store the surrouding blocks that have been sent to the player (false = the player can see the origin block, true = the player can see the destination block)
    public HashMap<Vector, Object> surroundingPortalBlockStates = new HashMap<>();

    // Deals with hiding and showing entities
    public PlayerEntityManipulator entityManipulator;

    // Last position of the player recorded by PlayerRayCast, used to decide whether or not to re-render to portal view
    public Vector lastPosition = null;

    public PlayerData(Player player) {
        resetPlayer(player);
    }

    // Used whenever a player relogs/ logs in for the first time
    public void resetPlayer(Player newPlayer)   {
        this.player = newPlayer;
        entityManipulator = new PlayerEntityManipulator(this);

        this.lastPosition = null; // Make sure the portal re-renders
        this.lastActivePortal = null; // No portal was active last tick, since we just logged in
        this.loadedWorldLastTick = true;
        resetSurroundingBlockStates();
    }

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    public void resetSurroundingBlockStates()   {
        surroundingPortalBlockStates = new HashMap<>();

        // If the last portal was in a different world, we don't need to reset the blocks
        if(lastActivePortal == null || lastActivePortal.portalPosition.getWorld() != player.getWorld()) {
            return;
        }

        MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
        // Loop through all of the potential ghost blocks, and add to the change manager to change them back
        for(BlockRaycastData data : lastActivePortal.currentBlocks)   {
            changeManager.addChange(data.originVec, data.originData);
        }
        changeManager.sendChanges();
    }
}