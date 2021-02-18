package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
import com.lauriethefish.betterportals.bukkit.util.nms.EntityUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntityTracker implements IEntityTracker    {
    private final Logger logger;
    private final Entity entity;
    @Getter private final EntityInfo entityInfo;
    @Getter private final IPortal portal;
    private final IEntityPacketManipulator packetManipulator;

    private final Set<Player> trackingPlayers = new HashSet<>();

    private final EntityEquipmentWatcher equipmentWatcher;
    private Vector lastPosition;
    private Vector lastDirection;
    private Vector lastVelocity;
    private float lastHeadRotation;

    @Inject
    public EntityTracker(@Assisted Entity entity, @Assisted IPortal portal, IEntityPacketManipulator packetManipulator, Logger logger) {
        // Non-living entities don't have equipment
        this.equipmentWatcher = entity instanceof LivingEntity ? new EntityEquipmentWatcher((LivingEntity) entity) : null;
        this.entity = entity;
        this.portal = portal;
        this.entityInfo = new EntityInfo(portal.getTransformations(), entity);
        this.packetManipulator = packetManipulator;
        this.logger = logger;
    }

    public void update() {
        sendMovementUpdates();

        // Equipment is disabled for living entities
        if(equipmentWatcher != null) {
            Map<EnumWrappers.ItemSlot, ItemStack> equipmentChanges = equipmentWatcher.checkForChanges();
            if(equipmentChanges.size() > 0) {
                packetManipulator.sendEntityEquipment(entityInfo, equipmentChanges, trackingPlayers);
            }
        }

        // The metadata packet contains tons of stuff, e.g. sneaking and beds on newer versions
        packetManipulator.sendMetadata(entityInfo, trackingPlayers);

        packetManipulator.sendEntityHeadRotation(entityInfo, trackingPlayers);

        Vector velocity = entity.getVelocity();
        if(lastVelocity != null && !velocity.equals(lastVelocity)) {
            packetManipulator.sendEntityVelocity(entityInfo, velocity, trackingPlayers);
            lastVelocity = velocity;
        }
    }

    @Override
    public void onAnimation(@NotNull AnimationType animationType) {
        packetManipulator.sendEntityAnimation(entityInfo, trackingPlayers, animationType);
    }

    @Override
    public void onPickup(@NotNull EntityInfo pickedUp) {
        packetManipulator.sendEntityPickupItem(entityInfo, pickedUp, trackingPlayers);
    }

    // Handles sending all movement and looking packets
    private void sendMovementUpdates() {
        Vector currentPosition = entity.getLocation().toVector();
        Vector currentDirection = EntityUtil.getActualEntityDirection(entity);

        boolean positionChanged = lastPosition != null && !currentPosition.equals(lastPosition);
        boolean rotationChanged = lastDirection != null && !currentDirection.equals(lastDirection);
        Vector posOffset = lastPosition == null ? new Vector() : currentPosition.clone().subtract(lastPosition);

        lastPosition = currentPosition;
        lastDirection = currentDirection;

        // Relative move packets have a limit of 8 blocks before we have to just send a teleport packet
        boolean canUseRelativeMove = posOffset.getX() < 8 && posOffset.getY() < 8 && posOffset.getZ() < 8;
        // We must combine the move and look to avoid issues on newer versions
        if (positionChanged && !canUseRelativeMove) {
            packetManipulator.sendEntityTeleport(entityInfo, trackingPlayers);
        } else if (positionChanged && rotationChanged) {
            packetManipulator.sendEntityMoveLook(entityInfo, posOffset, trackingPlayers);
        } else if (positionChanged) {
            packetManipulator.sendEntityMove(entityInfo, posOffset, trackingPlayers);
        } else if (rotationChanged) {
            packetManipulator.sendEntityLook(entityInfo, trackingPlayers);
        }

        // Bukkit uses the yaw as the head rotation for some reason, so we do it with that
        float headRotation = entity.getLocation().getYaw();
        if(lastHeadRotation != headRotation) {
            lastHeadRotation = headRotation;
            packetManipulator.sendEntityHeadRotation(entityInfo, trackingPlayers);
        }
    }

    public void addTracking(@NotNull Player player) {
        if(trackingPlayers.contains(player)) {throw new IllegalArgumentException("Player is already tracking this entity");}

        trackingPlayers.add(player);
        packetManipulator.showEntity(entityInfo, player);
    }

    public void removeTracking(@NotNull Player player, boolean sendPackets) {
        if(!trackingPlayers.contains(player)) {throw new IllegalArgumentException("Cannot stop player from tracking entity, they weren't viewing in the first place");}

        trackingPlayers.remove(player);
        if(sendPackets) {
            packetManipulator.hideEntity(entityInfo, player);
        }
    }

    public int getTrackingPlayerCount() {
        return trackingPlayers.size();
    }
}
