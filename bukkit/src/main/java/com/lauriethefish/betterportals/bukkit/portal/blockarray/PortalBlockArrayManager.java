package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.BlockDataArrayRequest;

// Handles creating the array of blocks that aren't obscured by other solid blocks.
public class PortalBlockArrayManager {
    private BetterPortals pl;

    private Map<BlockDataArrayRequest, CachedViewableBlocksArray> cachedArrays = new ConcurrentHashMap<>();
    private Map<BlockDataArrayRequest, BlockRequestWorker> externalUpdateWorkers = new ConcurrentHashMap<>();

    public PortalBlockArrayManager(BetterPortals pl) {
        this.pl = pl;
    }

    // Gets the current block array for the specified portal
    // This will throw NullPointerException if no array exists
    public Collection<BlockRaycastData> getBlockDataArray(BlockDataArrayRequest request) {
        return cachedArrays.get(request).getBlocks();
    }

    public CachedViewableBlocksArray getCachedArray(BlockDataArrayRequest request) {
        // Make a new cached array if one doesn't exist, or if the origin state array ID is different
        if (!cachedArrays.containsKey(request)) {
            cachedArrays.put(request, new CachedViewableBlocksArray(pl));
        }

        CachedViewableBlocksArray array = cachedArrays.get(request);

        // If the stated array ID on the origin server does not match the one in our current array, wipe the array and return a new one
        UUID arrayId = request.getOriginStateArrayId();
        if(arrayId != null && !array.getArrayId().equals(arrayId)) {
            pl.logDebug("Invalidating array due to invalid ID");
            return cachedArrays.put(request, new CachedViewableBlocksArray(pl, arrayId));
        }

        return array;
    }

    // Clears the cached array to free memory when a portal is unloaded
    // If waitForFinish is true, this will block on clearing external block arrays
    public void clearCachedArray(BlockDataArrayRequest request, boolean waitForFinish) {
        pl.logDebug("Clearing cached array");
        cachedArrays.remove(request);
        externalUpdateWorkers.remove(request);

        if(request.getDestPos().isExternal()) {
            pl.logDebug("Clearing cached array at destination");
            // No need to pass a cached array - we're only clearing it. We also shouldn't add it to the external update workers as it doesn't need any kind of finishing
            new BlockRequestWorker(pl, request, null, !waitForFinish);
        }
    }

    // Called on plugin disable, cleans up external block arrays
    public void cleanUp() {
        pl.logDebug("Clearing up leftover block arrays");
        for(BlockDataArrayRequest request : cachedArrays.keySet()) {
            BlockDataArrayRequest clearRequest = new BlockDataArrayRequest(request.getOriginPos(), request.getDestPos(), BlockDataArrayRequest.Mode.CLEAR);

            clearCachedArray(clearRequest, true);
        }
    }

    // Updates/creates the block array if it does not exist
    public void updateBlockDataArray(BlockDataArrayRequest request) {
        long timeBefore = System.nanoTime();

        CachedViewableBlocksArray array = getCachedArray(request);
        request.setOriginStateArrayId(array.getArrayId()); // Guarantee that we get a new array from the destination server, not non-existing updates from an existing one
        
        // We still need to process changes here, even for an external portal, although for external portals only the origin gets processed
        if (request.getDestPos().isExternal()) {
            if(pl.getNetworkClient() != null && !pl.getNetworkClient().isConnected()) {return;}

            pl.logDebug("Updating blocks for external portal . . .");
            if(!externalUpdateWorkers.containsKey(request)) {
                externalUpdateWorkers.put(request, new BlockRequestWorker(pl, request, array, true));
            }   else    {
                pl.getLogger().warning("External portal update lagging behind, worker still processing next update attempt.");
            }
        }   else    {
            pl.logDebug("Updating blocks for local portal . . .");
            // Update the cached array based on which blocks changed at the origin/destination
            // This can all be done here - no need to send a request since the portal is local.
            Set<Integer> changes = array.checkForChanges(request, true, true);
            array.processChanges(request, changes);

        }
        long timeTaken = System.nanoTime() - timeBefore;
        pl.logDebug("Time taken: %.03fms", ((double) timeTaken) / 1000000);
    }

    private Map<BlockDataArrayRequest, BlockDataUpdateResult> pendingRequests = Collections.synchronizedMap(new HashMap<>()); // We need null values, so can't use ConcurrentHashMap
    // Called whenever a GetBlockDataArrayRequest is received from another server.
    public BlockDataUpdateResult handleGetBlockDataArrayRequest(BlockDataArrayRequest request) {
        // Clear the cached array if requested to
        if(request.getMode() == BlockDataArrayRequest.Mode.CLEAR) {
            pl.logDebug("Clearing array from external server");
            cachedArrays.remove(request);
            return null; // Return null if just clearing the array
        }

        pendingRequests.put(request, null);

        // Wait until the request has been processed by the main thread
        while(pendingRequests.get(request) == null) {
            try {
				Thread.sleep(10);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
        }

        return pendingRequests.remove(request); // Return the result from the main thread, removing it from the pending list
    }

    // Processes any external block updates that need to be sent back to the origin server
    // Also finishes any workers that now have the request from the destination server
    public void processPendingExternalUpdates() {
        if(pendingRequests.size() > 0)  {
            pl.logDebug("Processing %d external updates at destination . . .", pendingRequests.size());
        }

        for(Map.Entry<BlockDataArrayRequest, BlockDataUpdateResult> entry : pendingRequests.entrySet()) {
            if(entry.getValue() != null) {continue;} // Skip if already processed

            BlockDataUpdateResult result = handleGetBlockDataArrayRequestInternal(entry.getKey());
            pendingRequests.put(entry.getKey(), result);
        }

        Iterator<BlockRequestWorker> iterator = externalUpdateWorkers.values().iterator();
        while(iterator.hasNext()) {
            BlockRequestWorker worker = iterator.next();
            if(worker.hasFailed()) {
                iterator.remove(); // Just remove the worker - the error has already been printed by the worker thread
            }
            
            // If the worker has finished, we can process the fetched blocks on the main thread.
            if(worker.hasFinished()) {
                pl.logDebug("Finishing external update . . .");
                worker.finishUpdate();
                iterator.remove();
            }
        }
    }

    private BlockDataUpdateResult handleGetBlockDataArrayRequestInternal(BlockDataArrayRequest request) {
        CachedViewableBlocksArray array = getCachedArray(request);

        Set<Integer> changes = array.checkForChanges(request, false, true); // Check for the changes at the destination

        return array.processChanges(request, changes);
    }
}
