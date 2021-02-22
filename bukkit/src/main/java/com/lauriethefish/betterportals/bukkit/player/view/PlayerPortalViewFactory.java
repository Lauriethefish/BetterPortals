package com.lauriethefish.betterportals.bukkit.player.view;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Player;

public interface PlayerPortalViewFactory {
    IPlayerPortalView create(Player player, IPortal viewedPortal);
}
