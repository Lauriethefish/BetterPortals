package com.lauriethefish.betterportals.entitymanipulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_15_R1.EntityExperienceOrb;
import net.minecraft.server.v1_15_R1.EntityLiving;
import net.minecraft.server.v1_15_R1.EntityPainting;
import net.minecraft.server.v1_15_R1.EnumItemSlot;
import net.minecraft.server.v1_15_R1.Packet;
import net.minecraft.server.v1_15_R1.PacketListenerPlayOut;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityExperienceOrb;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityPainting;
import net.minecraft.server.v1_15_R1.PlayerConnection;

public class PlayerEntityManipulator {
    private BetterPortals pl;
    private PlayerData playerData;

    // Set of the UUIDs of all entities that are hidden
    // A set is used here, since we don't need to preserve order and O(1) is fun
    private Set<UUID> hiddenEntities = new HashSet<UUID>();

    // All of the fake entities that the player can currently see
    private Map<UUID, PlayerViewableEntity> fakeEntities = new HashMap<UUID, PlayerViewableEntity>();

    public PlayerEntityManipulator(BetterPortals pl, PlayerData playerData)    {
        this.pl = pl;
        this.playerData = playerData;
    }

    // Swaps the list of hidden entities with the new one, then
    // hides any entities now in the list, recreates any removed from the list
    // and leaves already hidden entities how they are
    public void swapHiddenEntities(Set<UUID> newHiddenEntites)   {
        for(UUID entityId : newHiddenEntites)    {
            // If the entity is already hidden, and should remain hidden,
            // remove it from the current array and do not hide or show it
            if(hiddenEntities.contains(entityId))   {
                hiddenEntities.remove(entityId);
                continue;
            }

            // If the entity was shown previously, and should now be hidden, hide it
            if(!hiddenEntities.contains(entityId))  {
                hideEntity(entityId);
            }
        }

        // Show the entities corresponding to the entries in the current array,
        // since these are the entities that should not stay hidden
        for(UUID entityId : hiddenEntities)  {
            showEntity(entityId, null);
        }

        hiddenEntities = newHiddenEntites; // Update the array
    }

    // Swaps the list of fake entities with the new one, adding or removing any new entities
    public void swapFakeEntities(Set<UUID> newFakeEntities, Vector locationOffset)  {
        // Loop through all of the existing fake entities and remove any that will no longer be visible to the player
        Iterator<UUID> removingIterator = fakeEntities.keySet().iterator();
        while(removingIterator.hasNext())   {
            // Get then next id, check if it is still visible in the new entities
            UUID id = removingIterator.next();
            if(!newFakeEntities.contains(id))    {
                // If not, send an entity destroy packet, then remove the entity
                hideEntity(id);
                removingIterator.remove();
            }
        }

        // Loop through all the new entities
        for(UUID id : newFakeEntities)  {
            // If the current entity does not exist in the list
            if(!fakeEntities.containsKey(id))   {
                // Make a new PlayerViewableEntity instance from the entity, then send the packets to show it
                PlayerViewableEntity newEntity = new PlayerViewableEntity(pl.getServer().getEntity(id), locationOffset);
                showEntity(id, newEntity.location);
                fakeEntities.put(id, newEntity); // Add the entity to the list
            }
        }
    }

    // Hides the given entity, adding it to the set
    public void addHiddenEntity(UUID entityId)  {
        hiddenEntities.add(entityId);
        hideEntity(entityId);
    }

    // Sends a packet to hide the entity with the given ID
    private void hideEntity(UUID entityId)   {
        // Create a packet to destroy the entity
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(pl.getServer().getEntity(entityId).getEntityId());
        ((CraftPlayer) playerData.player).getHandle().playerConnection.sendPacket(packet); // Send it
    }   

