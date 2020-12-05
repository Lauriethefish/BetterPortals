package com.lauriethefish.betterportals.bukkit.portal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.CachedViewableBlocksArray;


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

    // Updates/creates the block array if it does not exist
    public void updateBlockArray(GetBlockDataArrayRequest request) {
        long timeBefore = System.nanoTime();

        // Make a new cached array if one doesn't exist
        if(!cachedArrays.containsKey(request.getDestPos())) {
            cachedArrays.put(request.getDestPos(), new CachedViewableBlocksArray(pl));
        }

        // Update the cached array and return it
        CachedViewableBlocksArray array = cachedArrays.get(request.getDestPos());
        Set<Integer> changes = array.checkForChanges(request);
        array.processChanges(request, changes);

        long timeTaken = System.nanoTime() - timeBefore;
        pl.logDebug("Time taken: %.03fms", ((double) timeTaken) / 1000000);
    }
}
