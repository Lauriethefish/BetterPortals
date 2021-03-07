package com.lauriethefish.betterportals.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Used to allow/disallow viewing/using/activating portals depending on various factors.
 */
public interface PortalPredicate {
    boolean test(@NotNull BetterPortal portal, @NotNull Player player);
}
