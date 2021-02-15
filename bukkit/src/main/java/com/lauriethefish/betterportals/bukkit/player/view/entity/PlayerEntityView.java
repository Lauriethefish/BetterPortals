package com.lauriethefish.betterportals.bukkit.player.view.entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.entity.faking.EntityInfo;
import com.lauriethefish.betterportals.bukkit.entity.faking.IEntityPacketManipulator;
import com.lauriethefish.betterportals.bukkit.entity.faking.IEntityTrackingManager;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerEntityView implements IPlayerEntityView  {
    private final Logger logger;
    private final IPortal portal;
    private final Player player;
    private final IEntityPacketManipulator packetManipulator;
    private final IEntityTrackingManager trackingManager;
    private final Map<Entity, EntityInfo> hiddenEntities = new HashMap<>();
    private final Set<Entity> replicatedEntities = new HashSet<>();

    @Inject
    public PlayerEntityView(@Assisted IPortal portal, @Assisted Player player, IEntityPacketManipulator packetManipulator, Logger logger, IEntityTrackingManager trackingManager) {
        this.portal = portal;
        this.player = player;
        this.packetManipulator = packetManipulator;
        this.logger = logger;
        this.trackingManager = trackingManager;
    }

    @Override
    public void update() {
        updateHiddenEntities();
        updateReplicatedEntities();
    }

    private void updateHiddenEntities() {
        PlaneIntersectionChecker intersectionChecker = portal.getTransformations().createIntersectionChecker(player.getEyeLocation().toVector());

        Set<Entity> nowHidden = new HashSet<>();
        for(Entity entity : portal.getEntityList().getOriginEntities()) {
            // If the line from the player's position to the entity intersects the portal, then hide it since it'll spoil the effect by appearing in front of the blocks
            boolean shouldBeHidden = intersectionChecker.checkIfIntersects(entity.getLocation().toVector());
            if(!shouldBeHidden) {continue;}

            nowHidden.add(entity);
            if(!hiddenEntities.containsKey(entity)) {
                hide(entity);
            }
        }

        hiddenEntities.entrySet().removeIf(entry -> {
            boolean isHidden = nowHidden.contains(entry.getKey());
            // Reshow entities that are no longer hidden and that still exist
            if(!isHidden && entry.getKey().isValid()) {
                packetManipulator.showEntity(entry.getValue(), player);
            }
            return !isHidden;
        });
    }

    private void updateReplicatedEntities() {
        PlaneIntersectionChecker intersectionChecker = portal.getTransformations().createIntersectionChecker(player.getEyeLocation().toVector());

        // Start tracking newly replicated entities
        Set<Entity> nowReplicated = new HashSet<>();
        for(Entity entity : portal.getEntityList().getDestinationEntities()) {
            Location originPos = portal.getTransformations().moveToOrigin(entity.getLocation());

            boolean shouldBeReplicated = intersectionChecker.checkIfIntersects(originPos.toVector());
            if(!shouldBeReplicated) {continue;}
            nowReplicated.add(entity);

            // Only set it to be tracking if it wasn't previously
            if(!replicatedEntities.contains(entity)) {
                replicatedEntities.add(entity);
                trackingManager.setTracking(entity, portal, player);
            }
        }

        // Stop tracking entities that are no longer visible through the portal
        replicatedEntities.removeIf(entity -> {
            boolean isReplicated = nowReplicated.contains(entity);

            if(!isReplicated) {
                trackingManager.setNoLongerTracking(entity, portal, player, true);
            }
            return !isReplicated;
        });
    }

    // Send packets to remove the entity from the player's view
    private void hide(Entity entity) {
        EntityInfo entityInfo = new EntityInfo(entity);
        packetManipulator.hideEntity(entityInfo, player);
        hiddenEntities.put(entity, entityInfo);
    }

    @Override
    public void onDeactivate(boolean shouldResetEntities) {
        if(shouldResetEntities) {
            hiddenEntities.forEach((entity, entityInfo) -> packetManipulator.showEntity(entityInfo, player));
        }
        replicatedEntities.forEach(entity -> trackingManager.setNoLongerTracking(entity, portal, player, shouldResetEntities));
    }
}
