package com.lauriethefish.betterportals.bukkit;

import com.lauriethefish.betterportals.bukkit.math.MathUtils;
import com.lauriethefish.betterportals.bukkit.portal.Portal;

import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

// Handles rotating a BlockState in modern versions (those with BlockData)
public class BlockRotator_Modern implements BlockRotator    {
    private Portal portal;
    public BlockRotator_Modern(Portal portal)  {
        this.portal = portal;
    }

    @Override
    public void rotateToOrigin(BlockState state)    {
        // Rotating blocks is not necessary if the two portals face in the same direction
        if(portal.getOriginPos().getDirection() == portal.getDestPos().getDirection())    {return;}

        BlockData data = state.getBlockData();
        if(data instanceof Directional)   {
            Directional rotatable = (Directional) data;
            // Get the face as a vector, and rotate it with the portals matrix, then set the direction to the new one
            Vector finalDir = MathUtils.round(portal.getLocTransformer().rotateToOrigin(rotatable.getFacing().getDirection()));
            rotatable.setFacing(ReflectUtils.getBlockFace(finalDir));
            state.setBlockData(rotatable); // Set the modified block data
        }
    }
}
