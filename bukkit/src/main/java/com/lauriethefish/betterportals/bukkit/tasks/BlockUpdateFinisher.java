package com.lauriethefish.betterportals.bukkit.tasks;

import com.lauriethefish.betterportals.bukkit.player.view.block.PlayerBlockView;
import com.lauriethefish.betterportals.shared.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Handles doing the final processing for portal block updates on another thread
 * Doing tons of raycasts to find which blocks are visible is moderately expensive, so happens on another thread
 */
public abstract class BlockUpdateFinisher {
    private static class BlockViewUpdateInfo {
        PlayerBlockView blockView;
        boolean refresh;
        public BlockViewUpdateInfo(PlayerBlockView blockView, boolean refresh) {
            this.blockView = blockView;
            this.refresh = refresh;
        }

        // Equals only requires the BlockViewUpdateInfo to be equal - used to check if an update is already pending
        @Override
        public boolean equals(Object other) {
            if(!(other instanceof BlockViewUpdateInfo)) {return false;}

            return blockView == ((BlockViewUpdateInfo) other).blockView;
        }
    }

    private final BlockingQueue<BlockViewUpdateInfo> updateQueue = new LinkedBlockingQueue<>();
    protected final Logger logger;

    protected BlockUpdateFinisher(Logger logger) {
        this.logger = logger;
    }

    protected void finishPendingUpdates() {
        while(true) {
            BlockViewUpdateInfo next = updateQueue.poll();
            if(next == null) {return;}

            next.blockView.finishUpdate(next.refresh);
        }
    }

    /**
     * Schedules the update for <code>blockView</code> to happen on another thread.
     * @param blockView The block view to be updated
     * @param refresh Whether to resend all block states regardless of if they were already sent
     */
    public void scheduleUpdate(PlayerBlockView blockView, boolean refresh) {
        BlockViewUpdateInfo updateInfo = new BlockViewUpdateInfo(blockView, refresh);

        if(updateQueue.contains(updateInfo)) {
            logger.fine("Block update was scheduled when previous update had not finished. Server is running behind!");
            return;
        }

        try {
            updateQueue.put(updateInfo);
        }   catch(InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
