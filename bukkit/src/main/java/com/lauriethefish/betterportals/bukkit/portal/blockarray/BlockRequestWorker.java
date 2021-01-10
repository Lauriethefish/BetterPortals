package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.BlockDataArrayRequest;

import lombok.Getter;

public class BlockRequestWorker implements Runnable {
    private BetterPortals pl;
    @Getter private CachedViewableBlocksArray cachedArray;
    private BlockDataArrayRequest request;

    private volatile BlockDataUpdateResult result = null;
    private volatile boolean failed = false;

    public BlockRequestWorker(BetterPortals pl, BlockDataArrayRequest request, CachedViewableBlocksArray cachedArray, boolean runAsync) {
        this.pl = pl;
        this.cachedArray = cachedArray;
        this.request = request;

        // Choose whether on not to run asynchronously
        if(runAsync) {
            new Thread(this).start();
        }   else    {
            run();
        }
    }

    public boolean hasFinished() {
        return result != null;
    }

    public boolean hasFailed() {
        return failed;
    }

    // Called to process the BlockDataUpdateResult on the main thread once it has been fetched (only called for get/update block array requests)
    public void finishUpdate() {
        if(request.getMode() != BlockDataArrayRequest.Mode.GET_OR_UPDATE) {throw new IllegalStateException("Non-get/update requests do not need finishing");}

        cachedArray.checkForChanges(request, true, false);
        cachedArray.processExternalUpdate(request, result);
    }

    @Override
    public void run() {
        String destinationServer = request.getDestPos().getServerName();
        // Make sure we're actually connected
        if(pl.getNetworkClient() == null) {
            pl.getLogger().warning("Update for external portal failed - bungeecord is not enabled!");
            failed = true;
            return;
        }

        // Update with the right code
        switch(request.getMode()) {
            case GET_OR_UPDATE:
                fetchUpdateResult();
                return;
            case CLEAR:
                fetchClearResult();
        }
    }

    // Fetches a BlockDataUpdateResult in order to get the blocks at the destination
    private void fetchUpdateResult() {
        String destinationServer = request.getDestPos().getServerName();
        try {
            // Send the request to the destination server
            Object rawResult = pl.getNetworkClient().sendRequestToServer(request, destinationServer);
            result = (BlockDataUpdateResult) rawResult;
        } catch (Throwable ex) {
            pl.getLogger().warning("An error occurred while fetching the blocks for an external portal. This portal will not activate.");
            failed = true;
            ex.printStackTrace();
        }
    }

    // Sends a request to just clear the block array
    private void fetchClearResult() {
        String destinationServer = request.getDestPos().getServerName();
        try {
            pl.getNetworkClient().sendRequestToServer(request, destinationServer);
        }   catch(Throwable ex) {
            pl.getLogger().warning("Failed to clear block array for an external portal when deactivated");
            failed = true;
            ex.printStackTrace();
        }
    }
}
