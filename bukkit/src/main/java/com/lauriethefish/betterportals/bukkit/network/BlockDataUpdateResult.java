package com.lauriethefish.betterportals.bukkit.network;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.bukkit.portal.blockarray.SerializableBlockData;

import lombok.Getter;

// Returned from the remote server after a GetBlockDataArrayRequest is sent
public class BlockDataUpdateResult implements Serializable {
    private static final long serialVersionUID = -6126046509630782868L;
    
    @Getter private Map<Integer, SerializableBlockData> updatedBlocks = new HashMap<>(); // Contains new/updated block position (key is array index)
    @Getter private Set<Integer> removedBlocks = new HashSet<>(); // Any blocks that are now completely obscured and can be removed
}
