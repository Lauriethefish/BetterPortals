package com.lauriethefish.betterportals.bukkit.portal.predicate;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Used to allow/disallow viewing/using/activating portals depending on various factors.
 */
public interface PortalPredicate {
    boolean test(@NotNull IPortal portal, @NotNull Player player);
}
