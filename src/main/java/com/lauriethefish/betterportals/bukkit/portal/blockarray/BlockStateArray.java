package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.Config;

import org.bukkit.Location;
import org.bukkit.block.Block;

import lombok.Getter;

class BlockStateArray {
    private Config config;
    
    @Getter private boolean[] occlusion = null; // Array of which blocks fully block out light
    @Getter private int[] combinedIds = null; // Array of combined block IDs

    public BlockStateArray(BetterPortals pl) {
        this.config = pl.config;
    }

    boolean initialise() {
        if(occlusion != null) {return false;} // If we've already initialised both arrays, return false
        
        // Otherwise, initialise and return true
        occlusion = new boolean[config.totalArrayLength];
        combinedIds = new int[config.totalArrayLength];
        return true;
    }

    // Updates the arrays at the location and index. Returns true if the block changed
    @SuppressWarnings("deprecation")
    boolean update(Location loc, int index) {
        Block block = loc.getBlock();
        int combinedId = block.getType().getId() + (block.getData() << 12); // Find the combined block ID at this position

        // If it has changed
        if(combinedIds[index] != combinedId) {
            // Update the occlusion and combined ID arrays
            combinedIds[index] = combinedId;
            occlusion[index] = block.getType().isOccluding();
            return true;
        }   else    {
            return false;
        }
    }
}
