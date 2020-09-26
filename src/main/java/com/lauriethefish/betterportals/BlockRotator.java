package com.lauriethefish.betterportals;

import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.block.BlockState;

// Interface that allows you to use either implementation of BlockRotator
public interface BlockRotator {
    public static BlockRotator newInstance(Portal portal)  {
        if(ReflectUtils.isLegacy)   {
            return new BlockRotator_Legacy(portal);
        }   else    {
            return new BlockRotator_Modern(portal);
        }
    }

    public void rotateToOrigin(BlockState state);
}
