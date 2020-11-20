package com.lauriethefish.betterportals.bukkit;

import com.lauriethefish.betterportals.bukkit.network.GetBlockDataArrayRequest;

import org.bukkit.block.BlockState;

// Interface that allows you to use either implementation of BlockRotator
public interface BlockRotator {
    public static BlockRotator newInstance(GetBlockDataArrayRequest request)  {
        if(ReflectUtils.isLegacy)   {
            return new BlockRotator_Legacy(request);
        }   else    {
            return new BlockRotator_Modern(request);
        }
    }

    public void rotateToOrigin(BlockState state);
}
