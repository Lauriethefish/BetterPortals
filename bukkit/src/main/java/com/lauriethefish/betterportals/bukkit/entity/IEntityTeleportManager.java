package com.lauriethefish.betterportals.bukkit.entity;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public interface IEntityTeleportManager {
    /**
     * Called every tick while the portal is active.
     * Teleports entities that walk through the portal
     */
    void update();

    class Factory {
        private final Logger logger;
        private final IPortalPredicateManager predicateManager;
        private final IPortalClient portalClient;
        private final JavaPlugin pl;

        @Inject
        public Factory(Logger logger, IPortalPredicateManager predicateManager, IPortalClient portalClient, JavaPlugin pl) {
            this.logger = logger;
            this.predicateManager = predicateManager;
            this.portalClient = portalClient;
            this.pl = pl;
        }

        public IEntityTeleportManager create(IPortal portal) {
            if(portal.isCrossServer()) {
                return new ExternalEntityTeleportManager(portal, logger, pl, predicateManager, portalClient);
            }   else    {
                return new LocalEntityTeleportManager(portal, logger, predicateManager);
            }
        }
    }
}
