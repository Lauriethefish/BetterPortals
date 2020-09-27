package com.lauriethefish.betterportals.entitymanipulation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.math.MathUtils;
import com.lauriethefish.betterportals.portal.Portal;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityManipulator    {
    private Random random;
    private Object playerConnection; // Store the NMS connection update to increase speed of sending packets

    // Set of all hidden entities
    // A set is used here, since we don't need to preserve order and O(1) is fun
    private Set<Entity> hiddenEntities = new HashSet<Entity>();

    // All of the fake entities that the player can currently see
    private Map<Entity, ViewableEntity> replicatedEntites = new HashMap<Entity, ViewableEntity>();

    public EntityManipulator(BetterPortals pl, PlayerData playerData)    {
        Player player = playerData.getPlayer();

        random = new Random(player.getEntityId());
        // Find the NMS connection using reflection
        Object craftPlayer = ReflectUtils.runMethod(player, "getHandle");
        playerConnection = ReflectUtils.getField(craftPlayer, "playerConnection");
    }

    // Sends a PacketPlayOutCollect to play the animation of entity picking up item
    public void sendPickupItemPacket(ViewableEntity entity, ViewableEntity item)  {
        Object packet = ReflectUtils.newInstance("PacketPlayOutCollect");
        ReflectUtils.setField(packet, "a", item.getEntityId());
        ReflectUtils.setField(packet, "b", entity.getEntityId());
        ReflectUtils.setField(packet, "c", ((Item) item.getEntity()).getItemStack().getAmount());
        sendPacket(packet);
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
                hideEntity(entity.getEntityId());
            }
        }

        // Show the entities corresponding to the entries in the current array,
        // since these are the entities that should not stay hidden
        for(Entity entity : hiddenEntities)  {
            showEntity(entity, null, null, entity.getEntityId());
        }

        hiddenEntities = newHiddenEntites; // Update the array
    }

    // Removes all replicated entities and shows any hidden entities
    public void resetAll(boolean sendPackets)  {
        if(sendPackets)    {
            swapHiddenEntities(new HashSet<>());
            swapReplicatedEntities(new HashSet<>(), null);
        }   else    {
            hiddenEntities = new HashSet<>();
            replicatedEntites = new HashMap<>();
        }
    }

    // Sends a PacketPlayOutAnimation to the player for the given entity
    public void sendAnimationPacket(ViewableEntity entity, int animationType) {
        Object packet = ReflectUtils.newInstance("PacketPlayOutAnimation");
        ReflectUtils.setField(packet, "a", entity.getEntityId());
        ReflectUtils.setField(packet, "b", animationType);
        sendPacket(packet);
    }

    // Sends a PacketPlayOutMount to show to the player that the entities in mountedIds are riding the entity entityId
    public void sendMountPacket(int entityId, int[] mountedIds)  {
        Object packet = ReflectUtils.newInstance("PacketPlayOutMount");
        ReflectUtils.setField(packet, "a", entityId);
        ReflectUtils.setField(packet, "b", mountedIds);
        sendPacket(packet);
    }

    public ViewableEntity getViewedEntity(Entity entity)  {
        return replicatedEntites.get(entity);
    }

    // Swaps the list of fake entities with the new one, adding or removing any new entities
    public void swapReplicatedEntities(Set<Entity> newReplicatedEntities, Portal portal)  {
        // Loop through all of the existing fake entities and remove any that will no longer be visible to the player
        Iterator<ViewableEntity> removingIterator = replicatedEntites.values().iterator();
        while(removingIterator.hasNext())   {
            // Get the next entity, check if it is still visible in the new entities
            ViewableEntity entity = removingIterator.next();
            if(!newReplicatedEntities.contains(entity.getEntity()))   {
                // If not, send an entity destroy packet, then remove the entity
                hideEntity(entity.getEntityId());
                removingIterator.remove();
            }
        }

        // Loop through all the new entities
        for(Entity entity : newReplicatedEntities)  {
            // If the current entity does not exist in the list
            if(!replicatedEntites.containsKey(entity))   {
                // Make a new PlayerViewableEntity instance from the entity, then send the packets to show it
                ViewableEntity newEntity = new ViewableEntity(this, entity, portal, random);
                showEntity(entity, newEntity.getLocation(), newEntity.getRotation(), newEntity.getEntityId());
                replicatedEntites.put(entity, newEntity); // Add the entity to the list
            }
        }
    }

    // Hides the given entity, adding it to the set
    public void addHiddenEntity(Entity entity)  {
        hiddenEntities.add(entity);
        hideEntity(entity.getEntityId());
    }

    // Sends a packet to hide the entity with the given ID
    private void hideEntity(int entityId)   {
        Object idArray = Array.newInstance(int.class, 1);
        Array.set(idArray, 0, entityId);

        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityDestroy", new Class[]{}, new Object[]{});
        ReflectUtils.setField(packet, "a", idArray);
        sendPacket(packet);
    }
    
    // Sets the location in a spawn packet, using the field names given
    // This is used because different types of spawn packet have different field names for the coordinates
    private void setSpawnLocation(Object packet, Vector location, String xName, String yName, String zName)  {
        ReflectUtils.setField(packet, xName, location.getX());
        ReflectUtils.setField(packet, yName, location.getY());
        ReflectUtils.setField(packet, zName, location.getZ());
    }

    // Generates an entity spawn packet in a way that works in 1.13 and 1.12 by using an EntityTrackerEntry to generate the packet for us
    private Object generateEntitySpawnPacket_Old(Object nmsEntity)  {
        Object trackerEntry = ReflectUtils.newInstance("EntityTrackerEntry", new Class[] {ReflectUtils.getMcClass("Entity"), int.class, int.class, int.class, boolean.class},
                                                      new Object[]{nmsEntity, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true});

        Object packet = ReflectUtils.runMethod(trackerEntry, "e");
        return packet;
    }

    static Vector getEntityPosition(Entity entity, Object nmsEntity)  {
        // For entities like item frames and paintings, we get the coordinates using a BlockPosition
        // This is because the normal entity coordinates are not accurate for spawn packets
        if(entity instanceof Hanging)   {
            Object blockPosition = ReflectUtils.getField(nmsEntity, "blockPosition");
            return MathUtils.moveToCenterOfBlock(ReflectUtils.blockPositionToVector(blockPosition));
        }   else    {
            return entity.getLocation().toVector();
        }
    }

    // Sends all of the packets necessary to spawn a fake entity, or respawn a real one after it was removed
    private void showEntity(Entity entity, Vector locationOverride, Vector directionOverride, int entityId)   {
        if(entity.isDead()) {return;}
        Object nmsEntity = ReflectUtils.runMethod(entity, "getHandle");

        // Use either the entities location, or the override
        Vector location = locationOverride == null ? getEntityPosition(entity, nmsEntity) : locationOverride;
        Vector direction = directionOverride == null ? entity.getLocation().getDirection() : directionOverride;

        // Create the correct type of spawn packet, depending on the entity
        Object spawnPacket;
        if(entity instanceof Painting)  {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityPainting", new Class[]{ReflectUtils.getMcClass("EntityPainting")}, new Object[]{nmsEntity});
            
            // Painting spawn packets are slightly different, as they use a BlockPosition and EnumDirection
            Object blockPosition = ReflectUtils.newInstance("BlockPosition", new Class[]{int.class, int.class, int.class},
                                                            new Object[]{location.getBlockX(), location.getBlockY(), location.getBlockZ()});
            ReflectUtils.setField(spawnPacket, "c", blockPosition);

            Object enumDirection = ReflectUtils.getEnumDirection(direction);
            ReflectUtils.setField(spawnPacket, "d", enumDirection);
        }   else if(entity instanceof ExperienceOrb)    {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityExperienceOrb", new Class[]{ReflectUtils.getMcClass("EntityExperienceOrb")}, new Object[]{nmsEntity});
            setSpawnLocation(spawnPacket, location, "b", "c", "d");
        }   else if(entity instanceof HumanEntity)  {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutNamedEntitySpawn", new Class[]{ReflectUtils.getMcClass("EntityHuman")}, new Object[]{nmsEntity});
            setSpawnLocation(spawnPacket, location, "c", "d", "e");
        }   else if(entity instanceof LivingEntity) {
            spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntityLiving", new Class[]{ReflectUtils.getMcClass("EntityLiving")}, new Object[]{nmsEntity});
            setSpawnLocation(spawnPacket, location, "d", "e", "f");
        }   else    {
            // If we are on 1.14 and up, the contructor for PacketPlayOutSpawnEntity can find all the entity type stuff for us
            // Otherwise, we use a function that gets EntityTrackerEntry to do everything
            if(ReflectUtils.useNewEntitySpawnAndMoveImpl)   {
                spawnPacket = ReflectUtils.newInstance("PacketPlayOutSpawnEntity", new Class[]{ReflectUtils.getMcClass("Entity")}, new Object[]{nmsEntity});
            }   else    {
                spawnPacket = generateEntitySpawnPacket_Old(nmsEntity);
            }
            setSpawnLocation(spawnPacket, location, "c", "d", "e");
        }

        ReflectUtils.setField(spawnPacket, "a", entityId);
        sendPacket(spawnPacket);

        // Force the entity to update its data, since it just spawned
        sendMetadataPacket(nmsEntity, entityId);

        // If the entity is living, we have to set its armor and hand/offhand
        if(entity instanceof LivingEntity)   {
            sendEntityEquipmentPackets((LivingEntity) entity, entityId);
        }
    }

    // Sends a PacketPlayOutEntityMetadata to update the entities data, if necessary
    public void sendMetadataPacket(Object nmsEntity, int entityId) {
        // The NMS DataWatcher deals with checking an entity for data changes, we use it to send the metadata packet
        Object dataWatcher = ReflectUtils.getField(nmsEntity, "datawatcher");
        Object dataPacket = ReflectUtils.newInstance("PacketPlayOutEntityMetadata", new Class[]{int.class, dataWatcher.getClass(), boolean.class},
                                                                                    new Object[]{entityId, dataWatcher, true});
        sendPacket(dataPacket);
        
    }

    public void sendMovePacket(int entityId, Vector offset)    {
        short x = (short) (offset.getX() * 4096);
        short y = (short) (offset.getY() * 4096);
        short z = (short) (offset.getZ() * 4096);

        // In newer versions, a short is used for relative move packets, but in 1.13 and under, a long is used
        if(ReflectUtils.useNewEntitySpawnAndMoveImpl)   {
            sendPacket(ReflectUtils.newInstance("PacketPlayOutEntity$PacketPlayOutRelEntityMove", 
                        new Class[]{int.class, short.class, short.class, short.class, boolean.class},
                        new Object[]{entityId, x, y, z, true}));
        }   else    {
            sendPacket(ReflectUtils.newInstance("PacketPlayOutEntity$PacketPlayOutRelEntityMove", 
                        new Class[]{int.class, long.class, long.class, long.class, boolean.class},
                        new Object[]{entityId, (long) x, (long) y, (long) z, true}));
        }
    }

    // Returns true if the offset is small enough for relative move packets
    public boolean isSafeForMovePacket(Vector offset)   {
        return offset.getX() < 8.0 && offset.getY() < 8.0 && offset.getZ() < 8.0;
    }

    // Sends a PacketPlayOutRelEntityMoveLook packet to the player
    public void sendMoveLookPacket(int entityId, Vector offset, byte yaw, byte pitch)    {
        short x = (short) (offset.getX() * 4096);
        short y = (short) (offset.getY() * 4096);
        short z = (short) (offset.getZ() * 4096);

        // In newer versions, a short is used for relative move packets, but in 1.13 and under, a long is used
        if(ReflectUtils.useNewEntitySpawnAndMoveImpl)   {
            sendPacket(ReflectUtils.newInstance("PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook", 
                        new Class[]{int.class, short.class, short.class, short.class, byte.class, byte.class, boolean.class},
                        new Object[]{entityId, x, y, z, yaw, pitch, true}));
        }   else    {
            sendPacket(ReflectUtils.newInstance("PacketPlayOutEntity$PacketPlayOutRelEntityMoveLook", 
                        new Class[]{int.class, long.class, long.class, long.class, byte.class, byte.class, boolean.class},
                        new Object[]{entityId, (long) x, (long) y, (long) z, yaw, pitch, true}));
        }
    }

    public void sendTeleportPacket(int entityId, Vector destination, byte yaw, byte pitch)    {
        // Make a teleport packet
        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityTeleport");
        ReflectUtils.setField(packet, "a", entityId);

        // Set the teleport location to the position of the entity on the player's side of the portal
        ReflectUtils.setField(packet, "b", destination.getX());
        ReflectUtils.setField(packet, "c", destination.getY());
        ReflectUtils.setField(packet, "d", destination.getZ());
        ReflectUtils.setField(packet, "e", yaw);
        ReflectUtils.setField(packet, "f", pitch);

        sendPacket(packet);
    }

    public void sendHeadRotationPacket(int entityId, byte headRotation)    {
        Object packet = ReflectUtils.newInstance("PacketPlayOutEntityHeadRotation");
        ReflectUtils.setField(packet, "a", entityId); // Set the overridden entityId
        ReflectUtils.setField(packet, "b", headRotation); // Use the randomized entity ID of fake entities

        sendPacket(packet);
    }

    // Sends the two packets that rotate an entities head
    public void sendLookPacket(int entityId, byte yaw, byte pitch)   {
        // Send both the head rotation and look packets
        sendPacket(ReflectUtils.newInstance("PacketPlayOutEntity$PacketPlayOutEntityLook", 
                                            new Class[]{int.class, byte.class, byte.class, boolean.class},
                                            new Object[]{entityId, yaw, pitch, true}));                                  
    }

    public void sendSleepPacket(Object nmsEntity, int entityId)   {
        Object packet = ReflectUtils.newInstance("PacketPlayOutBed");
        Object blockPosition = ReflectUtils.getField(nmsEntity, "bedPosition");

        ReflectUtils.setField(packet, "a", entityId);
        ReflectUtils.setField(packet, "b", blockPosition);
        sendPacket(packet);
    }

    public void sendBlockBreakPacket(ViewableEntity entity, Block block)  {
        Object packet = ReflectUtils.newInstance("PacketPlayOutBlockBreakAnimation", 
                                                new Class[]{int.class, ReflectUtils.getMcClass("BlockPosition"), int.class},
                                                new Object[]{entity.getEntityId(), ReflectUtils.createBlockPosition(block.getLocation()), 0});
        sendPacket(packet);
    }

    // Loops through all the fake entities and updates their position and equipment
    public void updateFakeEntities()   {      
        for(ViewableEntity playerEntity : replicatedEntites.values()) {
            playerEntity.update();
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
    public void sendEntityEquipmentPackets(LivingEntity entity, int entityId)    {
        EntityEquipment equipment = entity.getEquipment(); // Get all of the equipment slots from the entity
        if(equipment == null)   { // Null check for the equipment, since not all living entities can equip armor
            return;
        }

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