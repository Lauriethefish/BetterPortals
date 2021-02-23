package com.lauriethefish.betterportals.bukkit.portal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

@Singleton
public class PortalManager implements IPortalManager    {
    private final Logger logger;
    private final IPortalPredicateManager predicateManager;
    private final IPortalActivityManager portalActivityManager;

    // Multiple portals can have the same origin position
    private final Map<Location, Set<IPortal>> portals = new HashMap<>();
    private final Map<UUID, IPortal> portalsById = new HashMap<>();

    @Inject
    public PortalManager(Logger logger, IPortalPredicateManager predicateManager, IPortalActivityManager portalActivityManager) {
        this.logger = logger;
        this.predicateManager = predicateManager;
        this.portalActivityManager = portalActivityManager;
    }

    @Override
    public Collection<IPortal> getAllPortals() {
        return portalsById.values();
    }

    @Override
    public Collection<IPortal> getPortalsAt(Location originLoc) {
        Set<IPortal> portalsAtLoc = portals.get(originLoc);
        return portalsAtLoc == null ? Collections.emptyList() : portalsAtLoc;
    }

    @Override
    public IPortal getPortalById(@Nullable UUID id) {
        return portalsById.get(id);
    }

    @Override
    public IPortal findClosestPortal(@NotNull Location position, double maximumDistance, Predicate<IPortal> predicate) {
        IPortal currentClosest = null;
        double currentClosestDistance = maximumDistance;
        for(Map.Entry<Location, Set<IPortal>> entry : portals.entrySet()) {

            Location portalPos = entry.getKey();
            // Avoid throwing an exception when portals not in this world are checked
            if(portalPos.getWorld() != position.getWorld()) {continue;}
            double distance = portalPos.distance(position);

            if(distance >= currentClosestDistance) {continue;}

            // Check to see if any portals here match the predicate
            for(IPortal portal : entry.getValue()) {
                if(!predicate.test(portal)) {continue;}

                currentClosest = portal;
                currentClosestDistance = distance;
                break;
            }
        }

        return currentClosest;
    }

    @Override
    public @NotNull Collection<IPortal> findActivatablePortals(@NotNull Player player) {
        List<IPortal> result = new ArrayList<>();

        for(Set<IPortal> portalSet : portals.values()) {
            for(IPortal portal : portalSet) {
                // Test that the portal passes all predicates for activation
                if(predicateManager.isActivatable(portal, player)) {
                    result.add(portal);
                }
            }
        }

        return result;
    }

    @Override
    public void registerPortal(@NotNull IPortal portal) {
        logger.fine("Registering portal with origin position %s", portal.getOriginPos());

        // Add a new portal array if one doesn't already exist for this location
        Location originLoc = portal.getOriginPos().getLocation();
        if(!portals.containsKey(originLoc)) {
            portals.put(originLoc, new HashSet<>());
        }
        portalsById.put(portal.getId(), portal);

        portals.get(originLoc).add(portal);
    }

    @Override
    public int removePortalsAt(@NotNull Location originLoc) {
        Set<IPortal> portalsRemoved = portals.remove(originLoc);
        if(portalsRemoved == null) {return 0;}

        // Make sure to also remove them from the ID map
        for(IPortal portal : portalsRemoved) {
            portalsById.remove(portal.getId());
        }

        logger.fine("Unregistering %d portal(s) at position %s", portalsRemoved.size(), originLoc);
        return portalsRemoved.size();
    }

    @Override
    public boolean removePortal(@NotNull IPortal portal) {
        logger.fine("Unregistering portal at position %s", portal.getOriginPos().getLocation());

        Set<IPortal> portalsAtLoc = portals.get(portal.getOriginPos().getLocation());
        if(portalsAtLoc == null) {return false;}

        boolean wasRemoved = portalsAtLoc.remove(portal);
        // Remove the portal array if there are no longer any portals at this location
        if(portalsAtLoc.size() == 0) {
            portals.remove(portal.getOriginPos().getLocation());
        }
        portalsById.remove(portal.getId());
        return wasRemoved;
    }

    @Override
    public boolean removePortalById(@NotNull UUID id) {
        IPortal removed = portalsById.remove(id);
        if(removed == null) {return false;}
        removePortal(removed); // Also remove it in the location map

        return true;
    }

    @Override
    public void onReload() {
        portalActivityManager.resetActivity();
    }
}
