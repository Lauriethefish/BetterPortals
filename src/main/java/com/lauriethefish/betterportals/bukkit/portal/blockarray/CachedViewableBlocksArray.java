package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.BlockRotator;
import com.lauriethefish.betterportals.bukkit.Config;
import com.lauriethefish.betterportals.bukkit.math.MathUtils;
import com.lauriethefish.betterportals.bukkit.network.BlockDataUpdateResult;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

public class CachedViewableBlocksArray {
    // Contains a mapping of the index in the block occlusion array to the BlockRaycastData
    private Map<Integer, BlockRaycastData> blocks = new HashMap<>();

    private BlockStateArray blockStatesOrigin;
    private BlockStateArray blockStatesDestination;

    private Config config;
    private BetterPortals pl;

    // Initial constructor, called when a new Cached array is created
    public CachedViewableBlocksArray(BetterPortals pl) {
        this.config = pl.config;
        this.pl = pl;
        blockStatesOrigin = new BlockStateArray(pl);
        blockStatesDestination = new BlockStateArray(pl);
    }

    // Returns teh current list of blocks
    public Collection<BlockRaycastData> getBlocks() {
        return blocks.values();
    }

    public Set<Integer> checkForChanges(GetBlockDataArrayRequest request) {
        boolean updateOrigin = request.getDestPos().isExternal();

        // If this is the first update, then we have to update all blocks.
        boolean updateAll = (updateOrigin && blockStatesOrigin.initialise()) | (updateOrigin && blockStatesDestination.initialise());

        Set<Integer> positionsNeedUpdating = new HashSet<>();
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    Location originBlockLoc = MathUtils.moveToCenterOfBlock(request.getOriginPos().getLocation().add(x, y, z));
                    
                    // Find the position at the destination of the portal
                    Location destBlockLoc = request.getTransformations().moveToDestination(originBlockLoc);
                    // Find if the type at that position is occuluding
                    int arrayIndex = config.calculateBlockArrayIndex(x, y, z);
                    // If the block state has changed, then we need to update all blocks around this one
                    
                    boolean originChanged = updateOrigin && blockStatesOrigin.update(originBlockLoc, arrayIndex);
                    boolean destinationChanged =  blockStatesDestination.update(destBlockLoc, arrayIndex);

                    // If a destination block changed, or this is the initial update, we need to loop around the surrounding blocks and update them
                    if(destinationChanged || updateAll) {
                        for(int offset : config.surroundingOffsets) {
                            positionsNeedUpdating.add(arrayIndex + offset);
                        }
                    }
                    if(originChanged || destinationChanged) { // Even if only the origin changed, we must update the current block
                        positionsNeedUpdating.add(arrayIndex);
                    }
                }
            }
        }

        return positionsNeedUpdating;
    }

    // This is either called on the external server whenever an update is required, or on the local server for local portals
    // If it's called locally, null will be returned, otherwise the result will be returned to be sent back to the origin server
    public BlockDataUpdateResult processChanges(GetBlockDataArrayRequest request, Set<Integer> changes) {
        boolean external = request.getDestPos().isExternal();

        BlockDataUpdateResult result = new BlockDataUpdateResult();

        // For each of the positions that have changed, we need to update them
        BlockRotator rotator = BlockRotator.newInstance(request);
        pl.logDebug("Block update count %d", changes.size());
        for(int index : changes) {
            if(isOutOfBounds(index)) {continue;}

            // Check the surrounding block's occlusion values to see if it is fully covered
            boolean isFullySurrounded = true;
            for(int offset : config.surroundingOffsets) {
                if(!isOutOfBounds(index + offset) && !blockStatesDestination.getOcclusion()[index + offset]) {
                    isFullySurrounded = false;
                    break;
                }
            }
    
            if(isFullySurrounded && blocks.containsKey(index)) { // If the block is fully surrounded and currently has a mapping, then we should remove it
                if(external) {
                    // If the portal's origin is on another server, then we tell the origin that we need to remove this block
                    result.getRemovedBlocks().add(index);
                }   else    {
                    blocks.remove(index); // Otherwise, we can just remove it now
                }
            }   else if(!isFullySurrounded) { // If the block is not fully surrounded, and there is not a mapping, we need to add one
                Vector relativePos = config.calculateRelativePos(index);
                Location originLoc = MathUtils.moveToCenterOfBlock(request.getOriginPos().getLocation().add(relativePos));
                Location destLoc = request.getTransformations().moveToDestination(originLoc);

                // Find the destination combined ID, making sure to rotate the block if necessary
                BlockState destState = destLoc.getBlock().getState();
                rotator.rotateToOrigin(destState);
                int destCombinedID = BlockRaycastData.getCombinedId(destState);

                if(external) {
                    // If this portal has its destination on another server, then we send the destination block ID back to the origin server
                    result.getUpdatedBlocks().put(index, destCombinedID);
                }   else    {
                    // Otherwise, we can also find the origin data and just add it directly to the list
                    int originCombinedID = BlockRaycastData.getCombinedId(originLoc.getBlock().getState());
                    // Make a new BlockRaycastData
                    BlockRaycastData newData = new BlockRaycastData(originLoc.toVector(), originCombinedID, destCombinedID, isEdge(relativePos));
                    blocks.put(index, newData);
                }
            } 
        }

        return external ? result : null;
    }

    // Checks if the portal relative coordinates are on the edge - they should be replaces with black concrete
    private boolean isEdge(Vector vec) {
        return vec.getX() == config.maxXZ || vec.getX() == config.minXZ || vec.getZ() == config.maxXZ || vec.getZ() == config.minXZ || vec.getY() == config.maxY || vec.getY() == config.minY;
    }

    private boolean isOutOfBounds(int index) {
        return index >= config.totalArrayLength || index < 0;
    }
}
