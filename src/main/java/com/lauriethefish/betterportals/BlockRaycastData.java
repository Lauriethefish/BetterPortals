package com.lauriethefish.betterportals;

import com.lauriethefish.betterportals.runnables.PlayerRayCast;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData {
    public Vector originVec;
    public BlockData originData;
    public BlockData destData;
    
    public BlockRaycastData(Location originLoc, Location destLoc, BlockData destData)   {
        this.originVec = PlayerRayCast.moveVectorToCenterOfBlock(originLoc.toVector());
        this.originData = originLoc.getBlock().getBlockData();
        this.destData = destData;
    }
}