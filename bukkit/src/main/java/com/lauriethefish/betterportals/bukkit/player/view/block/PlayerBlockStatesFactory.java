package com.lauriethefish.betterportals.bukkit.player.view.block;

import org.bukkit.entity.Player;

public interface PlayerBlockStatesFactory {
    IPlayerBlockStates create(Player player);
}
