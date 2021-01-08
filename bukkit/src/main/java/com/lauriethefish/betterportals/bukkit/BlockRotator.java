package com.lauriethefish.betterportals.bukkit;

import com.lauriethefish.betterportals.bukkit.network.BlockDataArrayRequest;

import org.bukkit.block.BlockState;

// Interface that allows you to use either implementation of BlockRotator
public interface BlockRotator {
    static BlockRotator newInstance(BlockDataArrayRequest request)  {
        if(ReflectUtils.isLegacy)   {
            return new BlockRotator_Legacy(request);
        }   else    {
            return new BlockRotator_Modern(request);
        }
    }

    void rotateToOrigin(BlockState state);
}
