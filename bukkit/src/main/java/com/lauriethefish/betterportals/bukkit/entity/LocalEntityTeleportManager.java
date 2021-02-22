package com.lauriethefish.betterportals.bukkit.entity;

import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformations;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class LocalEntityTeleportManager implements IEntityTeleportManager {
    private final IPortal portal;
    private final Logger logger;
    private final IPortalPredicateManager predicateManager;

    public LocalEntityTeleportManager(IPortal portal, Logger logger, IPortalPredicateManager predicateManager) {
        this.portal = portal;
        this.logger = logger;
        this.predicateManager = predicateManager;
    }

    private final Map<Entity, Location> lastEntityPositions = new HashMap<>();

    @Override
    public void update() {
        removeNonExistingEntities();

        // Check each entity at the origin to see if it teleported
        for(Entity entity : portal.getEntityList().getOriginEntities()) {
            Location lastPosition = lastEntityPositions.get(entity);
            Location currentPosition = entity.getLocation();

            // Use an intersection check to see if it moved through the portal
            if(lastPosition != null) {
                boolean didWalkThroughPortal = portal.getTransformations()
                        .createIntersectionChecker(lastPosition.toVector())
                        .checkIfIntersects(currentPosition.toVector());


                if(didWalkThroughPortal && checkCanTeleport(entity)) {
                    teleportEntity(entity);
                    continue;
                }
            }

            lastEntityPositions.put(entity, currentPosition);
        }
    }

    /**
     * Verifies that <code>entity</code> can teleport using {@link IPortalPredicateManager}
     * @param entity Entity to check
     * @return Whether it can teleport
     */
    private boolean checkCanTeleport(Entity entity) {
        // Enforce teleportation predicates
        if(entity instanceof Player) {
            return predicateManager.canTeleport(portal, (Player) entity);
        }   else    {
            return true;
        }
    }

    private void removeNonExistingEntities() {
        lastEntityPositions.keySet().removeIf(entity -> !portal.getEntityList().getOriginEntities().contains(entity));
    }

    /**
     * Limits the coordinates of <code>preferred</code> to avoid spawning players on top of portals when they're slightly inside the block hitbox
     * @param preferred Position to limit to the hitbox
     * @return The spawn position, or <code>preferred</code> if none was found.
     */
    private @NotNull Location limitToBlockHitbox(@NotNull Location preferred) {
        Location flooredPos = MathUtil.floor(preferred);
        Location blockOffset = preferred.clone().subtract(flooredPos);

        if(blockOffset.getZ() > 0.6 && preferred.clone().add(0.0, 0.0, 1.0).getBlock().getType().isSolid()) {
            blockOffset.setZ(0.6);
        }
        if(blockOffset.getX() > 0.6 && preferred.clone().add(1.0, 0.0, 0.0).getBlock().getType().isSolid()) {
            blockOffset.setX(0.6);
        }
        if(blockOffset.getZ() < 0.4 && preferred.clone().add(0.0, 0.0, -1.0).getBlock().getType().isSolid()) {
            blockOffset.setZ(0.4);
        }
        if(blockOffset.getX() < 0.4 && preferred.clone().add(-1.0, 0.0, 0.0).getBlock().getType().isSolid()) {
            blockOffset.setX(0.4);
        }
        logger.finer("Fixing position. Floored pos: %s. Block offset: %s", flooredPos.toVector(), blockOffset.toVector());

        return blockOffset.add(flooredPos);
    }

    /**
     * Moves the entity from the origin to the destination of the portal.
     * This also preserves/rotates entity velocity and direction.
     * @param entity The entity to be teleported
     */
    private void teleportEntity(Entity entity) {
        lastEntityPositions.remove(entity);

        PortalTransformations transformations = portal.getTransformations();

        Location destPos;
        destPos = limitToBlockHitbox(transformations.moveToDestination(entity.getLocation()));

        destPos.setDirection(transformations.rotateToDestination(entity.getLocation().getDirection()));

        // Teleporting an entity removes the velocity, so we have to re-add it
        Vector velocity = entity.getVelocity();
        velocity = transformations.rotateToDestination(velocity);

        logger.fine("Teleporting entity with ID %d and of type %s to position %s", entity.getEntityId(), entity.getType(), destPos.toVector());

        entity.teleport(destPos);
        entity.setVelocity(velocity);
    }
}
