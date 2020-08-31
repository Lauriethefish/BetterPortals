package com.lauriethefish.betterportals.entitymanipulation;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

// Stores the current state of a fake entity that the player can see
public class PlayerViewableEntity {
    public Entity entity; // The entity that this fake entity replicates
    public Object nmsEntity;
    public int entityId;
    
    // The location that the player currently sees the entity
    public Vector location;
    public Vector locationOffset; // Difference between the location of the real entity and the replicated one
    public EntityEquipmentState equipment; // The equipment that the player current sees on the entity

    public Vector rotation; // Rotation of the entity in the last tick, used to find if we need to resend the entity look packet

    // The rotation and location could just be stored together, however this would be annoying when checking for equality

    public PlayerViewableEntity(Entity entity, Vector locationOffset)   {
        this.entity = entity;
        this.locationOffset = locationOffset;
        // Find the nms entity and its id here to avoid doing it multiple times
        this.nmsEntity = ReflectUtils.runMethod(entity, "getHandle");
        this.entityId = (int) ReflectUtils.runMethod(nmsEntity, "getId");

        calculateLocation();
        calculateRotation();
        updateEntityEquipment();
    }

    public void calculateLocation() {
        this.location = entity.getLocation().toVector().add(locationOffset);
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