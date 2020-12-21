package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;

// Handles creating the array of blocks that aren't obscured by other solid blocks.
public class PortalBlockArrayManager {
    private BetterPortals pl;

    private Map<GetBlockDataArrayRequest, CachedViewableBlocksArray> cachedArrays = new HashMap<>();
    private Map<GetBlockDataArrayRequest, ExternalUpdateWorker> externalUpdateWorkers = new HashMap<>();

    public PortalBlockArrayManager(BetterPortals pl) {
        this.pl = pl;
    }

    // Gets the current block array for the specified portal
    // This will throw NullPointerException if no array exists
    public Collection<BlockRaycastData> getBlockDataArray(GetBlockDataArrayRequest request) {
        return cachedArrays.get(request).getBlocks();
    }

    public CachedViewableBlocksArray getCachedArray(GetBlockDataArrayRequest request) {
        // Make a new cached array if one doesn't exist
        if (!cachedArrays.containsKey(request)) {
            cachedArrays.put(request, new CachedViewableBlocksArray(pl));
        }

        return cachedArrays.get(request);
    }

    // Updates/creates the block array if it does not exist
    public void updateBlockArray(GetBlockDataArrayRequest request) {
        long timeBefore = System.nanoTime();

        CachedViewableBlocksArray array = getCachedArray(request);
        
        // We still need to process changes here, even for an external portal, although for external portals only the origin gets processed
        if (request.getDestPos().isExternal()) {
            if(pl.getNetworkClient() != null && !pl.getNetworkClient().isConnected()) {return;}

            pl.logDebug("Updating blocks for external portal . . .");
            if(!externalUpdateWorkers.containsKey(request)) {
                externalUpdateWorkers.put(request, new ExternalUpdateWorker(pl, request, array));
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

    private Map<GetBlockDataArrayRequest, BlockDataUpdateResult> pendingRequests = Collections.synchronizedMap(new HashMap<>()); // We need null values, so can't use ConcurrentHashMap
    // Called whenever a GetBlockDataArrayRequest is received from another server.
    public BlockDataUpdateResult handleGetBlockDataArrayRequest(GetBlockDataArrayRequest request) {
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

        for(Map.Entry<GetBlockDataArrayRequest, BlockDataUpdateResult> entry : pendingRequests.entrySet()) {
            if(entry.getValue() != null) {continue;} // Skip if already processed

            BlockDataUpdateResult result = handleGetBlockDataArrayRequestInternal(entry.getKey());
            pendingRequests.put(entry.getKey(), result);
        }

        Iterator<ExternalUpdateWorker> iterator = externalUpdateWorkers.values().iterator();
        while(iterator.hasNext()) {
            ExternalUpdateWorker worker = iterator.next();
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

    private BlockDataUpdateResult handleGetBlockDataArrayRequestInternal(GetBlockDataArrayRequest request) {
        CachedViewableBlocksArray array = getCachedArray(request);
        Set<Integer> changes = array.checkForChanges(request, false, true); // Check for the changes at the destination

        BlockDataUpdateResult nonObscuredChanges = array.processChanges(request, changes);
        return nonObscuredChanges;
    }
}
