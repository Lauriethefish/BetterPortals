package com.lauriethefish.betterportals.portal;

import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.BlockRaycastData;
import com.lauriethefish.betterportals.Config;

import org.bukkit.Location;

public class BlockStateManager {
    private Portal portal;
    private Config config;
    
    private Map<Integer, BlockRaycastData> states;

    BlockStateManager(Portal portal, Config config) {
        this.portal = portal;
    }

    void update()   {
        if(states == null)  {
            states = new HashMap<>();
        }
    }

    private boolean expandFromPos(Location loc, int index)   {
        boolean transparentSide = false;
        for(int offset : config.surroundingOffsets) {
            int offsetIndex = index + offset;
            
            if(states.get(offsetIndex) != null) {continue;}

            if(expandFromPos(loc, index))   {
                transparentSide = true;
                break;
            }
        }

        if(transparentSide) {
            
        }

        return loc.getBlock().getType().isOccluding();
    }
}
