package com.lauriethefish.betterportals.bukkit.portal;

import java.util.ArrayList;
import java.util.List;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.BlockRotator;
import com.lauriethefish.betterportals.bukkit.Config;
import com.lauriethefish.betterportals.bukkit.math.MathUtils;
import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;

import org.bukkit.Location;

// Handles creating the array of blocks that aren't obscured by other solid blocks.
public class PortalBlockArrayProcessor {
    private Config config;

    public PortalBlockArrayProcessor(BetterPortals pl) {
        this.config = pl.config;
    }

    public List<BlockRaycastData> findPortalDataArray(GetBlockDataArrayRequest request) {
        List<BlockRaycastData> newBlocks = new ArrayList<>();
        boolean[] occlusionArray = findBlockOcclusionArray(request);

        BlockRotator blockRotator = BlockRotator.newInstance(request);

        // Check to see if each block is fully obscured, if not, add it to the list
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    int arrayIndex = config.calculateBlockArrayIndex(x, y, z);

                    Location originBlockLoc = MathUtils.moveToCenterOfBlock(request.getOriginPos().getLocation().add(x, y, z));
                    Location destLoc = request.getTransformations().moveToDestination(originBlockLoc);
                    
                    // Skip blocks directly in line with the portal
                    if(request.getOriginPos().isInLine(originBlockLoc)) {continue;}
                    
                    // First check if the block is visible from any neighboring block
                    boolean transparentBlock = false;
                    for(int offset : config.surroundingOffsets) {
                        int finalIndex = arrayIndex + offset;
                        if(finalIndex < 0 || finalIndex >= config.totalArrayLength) {
                            continue;
                        }

                        if(!occlusionArray[finalIndex])  {
                            transparentBlock = true;
                            break;
                        }
                    }

                    // If the block is bordered by at least one transparent block, add it to the list
                    if(transparentBlock)    {
                        boolean edge = x == config.maxXZ || x == config.minXZ || z == config.maxXZ || z == config.minXZ || y == config.maxY || y == config.minY;
                        newBlocks.add(new BlockRaycastData(blockRotator, originBlockLoc, destLoc, edge));
                    }
                }
            }
        }
        
        return newBlocks;
    }

    // Returns an array of which blocks around the portal allow light through, and which don't.
    private boolean[] findBlockOcclusionArray(GetBlockDataArrayRequest request) {
        // Loop through the surrounding blocks, and check which ones are occluding
        boolean[] occlusionArray = new boolean[config.totalArrayLength];
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    Location originBlockLoc = MathUtils.moveToCenterOfBlock(request.getOriginPos().getLocation().add(x, y, z));
                    // Find the position at the destination of the portal
                    Location position = request.getTransformations().moveToDestination(originBlockLoc);
                    // Find if the type at that position is occuluding
                    occlusionArray[config.calculateBlockArrayIndex(x, y, z)] = position.getBlock().getType().isOccluding();
                }
            }
        }

        return occlusionArray;
    }
}
