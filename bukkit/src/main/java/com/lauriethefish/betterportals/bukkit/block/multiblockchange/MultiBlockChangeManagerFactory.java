package com.lauriethefish.betterportals.bukkit.block.multiblockchange;

import org.bukkit.entity.Player;

public interface MultiBlockChangeManagerFactory {
    IMultiBlockChangeManager create(Player player);
}
