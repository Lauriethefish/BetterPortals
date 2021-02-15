package com.lauriethefish.betterportals.bukkit.player;

import org.bukkit.entity.Player;

public interface PlayerDataFactory {
    IPlayerData create(Player player);
}
