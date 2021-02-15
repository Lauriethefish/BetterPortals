package com.lauriethefish.betterportals.bukkit.entity;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.shared.logging.Logger;

public class EntityTeleportationManagerFactory    {
    private final Logger logger;
    private final IPortalPredicateManager predicateManager;
    private final IPortalClient portalClient;

    @Inject
    public EntityTeleportationManagerFactory(Logger logger, IPortalPredicateManager predicateManager, IPortalClient portalClient) {
        this.logger = logger;
        this.predicateManager = predicateManager;
        this.portalClient = portalClient;
    }

    public IEntityTeleportationManager create(IPortal portal) {
        if(portal.isCrossServer()) {
            return new ExternalEntityTeleportationManager(portal, logger, predicateManager, portalClient);
        }   else    {
            return new LocalEntityTeleportationManager(portal, logger, predicateManager);
        }
    }
}
