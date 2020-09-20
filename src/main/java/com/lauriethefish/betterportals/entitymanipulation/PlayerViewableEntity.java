package com.lauriethefish.betterportals.entitymanipulation;

import java.util.Random;

import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.math.MathUtils;
import com.lauriethefish.betterportals.portal.PortalPos;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

// Stores the current state of a fake entity
public class PlayerViewableEntity {
    public Entity entity; // The entity that this fake entity replicates
    public Object nmsEntity;
    public int entityId;
    
    public PortalPos portal; // Used to find where to entity should appear on this side of the portal
    public EntityEquipmentState equipment; // The equipment that the player current sees on the entity

    // The rotation and location could just be stored together, however this would be annoying when checking for equality
    public Vector location;
    public Vector rotation; // Rotation of the entity in the last tick, used to find if we need to resend the entity look packet
    public byte byteYaw;
    public byte bytePitch;
    public byte byteHeadRotation;
    // Store the location in the previous tick, as this is needed to send relative move packets
    public Vector oldLocation;
    public boolean sleepingLastTick = false;

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

    public boolean calculateLocation() {
        oldLocation = location;
        
        location = portal.moveDestinationToOrigin(PlayerEntityManipulator.getEntityPosition(entity, nmsEntity));
        if(entity instanceof Hanging)   {
            location = MathUtils.round(location);
        }

        return !location.equals(oldLocation);
    }

    public boolean calculateRotation() {
        byte oldHeadRotation = byteHeadRotation;
        Vector oldRotation = rotation;
        rotation = portal.rotateToOrigin(entity.getLocation().getDirection());

        // Dummy location to get the pitch and yaw more easily
        Location loc = entity.getLocation();
        loc.setDirection(rotation);
        byteYaw = (byte) (loc.getYaw() * 256 / 360);
        bytePitch = (byte) (loc.getPitch() * 256 / 360);

        // Find the headRotation as well
        float headRotation = (float) ReflectUtils.runMethod(nmsEntity, "getHeadRotation");
        loc.setYaw(headRotation);
        loc = loc.setDirection(portal.rotateToOrigin(loc.getDirection()));
        byteHeadRotation = (byte) (loc.getYaw() * 256 / 360);

        return !rotation.equals(oldRotation) || byteHeadRotation != oldHeadRotation;
    }

    public void updateEntityEquipment()    {
        if(entity instanceof LivingEntity)  {
            equipment = new EntityEquipmentState(((LivingEntity) entity).getEquipment());
        }
    }
}