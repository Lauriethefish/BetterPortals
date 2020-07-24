package com.lauriethefish.betterportals.entitymanipulation;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

// Stores the current state of a fake entity that the player can see
public class PlayerViewableEntity {
    public Entity entity; // The entity that this fake entity replicates
    
    // The location that the player currently sees the entity
    public Vector location;
    public Vector locationOffset; // Difference between the location of the real entity and the replicated one
    public EntityEquipmentState equipment; // The equipment that the player current sees on the entity

    public PlayerViewableEntity(Entity entity, Vector locationOffset)   {
        this.locationOffset = locationOffset;
        calculateLocation();
        updateEntityEquipment();
    }

    public void calculateLocation() {
        this.location = entity.getLocation().toVector().add(locationOffset);
    }

    public void updateEntityEquipment()    {
        if(entity instanceof LivingEntity)  {
            equipment = new EntityEquipmentState(((LivingEntity) entity).getEquipment());
        }
    }
}