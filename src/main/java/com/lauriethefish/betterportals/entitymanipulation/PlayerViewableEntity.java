package com.lauriethefish.betterportals.entitymanipulation;

import java.util.Random;

import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.portal.PortalPos;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

// Stores the current state of a fake entity
public class PlayerViewableEntity {
    public Entity entity; // The entity that this fake entity replicates
    public Object nmsEntity;
    public int entityId;
    
    // The location that the player currently sees the entity
    public Vector location;
    public PortalPos portal; // Used to find where to entity should appear on this side of the portal
    public EntityEquipmentState equipment; // The equipment that the player current sees on the entity

    public Vector rotation; // Rotation of the entity in the last tick, used to find if we need to resend the entity look packet

    // The rotation and location could just be stored together, however this would be annoying when checking for equality

    public PlayerViewableEntity(Entity entity, PortalPos portal, Random random)   {
        this.entity = entity;
        this.portal = portal;
        // Find the nms entity and its id here to avoid doing it multiple times
        this.nmsEntity = ReflectUtils.runMethod(entity, "getHandle");
        // Generate a random entityId, since otherwise, the real entity with the same ID may be moved instead of the fake one
        this.entityId = random.nextInt(Integer.MAX_VALUE);

        calculateLocation();
        calculateRotation();
        updateEntityEquipment();
    }

    public void calculateLocation() {
        this.location = portal.moveDestinationToOrigin(PlayerEntityManipulator.getEntityPosition(entity, nmsEntity));
    }

    public void calculateRotation() {
        this.rotation = entity.getLocation().getDirection();
    }

    public void updateEntityEquipment()    {
        if(entity instanceof LivingEntity)  {
            equipment = new EntityEquipmentState(((LivingEntity) entity).getEquipment());
        }
    }
}