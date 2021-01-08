package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import lombok.Getter;

class LegacyBlockStateArray implements BlockStateArray {
    private final RenderConfig renderConfig;

    @Getter private boolean[] occlusion = null; // Array of which blocks fully block out light
    @Getter private Material[] materials = null; // Array of block materials
    @Getter private byte[] dataValues = null; // Array of the data values

    public LegacyBlockStateArray(BetterPortals pl) {
        this.renderConfig = pl.getLoadedConfig().getRendering();
    }

    @Override
    public boolean initialise() {
        if(occlusion != null) {return false;} // If we've already initialised both arrays, return false
        
        // Otherwise, initialise and return true
        occlusion = new boolean[renderConfig.getTotalArrayLength()];
        materials = new Material[renderConfig.getTotalArrayLength()];
        dataValues = new byte[renderConfig.getTotalArrayLength()];
        return true;
    }

    // Updates the arrays at the location and index. Returns true if the block changed
    @SuppressWarnings("deprecation")
    @Override
    public boolean update(Location loc, int index) {
        Block block = loc.getBlock();

        Material material = block.getType();
        byte data = block.getData();
        // If it has changed
        if(materials[index] != material || dataValues[index] != data) {
            // Update the occlusion and combined ID arrays
            materials[index] = material;
            dataValues[index] = data;
            occlusion[index] = material.isOccluding();
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
        return new SerializableBlockData(materials[index], dataValues[index]);
    }
}
