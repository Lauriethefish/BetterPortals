package com.lauriethefish.betterportals.bukkit.player.view;

import com.lauriethefish.betterportals.bukkit.player.view.block.IPlayerBlockView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.IPlayerEntityView;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Player;

public interface ViewFactory {
    IPlayerBlockView createBlockView(Player player, IPortal portal);
    IPlayerEntityView createEntityView(Player player, IPortal portal);
}
