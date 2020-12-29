package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;

import lombok.Getter;

public class ExternalUpdateWorker implements Runnable {
    private BetterPortals pl;
    @Getter private CachedViewableBlocksArray cachedArray;
    private GetBlockDataArrayRequest request;

    private volatile BlockDataUpdateResult result = null;
    private volatile boolean failed = false;

    public ExternalUpdateWorker(BetterPortals pl, GetBlockDataArrayRequest request, CachedViewableBlocksArray cachedArray) {
        this.pl = pl;
        this.cachedArray = cachedArray;
        this.request = request;

        new Thread(this).start();
    }

    public boolean hasFinished() {
        return result != null;
    }

    public boolean hasFailed() {
        return failed;
    }

    // Called to process the BlockDataUpdateResult on the main thread once it has been fetched
    public void finishUpdate() {
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

        try {
            // Send the request to the destination server
            Object rawResult = pl.getNetworkClient().sendRequestToServer(request, destinationServer);
            result = (BlockDataUpdateResult) rawResult;
        } catch (Throwable ex) {
            pl.getLogger().warning("An error occured while fetching the blocks for an external portal. This portal will not activate.");
            failed = true;
            ex.printStackTrace();
        }
    }
}
