package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

// Stores all of the information about a block that has been modified from the player's perspective
public class BlockConfig {
    // The location is stored so that the block can be changed back later
    public Location location;
    
    // Store Data value of the block
    public BlockData data;

    public BlockConfig(Location location, BlockData data)    {
        this.location = location;
        this.data = data;
    }

    // Checks if all of the values are equal to the other config
    public boolean equals(BlockConfig other)    {
        return other.data.equals(data);
    }
}