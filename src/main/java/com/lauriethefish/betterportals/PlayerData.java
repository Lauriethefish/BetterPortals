package com.lauriethefish.betterportals;

import java.util.HashMap;

import com.lauriethefish.betterportals.entitymanipulation.PlayerEntityManipulator;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// This class stores information about the player, required by the plugin
public class PlayerData {
    // Reference to the player
    public Player player;
    // The destination of the last portal that they used
    // This is used as a form of cooldown for portals
    // If it is null then they were not in a portal during the last tick
    public Location lastUsedPortal = null;

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
        this.player = player;

        //entityManipulator = new PlayerEntityManipulator(pl, this);
    }

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    public void resetSurroundingBlockStates()   {
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

        surroundingPortalBlockStates = new HashMap<>();
    }
}