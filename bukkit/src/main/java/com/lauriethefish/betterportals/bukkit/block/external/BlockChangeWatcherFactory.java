package com.lauriethefish.betterportals.bukkit.block.external;

import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;

public interface BlockChangeWatcherFactory {
    IBlockChangeWatcher create(GetBlockDataChangesRequest request);
}
