package com.lauriethefish.betterportals.bukkit.portal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class PortalActivityManager implements IPortalActivityManager    {
    private final Logger logger;

    private final Set<IPortal> activePortals = new HashSet<>();
    private final Set<IPortal> activePortalsYetToUpdate = new HashSet<>();

    private final Set<IPortal> viewedPortals = new HashSet<>();
    private final Set<IPortal> viewActivePortalsYetToUpdate = new HashSet<>();

    @Inject
    public PortalActivityManager(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void onPortalActivatedThisTick(IPortal portal) {
        // If the portal is being activated and has not yet been added to the active list, activate it
        if(!activePortals.contains(portal)) {
            portal.onActivate();
            activePortals.add(portal);
            activePortalsYetToUpdate.add(portal);
        }

        // Make sure to only update the portal once per-tick
        if(activePortalsYetToUpdate.remove(portal)) {
            portal.onUpdate();
        }
    }

    @Override
    public void onPortalViewedThisTick(IPortal portal) {
        // If the portal is being viewed and has not yet been added to the viewed list, add it
        if(!viewedPortals.contains(portal)) {
            portal.onViewActivate();
            viewedPortals.add(portal);
            viewActivePortalsYetToUpdate.add(portal);
        }

        // Make sure to only update the portal once per-tick
        if(viewActivePortalsYetToUpdate.remove(portal)) {
            portal.onViewUpdate();
        }
    }

    @Override
    public void postUpdate() {
        // Call to view-deactivate any portals no longer viewed by the player
        for(IPortal portal : viewActivePortalsYetToUpdate) {
            viewedPortals.remove(portal);
            portal.onViewDeactivate();
        }

        viewActivePortalsYetToUpdate.clear();
        viewActivePortalsYetToUpdate.addAll(viewedPortals);

        // Deactivate active portals that weren't used
        for(IPortal portal : activePortalsYetToUpdate) {
            activePortals.remove(portal);
            portal.onDeactivate();
        }

        // Prepare the portals yet to update for the next tick
        activePortalsYetToUpdate.clear();
        activePortalsYetToUpdate.addAll(activePortals);
    }
}
