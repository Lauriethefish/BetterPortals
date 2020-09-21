package com.lauriethefish.betterportals.runnables;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.BlockRaycastData;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.entity.Player;

public class BlockProcessor implements Runnable {
    // Stores the information required to process the portal block update on another thread
    private class UpdateData {
        public PlayerData playerData;
        public PlaneIntersectionChecker checker;
        public Portal portal;
        public UpdateData(PlayerData playerData, PlaneIntersectionChecker checker, Portal portal)   {
            this.playerData = playerData; this.portal = portal; this.checker = checker;
        }
    }

    private BlockingQueue<UpdateData> updateQueue = new LinkedBlockingQueue<>(); 
    public BlockProcessor(BetterPortals pl) {
        pl.getServer().getScheduler().runTaskTimerAsynchronously(pl, this, 0, 1);
    }

    // Adds a new update to the queue to be processed asyncronously
    public void queueUpdate(PlayerData playerData, PlaneIntersectionChecker checker, Portal portal)  {
        updateQueue.add(new UpdateData(playerData, checker, portal));
    }

    @Override
    public void run() {
        while(true) {
            // Get the next update in the queue, and return if there are no more updates to process
            UpdateData data = updateQueue.poll();
            if(data == null)    {
                return;
            }

            handleUpdate(data);
        }
    }

    // Processes the given update by sending the correctly changed blocks to the player
    private void handleUpdate(UpdateData data)    {
        Player player = data.playerData.player;
        // Skip this portal if the player is no longer in the right world
        if(player.getWorld() != data.portal.portalPosition.getWorld())  {
            return;
        }

        List<BlockRaycastData> currentBlocks = data.portal.currentBlocks; // Store the current blocks incase they change while being processed
        MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
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
    
}
