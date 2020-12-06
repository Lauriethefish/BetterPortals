package com.lauriethefish.betterportals.bukkit.portal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.CachedViewableBlocksArray;
import com.lauriethefish.betterportals.network.Response.RequestException;

// Handles creating the array of blocks that aren't obscured by other solid blocks.
public class PortalBlockArrayManager {
    private BetterPortals pl;

    private Map<PortalPosition, CachedViewableBlocksArray> cachedArrays = new HashMap<>();

    public PortalBlockArrayManager(BetterPortals pl) {
        this.pl = pl;
    }

    // Gets the current block array for the specified portal
    // This will throw NullPointerException if no array exists
    public Collection<BlockRaycastData> getBlockDataArray(Portal portal) {
        return cachedArrays.get(portal.getDestPos()).getBlocks();
    }

    public CachedViewableBlocksArray getCachedArray(PortalPosition destPos) {
        // Make a new cached array if one doesn't exist
        if (!cachedArrays.containsKey(destPos)) {
            cachedArrays.put(destPos, new CachedViewableBlocksArray(pl));
        }

        return cachedArrays.get(destPos);
    }

    // Updates/creates the block array if it does not exist
    public void updateBlockArray(GetBlockDataArrayRequest request) {
        long timeBefore = System.nanoTime();

        CachedViewableBlocksArray array = getCachedArray(request.getDestPos());
        
        // We still need to process changes here, even for an external portal, although for external portals only the origin gets processed
        if (request.getDestPos().isExternal()) {
            pl.logDebug("Updating blocks for external portal . . .");
            try {
                array.checkForChanges(request, true, false);
                // Send a request to get which blocks changed at the destination and thus require updating
                BlockDataUpdateResult result = (BlockDataUpdateResult) pl.getNetworkClient().sendRequestToServer(request, request.getDestPos().getServerName());
                array.processExternalUpdate(request, result);
            } catch (RequestException ex) {
                ex.printStackTrace();
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

    // Called whenever a GetBlockDataArrayRequest is received from another server.
    public BlockDataUpdateResult handleGetBlockDataArrayRequest(GetBlockDataArrayRequest request) {
        CachedViewableBlocksArray array = getCachedArray(request.getDestPos());
        Set<Integer> changes = array.checkForChanges(request, false, true); // Check for the changes at the destination

        BlockDataUpdateResult nonObscuredChanges = array.processChanges(request, changes);
        return nonObscuredChanges;
    }
}
