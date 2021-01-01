package com.lauriethefish.betterportals.bukkit.runnables;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.PlayerData;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.CachedViewableBlocksArray;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// An asynchronous task that handles sending block updates to the player
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

    // Adds a new update to the queue to be processed asynchronously
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
        Player player = data.playerData.getPlayer();
        // Skip this portal if the player is no longer in the right world
        if(player.getWorld() != data.portal.getOriginPos().getWorld())  {
            return;
        }

        CachedViewableBlocksArray blocksArray = data.portal.getCachedViewableBlocksArray();
        MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
        Map<Vector, Object> blockStates = data.playerData.getSurroundingPortalBlockStates();

        blocksArray.lockWhileInUse();
        for(BlockRaycastData raycastData : blocksArray.getBlocks())    {
            Vector originPos = raycastData.getOriginVec();              
            // Check if the block is visible
            boolean visible = data.checker.checkIfVisibleThroughPortal(originPos);

            Object originData = raycastData.getOriginData().getNmsData();
            Object destData = raycastData.getDestData().getNmsData();

            if(visible) { // If the block is visible through the portal
                // Update the origin data in the map, and if it hadn't been sent already, sent it to the player
                if(blockStates.put(originPos, originData) == null) {
                    changeManager.addChange(originPos, destData);
                }
            }   else if(blockStates.containsKey(originPos)) { // If the block is no longer visible through the portal, and was sent to the player
                // Reset the block back to normal
                blockStates.remove(originPos);
                changeManager.addChange(originPos, originData);
            }
        }
        blocksArray.unlockAfterUse();

        // Send all the block changes
        changeManager.sendChanges();
    }
    
}
