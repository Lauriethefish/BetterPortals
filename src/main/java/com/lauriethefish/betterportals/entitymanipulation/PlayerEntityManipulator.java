package com.lauriethefish.betterportals.entitymanipulation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerEntityManipulator {
    private Object playerConnection; // Store the NMS connection update to increase speed of sending packets

    // Set of all hidden entities
    // A set is used here, since we don't need to preserve order and O(1) is fun
    private Set<Entity> hiddenEntities = new HashSet<Entity>();

    // All of the fake entities that the player can currently see
    private Map<Entity, PlayerViewableEntity> replicatedEntites = new HashMap<Entity, PlayerViewableEntity>();

    public PlayerEntityManipulator(PlayerData playerData)    {
        // Find the NMS connection using reflection
        Object craftPlayer = ReflectUtils.runMethod(playerData.player, "getHandle");
        playerConnection = ReflectUtils.getField(craftPlayer, "playerConnection");
    }

    // Swaps the list of hidden entities with the new one, then
    // hides any entities now in the list, recreates any removed from the list
    // and leaves already hidden entities how they are
    public void swapHiddenEntities(Set<Entity> newHiddenEntites)   {
        for(Entity entity : newHiddenEntites)    {
            // If the entity is already hidden, and should remain hidden,
            // remove it from the current array and do not hide or show it
            if(hiddenEntities.contains(entity))   {
                hiddenEntities.remove(entity);
                continue;
            }

            // If the entity was shown previously, and should now be hidden, hide it
            if(!hiddenEntities.contains(entity))  {
                hideEntity(entity);
            }
        }

        // Show the entities corresponding to the entries in the current array,
        // since these are the entities that should not stay hidden
        for(Entity entity : hiddenEntities)  {
            showEntity(entity, null);
        }

        hiddenEntities = newHiddenEntites; // Update the array
    }

    // Swaps the list of fake entities with the new one, adding or removing any new entities
    public void swapReplicatedEntities(Set<Entity> newReplicatedEntities, Vector locationOffset)  {
        // Loop through all of the existing fake entities and remove any that will no longer be visible to the player
        Iterator<Entity> removingIterator = replicatedEntites.keySet().iterator();
        while(removingIterator.hasNext())   {
            // Get then next id, check if it is still visible in the new entities
            Entity entity = removingIterator.next();
            if(!newReplicatedEntities.contains(entity))    {
                // If not, send an entity destroy packet, then remove the entity
                hideEntity(entity);
                removingIterator.remove();
            }
        }

        // Loop through all the new entities
        for(Entity entity : newReplicatedEntities)  {
            // If the current entity does not exist in the list
            if(!replicatedEntites.containsKey(entity))   {
                // Make a new PlayerViewableEntity instance from the entity, then send the packets to show it
                PlayerViewableEntity newEntity = new PlayerViewableEntity(entity, locationOffset);
                showEntity(entity, newEntity.location);
                replicatedEntites.put(entity, newEntity); // Add the entity to the list
            }
        }
    }

    // Hides the given entity, adding it to the set
    public void addHiddenEntity(Entity entity)  {
        hiddenEntities.add(entity);
        hideEntity(entity);
    }

    // Sends a packet to hide the entity with the given ID
    private void hideEntity(Entity entity)   {
        int entityId = entity.getEntityId();
        Object idArray = Array.newInstance(int.class, 1);
        Array.set(idArray, 0, entityId);

        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityDestroy", new Class[]{}, new Object[]{});
        ReflectUtils.setField(packet, "a", idArray);
        sendPacket(packet);
    }   

    // Sends all of the packets necessary to spawn a fake entity, or respawn a real one after it was removed
    // To disable the location override, set it to null
    private void showEntity(Entity entity, Vector locationOverride)   {
        // Get the entity from its UUID
        if(!entity.isValid())  {return;} // Don't spawn the entity if it doesn't exist

        Object nmsEntity = ReflectUtils.runMethod(entity, "getHandle");

        // Create the correct type of spawn packet, depending on the entity
        Object spawnPacket;
        if(entity instanceof Painting)  {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityPainting", new Class[]{ReflectUtils.getMcClass("EntityPainting")}, new Object[]{nmsEntity});
        }   else if(entity instanceof ExperienceOrb)    {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityExperienceOrb", new Class[]{ReflectUtils.getMcClass("EntityExperienceOrb")}, new Object[]{nmsEntity});
        }   else if(entity instanceof HumanEntity)  {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutNamedEntitySpawn", new Class[]{ReflectUtils.getMcClass("EntityHuman")}, new Object[]{nmsEntity});
        }   else if(entity instanceof LivingEntity) {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityLiving", new Class[]{ReflectUtils.getMcClass("EntityLiving")}, new Object[]{nmsEntity});
        }   else    {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntity", new Class[]{ReflectUtils.getMcClass("Entity")}, new Object[]{nmsEntity});
        }
        sendPacket(spawnPacket);

        // Send the packet that deals with the entity's metadata
        int nmsEntityId = (int) ReflectUtils.getField(nmsEntity, "id");
        Object dataWatcher = ReflectUtils.getField(nmsEntity, "datawatcher");
        Object dataPacket = ReflectUtils.newInstance("PacketPlayOutEntityMetadata", new Class[]{int.class, dataWatcher.getClass(), boolean.class},
                                                                                    new Object[]{nmsEntityId, dataWatcher, true});
        sendPacket(dataPacket);

        // Teleport the entity to the correct location if a location override is used
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
        for(PlayerViewableEntity playerEntity : replicatedEntites.values()) {
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

    // Generates and sends an entity equipment packet the old way (i.e. 1 pakcet per slot)
    private void sendEquipmentPackets_Old(int entityId, Map<String, ItemStack> newItems) {   
        // For each of the new items, make a packet and send it  
        for(Map.Entry<String, ItemStack> entry : newItems.entrySet())   {   
            Object packet = ReflectUtils.newInstance("PacketPlayOutEntityEquipment");
            // Set the entity ID, slot and item
            ReflectUtils.setField(packet, "a", entityId);
            ReflectUtils.setField(packet, "b", getNMSItemSlot(entry.getKey()));
            ReflectUtils.setField(packet, "c", getNMSItemStack(entry.getValue()));
            sendPacket(packet);
        }
    }

    // Generates and sends an equipment packet the new way, i.e. all in a list at once
    private void sendEquipmentPacket_New(int entityId, Map<String, ItemStack> newItems) {
        Class<?> pairClass = ReflectUtils.getClass("com.mojang.datafixers.util.Pair");

        List<Object> list = new ArrayList<>();
        for(Map.Entry<String, ItemStack> entry : newItems.entrySet())   {
            // Make the minecraft pair object and add it to the list
            Object pair = ReflectUtils.newInstance(pairClass,   new Class[]{Object.class, Object.class},
                                                                new Object[]{getNMSItemSlot(entry.getKey()), getNMSItemStack(entry.getValue())});
            list.add(pair);
        }
    
        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityEquipment");
        // In the new implementation, all items are sent in a list
        ReflectUtils.setField(packet, "a", entityId);
        ReflectUtils.setField(packet, "b", list);
        sendPacket(packet);
    }

    private Object getNMSItemSlot(String slot)  {
        return ReflectUtils.runMethod(null, ReflectUtils.getMcClass("EnumItemSlot"), "valueOf", new Class[]{String.class}, new Object[]{slot});
    }

    private Object getNMSItemStack(ItemStack item)  {
        return ReflectUtils.runMethod(null, ReflectUtils.getBukkitClass("inventory.CraftItemStack"), "asNMSCopy", new Class[]{ItemStack.class}, new Object[]{item});
    }

    // Sends the 6 entity equipment packets required to change the items in the
    // entities armor slots and hands
    private void sendEntityEquipmentPackets(LivingEntity entity)    {
        EntityEquipment equipment = entity.getEquipment(); // Get all of the equipment slots from the entity
        if(equipment == null)   { // Null check for the equipment, since not all living entities can equip armor
            return;
        }

        int entityId = entity.getEntityId(); // Get the entity ID, since this is what the packets use

        // Add each item to the map
        Map<String, ItemStack> newItems = new HashMap<>();
        newItems.put("MAINHAND", equipment.getItemInMainHand());
        newItems.put("OFFHAND", equipment.getItemInOffHand());
        newItems.put("FEET", equipment.getBoots());
        newItems.put("LEGS", equipment.getLeggings());
        newItems.put("CHEST", equipment.getChestplate());
        newItems.put("HEAD", equipment.getHelmet());
        // Use the correct implementation
        if(ReflectUtils.useNewEntityEquipmentImpl)  {
            sendEquipmentPacket_New(entityId, newItems);
        }   else    {
            sendEquipmentPackets_Old(entityId, newItems);
        }

    }

    private void sendPacket(Object packet)  {
        ReflectUtils.runMethod(playerConnection, "sendPacket", new Class[]{ReflectUtils.getMcClass("Packet")}, new Object[]{packet});
    }
}