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
    private BetterPortals pl;

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
        this.pl = pl;
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

        CachedViewableBlocksArray blocksArray = pl.getBlockArrayProcessor().getCachedArray(data.portal.getDestPos());
        MultiBlockChangeManager changeManager = MultiBlockChangeManager.createInstance(player);
        Map<Vector, Object> blockStates = data.playerData.getSurroundingPortalBlockStates();

        blocksArray.lockWhileInUse();
        for(BlockRaycastData raycastData : blocksArray.getBlocks())    {
            Vector originPos = raycastData.getOriginVec();              
            // Check if the block is visible
            boolean visible = data.checker.checkIfVisibleThroughPortal(originPos);

            Object oldState = blockStates.get(originPos); // Find if it was visible last tick
            Object newState = visible ? raycastData.getDestData().getNmsData() : raycastData.getOriginData().getNmsData();

            // If we are overwriting the block, change it in the player's block array and send them a block update
            if(!newState.equals(oldState)) {
                blockStates.put(originPos, newState);
                changeManager.addChange(originPos, newState);
            }
        }
        blocksArray.unlockAfterUse();

        // Send all the block changes
        changeManager.sendChanges();
    }
    
}
