package com.lauriethefish.betterportals.bukkit.portal.predicate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.api.BetterPortal;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Makes sure that portals outside of the configured activation distance can't be activated
 */
@Singleton
public class ActivationDistance implements PortalPredicate {
    private final MiscConfig miscConfig;

    @Inject
    public ActivationDistance(MiscConfig miscConfig) {
        this.miscConfig = miscConfig;
    }

    @Override
    public boolean test(@NotNull BetterPortal portal, @NotNull Player player) {
        Location portalOrigin = portal.getOriginPos().getLocation();
        Location playerPos = player.getLocation();
        if(portalOrigin.getWorld() != playerPos.getWorld()) {return false;} // Portals in other worlds are never viewable

        return playerPos.distance(portalOrigin) < miscConfig.getPortalActivationDistance();
    }
}
