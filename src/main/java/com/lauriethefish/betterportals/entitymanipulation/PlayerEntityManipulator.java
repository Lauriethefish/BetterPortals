package com.lauriethefish.betterportals.entitymanipulation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerEntityManipulator {
    private BetterPortals pl;
    private Object playerConnection; // Store the NMS connection update to increase speed of sending packets

    // Set of the UUIDs of all entities that are hidden
    // A set is used here, since we don't need to preserve order and O(1) is fun
    private Set<UUID> hiddenEntities = new HashSet<UUID>();

    // All of the fake entities that the player can currently see
    private Map<UUID, PlayerViewableEntity> fakeEntities = new HashMap<UUID, PlayerViewableEntity>();

    public PlayerEntityManipulator(BetterPortals pl, PlayerData playerData)    {
        this.pl = pl;
        // Find the NMS connection using reflection
        Object craftPlayer = ReflectUtils.runMethod(playerData.player, "getHandle");
        playerConnection = ReflectUtils.getField(craftPlayer, "playerConnection");
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
    private void hideEntity(UUID entityUUID)   {
        int entityId = pl.getServer().getEntity(entityUUID).getEntityId();
        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityDestroy", new Class[]{int.class}, new Object[entityId]);
        sendPacket(packet);
    }   

    // Sends all of the packets necessary to spawn a fake entity, or respawn a real one after it was removed
    // To disable the location override, set it to null
    private void showEntity(UUID entityId, Vector locationOverride)   {
        // Get the entity from its UUID
        Entity entity = pl.getServer().getEntity(entityId);
        Object nmsEntity = ReflectUtils.runMethod(entity, "getHandle");

        // Create the correct type of spawn packet, depending on the entity
        Object spawnPacket;
        if(entity instanceof Painting)  {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityPainting", new Class[]{ReflectUtils.getMcClass("EntityPainting")}, new Object[]{nmsEntity});
        }   else if(entity instanceof ExperienceOrb)    {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityExperienceOrb", new Class[]{ReflectUtils.getMcClass("EntityExperienceOrb")}, new Object[]{nmsEntity});
        }   else if(entity instanceof LivingEntity) {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityLiving", new Class[]{ReflectUtils.getMcClass("EntityLiving")}, new Object[]{nmsEntity});
        }   else    {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntity", new Class[]{ReflectUtils.getMcClass("Entity")}, new Object[]{nmsEntity});
        }
        sendPacket(spawnPacket);

        // Teleport the entity to the correct location if a location override is used
        // Reflection is used to change some private values in the packet
        if(locationOverride != null)    {
            // Make a new teleport packet
            Object teleportPacket = generateTeleportPacket(nmsEntity, locationOverride);
            sendPacket(teleportPacket);
        }

        // If the entity is living, we have to set its armor and hand/offhand
        if(entity instanceof LivingEntity)   {
            sendEntityEquipmentPackets((LivingEntity) entity);
        }
    }
    
    // Uses reflection to change the private x, y and z values of a teleport packet
    private Object generateTeleportPacket(Object nmsEntity, Vector location)    {
        // Create a teleport packet
        Object teleportPacket = ReflectUtils.newInstance("PacketPlayOutEntityTeleport", new Class[]{ReflectUtils.getMcClass("Entity")}, new Object[]{nmsEntity});
        // Write the private fields
        ReflectUtils.setField(teleportPacket, "b", location.getX());
        ReflectUtils.setField(teleportPacket, "c", location.getY());
        ReflectUtils.setField(teleportPacket, "d", location.getZ());
        return teleportPacket;
    }

    // Loops through all the fake entities and updates their position and equipment
    public void updateFakeEntities()   {
        for(PlayerViewableEntity playerEntity : fakeEntities.values()) {
            // First store the old location
            Vector oldLocation = playerEntity.location;
            if(oldLocation != null) {
                if(!oldLocation.equals(playerEntity.location))   {
                    Object packet = generateTeleportPacket(playerEntity.entity, playerEntity.location);
                    sendPacket(packet);
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

    // Generates and sends an entity equipment packet
    private void sendEquipmentPacket(int entityId, String slot, ItemStack item) {
        // Copy the ItemStack into the NMS object
        Object nmsItem = ReflectUtils.runMethod(null, ReflectUtils.getMcClass("CraftItemStack"), "asNMSCopy", new Class[]{ItemStack.class}, new Object[]{item});
        // Use the valueOf method to get the nms enum
        Object nmsSlot = ReflectUtils.runMethod(null, ReflectUtils.getMcClass("EnumItemSlot"), "valueOf", new Class[]{String.class}, new Object[]{slot});
        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityEquipment", new Class[]{
            int.class, ReflectUtils.getMcClass("EnumItemSlot"), ReflectUtils.getMcClass("ItemStack")
        }, new Object[] {entityId, nmsSlot, nmsItem});

        sendPacket(packet);
    }

    // Sends the 6 entity equipment packets required to change the items in the
    // entities armor slots and hands
    private void sendEntityEquipmentPackets(LivingEntity entity)    {
        EntityEquipment equipment = entity.getEquipment(); // Get all of the equipment slots from the entity
        if(equipment == null)   { // Null check for the equipment, since not all living entities can equip armor
            return;
        }

        int entityId = entity.getEntityId(); // Get the entity ID, since this is what the packets use
        // Send a packet for each slot
        sendEquipmentPacket(entityId, "MAINHAND", equipment.getItemInMainHand());
        sendEquipmentPacket(entityId, "OFFHAND", equipment.getItemInOffHand());
        sendEquipmentPacket(entityId, "FEET", equipment.getBoots());
        sendEquipmentPacket(entityId, "LEGS", equipment.getLeggings());
        sendEquipmentPacket(entityId, "CHEST", equipment.getChestplate());
        sendEquipmentPacket(entityId, "HEAD", equipment.getHelmet());
    }

    private void sendPacket(Object packet)  {
        ReflectUtils.runMethod(playerConnection, "sendPacket", new Class[]{ReflectUtils.getMcClass("Packet")}, new Object[]{packet});
    }
}