    // Sends all of the packets necessary to spawn a fake entity, or respawn a real one after it was removed
    // To disable the location override, set it to null
    private void showEntity(UUID entityId, Vector locationOverride)   {
        // Get the entity from its UUID
        Entity entity = pl.getServer().getEntity(entityId);
        net.minecraft.server.v1_15_R1.Entity nmsEntity = ((CraftEntity) entity).getHandle();

        // Get the NMS PlayerConnection
        PlayerConnection connection = ((CraftPlayer) playerData.player).getHandle().playerConnection;

        // Create the correct type of spawn packet, depending on the entity
        Packet<PacketListenerPlayOut> spawnPacket;
        if(entity instanceof Painting)  {
            spawnPacket = new PacketPlayOutSpawnEntityPainting((EntityPainting) nmsEntity);
        }   else if(entity instanceof ExperienceOrb)    {
            spawnPacket = new PacketPlayOutSpawnEntityExperienceOrb((EntityExperienceOrb) nmsEntity);
        }   else if(entity instanceof LivingEntity) {
            spawnPacket = new PacketPlayOutSpawnEntityLiving((EntityLiving) nmsEntity);
        }   else    {
            spawnPacket = new PacketPlayOutSpawnEntity(nmsEntity);
        }
        connection.sendPacket(spawnPacket); // Send the entity spawn packet

        // Teleport the entity to the correct location if a location override is used
        // Reflection is used to change some private values in the packet
        if(locationOverride != null)    {
            // Make a new teleport packet
            PacketPlayOutEntityTeleport teleportPacket = generateTeleportPacket(entity, locationOverride);
            connection.sendPacket(teleportPacket);
        }

        // If the entity is living, we have to set its armor and hand/offhand
        if(entity instanceof LivingEntity)   {
            sendEntityEquipmentPackets((LivingEntity) entity);
        }
    }
    
    // Uses reflection to change the private x, y and z values of a teleport packet
    private PacketPlayOutEntityTeleport generateTeleportPacket(Entity entity, Vector location)    {
        // Create a teleport packet
        PacketPlayOutEntityTeleport teleportPacket = new PacketPlayOutEntityTeleport(((CraftEntity) entity).getHandle());
        try {
            // Write the private fields
            FieldUtils.writeField(teleportPacket, "b", location.getX());
            FieldUtils.writeField(teleportPacket, "c", location.getY());
            FieldUtils.writeField(teleportPacket, "d", location.getZ());
        }   catch(IllegalAccessException e)   {
            e.printStackTrace();
        }

        return teleportPacket;
    }

    // Loops through all the fake entities and updates their position and equipment
    public void updateFakeEntities()   {
        // Get the player's connection to the server
        PlayerConnection connection = ((CraftPlayer) playerData.player).getHandle().playerConnection;

        for(PlayerViewableEntity playerEntity : fakeEntities.values()) {
            // First store the old location
            Vector oldLocation = playerEntity.location;
            if(oldLocation != null) {
                if(!oldLocation.equals(playerEntity.location))   {
                    PacketPlayOutEntityTeleport packet = generateTeleportPacket(playerEntity.entity, playerEntity.location);
                    connection.sendPacket(packet);
                }
            }

            // Update the entity equipment, then send EntityEquipment packets to the player if required
            EntityEquipmentState oldEntityEquipment = playerEntity.equipment;
            playerEntity.updateEntityEquipment();
            if(!oldEntityEquipment.equals(playerEntity.equipment))  {
                sendEntityEquipmentPackets((LivingEntity) playerEntity.entity);
            }

        }
    }

    // Sends the 6 entity equipment packets required to change the items in the
    // entities armor slots and hands
    private void sendEntityEquipmentPackets(LivingEntity entity)    {
        EntityEquipment equipment = entity.getEquipment(); // Get all of the equipment slots from the entity
        if(equipment == null)   { // Null check for the equipment, since not all living entities can equip armor
            return;
        }

        int entityId = entity.getEntityId(); // Get the entity ID, since this is what the packets use

        // Array to store our six packets
        PacketPlayOutEntityEquipment[] packets = new PacketPlayOutEntityEquipment[6];
        // Make all of our entity equipment packets for each slot
        packets[0] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.MAINHAND, CraftItemStack.asNMSCopy(equipment.getItemInMainHand()));
        packets[1] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.OFFHAND, CraftItemStack.asNMSCopy(equipment.getItemInOffHand()));
        packets[2] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.FEET, CraftItemStack.asNMSCopy(equipment.getBoots()));
        packets[3] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.LEGS, CraftItemStack.asNMSCopy(equipment.getLeggings()));
        packets[4] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.CHEST, CraftItemStack.asNMSCopy(equipment.getChestplate()));
        packets[5] = new PacketPlayOutEntityEquipment(entityId, EnumItemSlot.HEAD, CraftItemStack.asNMSCopy(equipment.getHelmet()));

        // Get the NMS PlayerConnection
        PlayerConnection connection = ((CraftPlayer) playerData.player).getHandle().playerConnection;
        for(PacketPlayOutEntityEquipment packet : packets)  {
            connection.sendPacket(packet); // Send each packet
        }
    }
}