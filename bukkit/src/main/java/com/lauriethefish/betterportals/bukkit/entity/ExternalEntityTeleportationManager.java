package com.lauriethefish.betterportals.bukkit.entity;

import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.RequestException;
import com.lauriethefish.betterportals.shared.net.requests.TeleportRequest;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends requests to the proxy for teleporting players across servers.
 * Entities are not currently supported, since teleporting them and keeping all of their properties is very difficult.
 */
public class ExternalEntityTeleportationManager implements IEntityTeleportationManager  {
    private final IPortal portal;
    private final Logger logger;
    private final IPortalPredicateManager predicateManager;
    private final Map<Player, Location> lastPlayerPositions = new HashMap<>();
    private final IPortalClient portalClient;

    public ExternalEntityTeleportationManager(IPortal portal, Logger logger, IPortalPredicateManager predicateManager, IPortalClient portalClient) {
        this.portal = portal;
        this.logger = logger;
        this.predicateManager = predicateManager;
        this.portalClient = portalClient;
    }

    @Override
    public void update() {
        for(Entity entity : portal.getEntityList().getOriginEntities()) {
            if(!(entity instanceof Player)) {continue;}
            Player player = (Player) entity;

            Location lastPosition = lastPlayerPositions.get(entity);
            Location currentPosition = entity.getLocation();

            // Use an intersection check to see if it moved through the portal
            if(lastPosition != null) {
                boolean didWalkThroughPortal = portal.getTransformations()
                        .createIntersectionChecker(lastPosition.toVector())
                        .checkIfIntersects(currentPosition.toVector());


                if(didWalkThroughPortal && predicateManager.canTeleport(portal, player)) {
                    sendTeleportRequest(player);
                }
            }

            lastPlayerPositions.put(player, currentPosition);
        }

        lastPlayerPositions.keySet().removeIf((player) -> !portal.getEntityList().getOriginEntities().contains(player));
    }

    private void sendTeleportRequest(Player player) {
        Location destPosition = portal.getTransformations().moveToDestination(player.getLocation());
        destPosition.setDirection(portal.getTransformations().rotateToDestination(player.getLocation().getDirection()));
        Vector destVelocity = portal.getTransformations().rotateToDestination(player.getVelocity());


        TeleportRequest request = new TeleportRequest();
        request.setDestWorldId(portal.getDestPos().getWorldId());
        request.setDestWorldName(portal.getDestPos().getWorldName());
        request.setDestServer(portal.getDestPos().getServerName());
        request.setPlayerId(player.getUniqueId());

        request.setDestX(destPosition.getX());
        request.setDestY(destPosition.getY());
        request.setDestZ(destPosition.getZ());
        request.setDestVelX(destVelocity.getX());
        request.setDestVelY(destVelocity.getY());
        request.setDestVelZ(destVelocity.getZ());

        request.setFlying(player.isFlying());
        request.setGliding(player.isGliding());

        request.setDestPitch(destPosition.getPitch());
        request.setDestYaw(destPosition.getYaw());

        portalClient.sendRequestToProxy(request, (response) -> {
            try {
                response.checkForErrors();
            }   catch(RequestException ex) {
                logger.warning("An error occurred while attempting to teleport a player across servers");
                ex.printStackTrace();
            }
        });
    }
}
