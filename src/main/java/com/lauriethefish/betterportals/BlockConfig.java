package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.Material;

// Stores all of the information about a block that has been modified from the player's perspective
public class BlockConfig {
    // The location is stored so that the block can be changed back later
    public Location location;
    
    // Store the type and data value of the block
    public Material material;
    public byte data;

    public BlockConfig(Location location, Material material, byte data)    {
        this.location = location;
        this.material = material;
        this.data = data;
    }

    // Checks if all of the values are equal to the other config
    public boolean equals(BlockConfig other)    {
        return other.material == material && other.data == data;
    }
}