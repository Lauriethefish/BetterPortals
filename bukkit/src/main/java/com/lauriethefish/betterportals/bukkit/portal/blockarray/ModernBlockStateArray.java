package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

class ModernBlockStateArray implements BlockStateArray {
    private final RenderConfig renderConfig;

    private boolean[] occlusion = null; // Array of which blocks fully block out light
    private BlockData[] blockData = null;

    public ModernBlockStateArray(BetterPortals pl) {
        this.renderConfig = pl.getLoadedConfig().getRendering();
    }

    @Override
    public boolean initialise() {
        if(occlusion != null) {return false;} // If we've already initialised both arrays, return false

        // Otherwise, initialise and return true
        occlusion = new boolean[renderConfig.getTotalArrayLength()];
        blockData = new BlockData[renderConfig.getTotalArrayLength()];
        return true;
    }

    // Updates the arrays at the location and index. Returns true if the block changed
    @Override
    public boolean update(Location loc, int index) {
        Block block = loc.getBlock();

        BlockData newBlockData = block.getBlockData();
        // If it has changed
        if(!newBlockData.equals(blockData[index])) {
            // Update the occlusion and combined ID arrays
            blockData[index] = newBlockData;
            occlusion[index] = newBlockData.getMaterial().isOccluding();
            return true;
        }   else    {
            return false;
        }
    }

    @Override
    public boolean isOccluding(int index) {
        return occlusion[index];
    }

    @Override
    public SerializableBlockData getBlockData(int index) {
        BlockData data = blockData[index];

        return new SerializableBlockData(data);
    }
}
