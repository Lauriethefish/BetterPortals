package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
import com.lauriethefish.betterportals.bukkit.util.nms.EntityUtil;
import com.lauriethefish.betterportals.bukkit.util.nms.RotationUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Deals with sending all of the packets for entity processing
 * Warning: a fair bit of this class is me complaining about mojang
 */
@Singleton
public class EntityPacketManipulator implements IEntityPacketManipulator {
    private final Logger logger;

    @Inject
    public EntityPacketManipulator(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void showEntity(EntityInfo tracker, Collection<Player> players) {
        // Generate the packet that NMS would normally use to spawn the entity
        PacketContainer spawnPacket = EntityUtil.getRawEntitySpawnPacket(tracker.getEntity());
        if(spawnPacket == null) {return;}

        // Use the rendered entity ID
        spawnPacket.getIntegers().write(0, tracker.getEntityId());

        // Translate to the correct rendered position
        Vector actualPos = getPositionFromSpawnPacket(spawnPacket);
        if(tracker.getEntity() instanceof Hanging) {
            actualPos = MathUtil.moveToCenterOfBlock(actualPos);
        }

        Vector renderedPos = tracker.getTranslation().transform(actualPos);
        writePositionToSpawnPacket(spawnPacket, renderedPos);
        setSpawnRotation(spawnPacket, tracker);

        sendPacket(spawnPacket, players);

        // Living Entities also require us to handle entity equipment
        if(tracker.getEntity() instanceof LivingEntity) {
            EntityEquipmentWatcher equipmentWatcher = new EntityEquipmentWatcher((LivingEntity) tracker.getEntity());
            Map<EnumWrappers.ItemSlot, ItemStack> changes = equipmentWatcher.checkForChanges();
            if(changes.size() > 0) {
                sendEntityEquipment(tracker, changes, players);
            }
        }

        sendMetadata(tracker, players);
    }

    private Vector getPositionFromSpawnPacket(PacketContainer packet) {
        if(packet.getType() == PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
            BlockPosition blockPos = packet.getBlockPositionModifier().read(0);
            return blockPos.toVector();
        }   else    {
            StructureModifier<Double> doubles = packet.getDoubles();
            return new Vector(
                doubles.read(0),
                doubles.read(1),
                doubles.read(2)
            );
        }
    }

    private void writePositionToSpawnPacket(PacketContainer packet, Vector position) {
        if(packet.getType() == PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
            packet.getBlockPositionModifier().write(0, new BlockPosition(position));
        }   else {
            StructureModifier<Double> doubles = packet.getDoubles();
            doubles.write(0, position.getX());
            doubles.write(1, position.getY());
            doubles.write(2, position.getZ());
        }
    }

    // Every packet handles spawn rotation differently for whatever reason
    // This method will set the correct field(s) for whatever packet type
    private void setSpawnRotation(PacketContainer packet, EntityInfo entityInfo) {
        // Calculate the correct byte yaw, pitch, and head rotation for the entity
        Location renderedPos = entityInfo.findRenderedLocation();

        byte yaw = (byte) (int) (renderedPos.getYaw() * 256.0f / 360.0f);
        byte pitch = (byte) (int) (renderedPos.getPitch() * 256.0f / 360.0f);

        PacketType packetType = packet.getType();
        if(packetType == PacketType.Play.Server.SPAWN_ENTITY_PAINTING) {
            StructureModifier<EnumWrappers.Direction> directions = packet.getDirections();
            EnumWrappers.Direction currentDirection = directions.read(0);
            EnumWrappers.Direction rotated = RotationUtil.rotateBy(currentDirection, entityInfo.getRotation());
            // Make sure to catch this - it should never happen unless something's gone very wrong
            if (rotated == null) {
                throw new IllegalStateException("Portal attempted to rotate painting to an invalid block direction");
            }
            logger.finer("Current direction: %s. Rotated: %s", currentDirection, rotated);

            directions.write(0, rotated);
        }   else if(packetType == PacketType.Play.Server.SPAWN_ENTITY)  {
            // Hanging entities use block faces for their rotation
            if(entityInfo.getEntity() instanceof Hanging) {
                // Minecraft deals with this as the ID of the direction as an int for item frames
                // RotationUtil has some convenient methods for dealing with this
                EnumWrappers.Direction currentDirection = RotationUtil.getDirection(packet.getIntegers().read(6));
                if(currentDirection != null) {
                    EnumWrappers.Direction rotated = RotationUtil.rotateBy(currentDirection, entityInfo.getRotation());
                    // Make sure to catch this - it should never happen unless something's gone very wrong
                    if (rotated == null) {
                        throw new IllegalStateException("Portal attempted to rotate a hanging entity to an invalid block direction");
                    }
                    logger.finer("Current direction: %s. Rotated: %s. ID: %d", currentDirection, rotated, RotationUtil.getId(rotated));
                    packet.getIntegers().write(6, RotationUtil.getId(rotated));
                }
            }

            // Set the modified pitch and yaw
            packet.getIntegers().write(4, RotationUtil.getPacketRotationInt(renderedPos.getYaw()));
            packet.getIntegers().write(5, RotationUtil.getPacketRotationInt(renderedPos.getPitch()));
        }   else if(packetType == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            packet.getBytes().write(0, yaw);
            packet.getBytes().write(1, pitch);
            packet.getBytes().write(2, yaw);
        }   else if(packetType == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            packet.getBytes().write(0, yaw);
            packet.getBytes().write(1, pitch);
        }
    }

    @Override
    public void hideEntity(EntityInfo tracker, Collection<Player> players) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);

        packet.getIntegerArrays().write(0, new int[]{tracker.getEntityId()});
        sendPacket(packet, players);
    }

    @Override
    public void sendEntityMove(EntityInfo tracker, Vector offset, Collection<Player> players) {
        offset = tracker.getRotation().transform(offset);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
        packet.getIntegers().write(0, tracker.getEntityId());

        // We need to convert to the short location, since minecraft is weird and does it like this
        if(VersionUtil.isMcVersionAtLeast("1.14.0")) {
            StructureModifier<Short> shorts = packet.getShorts();
            shorts.write(0, (short) (offset.getX() * 4096));
            shorts.write(1, (short) (offset.getY() * 4096));
            shorts.write(2, (short) (offset.getZ() * 4096));
        }   else    {
            StructureModifier<Integer> integers = packet.getIntegers();
            integers.write(1, (int) (offset.getX() * 4096));
            integers.write(2, (int) (offset.getY() * 4096));
            integers.write(3, (int) (offset.getZ() * 4096));
        }

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityMoveLook(EntityInfo tracker, Vector offset, Collection<Player> players) {
        Location entityPos = tracker.findRenderedLocation();
        offset = tracker.getRotation().transform(offset); // Make sure to transform the given offset so that it's correct for the rendered position

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        packet.getIntegers().write(0, tracker.getEntityId());

        // Minecraft is dumb and uses bytes for this (why... just use a float jeb)
        StructureModifier<Byte> bytes = packet.getBytes();
        bytes.write(0, (byte) RotationUtil.getPacketRotationInt(entityPos.getYaw()));
        bytes.write(1, (byte) RotationUtil.getPacketRotationInt(entityPos.getPitch()));

        // We need to convert to the short location, since minecraft is weird and does it like this
        if(VersionUtil.isMcVersionAtLeast("1.14.0")) {
            StructureModifier<Short> shorts = packet.getShorts();
            shorts.write(0, (short) (offset.getX() * 4096));
            shorts.write(1, (short) (offset.getY() * 4096));
            shorts.write(2, (short) (offset.getZ() * 4096));
        }   else    {
            StructureModifier<Integer> integers = packet.getIntegers();
            integers.write(1, (int) (offset.getX() * 4096));
            integers.write(2, (int) (offset.getY() * 4096));
            integers.write(3, (int) (offset.getZ() * 4096));
        }

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityLook(EntityInfo tracker, Collection<Player> players) {
        // Transform the entity rotation to the origin of the portal
        Location entityPos = tracker.findRenderedLocation();

        // Minecraft is dumb and uses bytes for this (why... just use a float jeb)
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
        packet.getIntegers().write(0, tracker.getEntityId());

        StructureModifier<Byte> bytes = packet.getBytes();
        bytes.write(0, (byte) RotationUtil.getPacketRotationInt(entityPos.getYaw()));
        bytes.write(1, (byte) RotationUtil.getPacketRotationInt(entityPos.getPitch()));

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityTeleport(EntityInfo tracker, Collection<Player> players) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, tracker.getEntityId());

        Location entityPos = tracker.findRenderedLocation();

        StructureModifier<Double> doubles = packet.getDoubles();
        doubles.write(0, entityPos.getX());
        doubles.write(1, entityPos.getY());
        doubles.write(2, entityPos.getZ());

        // Why minecraft, why must you use a byte!
        StructureModifier<Byte> bytes = packet.getBytes();
        bytes.write(0, (byte) (int) (entityPos.getYaw() * 256.0f / 360.0f));
        bytes.write(1, (byte) (int) (entityPos.getPitch() * 256.0f / 360.0f));

        packet.getBooleans().write(0, tracker.getEntity().isOnGround());
    }

    @Override
    public void sendEntityHeadRotation(EntityInfo tracker, Collection<Player> players) {
        // Entity yaw is actually head rotation in Bukkit
        Location renderedPos = tracker.findRenderedLocation();

        // Why.. why is this a byte
        byte headRotation = (byte) (int) (renderedPos.getYaw() * 256.0f / 360.0f);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        packet.getIntegers().write(0, tracker.getEntityId());
        packet.getBytes().write(0, headRotation);
        sendPacket(packet, players);
    }

    @Override
    public void sendMount(EntityInfo tracker, Collection<EntityInfo> riding, Collection<Player> players) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.MOUNT);

        int[] ridingIds = new int[riding.size()];
        int i = 0;
        for(EntityInfo ridingTracker : riding) {
            ridingIds[i] = ridingTracker.getEntityId();
            i++;
        }

        packet.getIntegers().write(0, tracker.getEntityId());
        packet.getIntegerArrays().write(0, ridingIds);

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityEquipment(EntityInfo tracker, Map<EnumWrappers.ItemSlot, ItemStack> changes, Collection<Player> players) {
        if(VersionUtil.isMcVersionAtLeast("1.16.0")) {
            // Why minecraft, why not just use a map...
            List<Pair<EnumWrappers.ItemSlot, ItemStack>> wrappedChanges = new ArrayList<>();
            changes.forEach((slot, item) -> {
                logger.finest("Performing equipment change. Slot: %s. New value: %s", slot, item);

                wrappedChanges.add(new Pair<>(slot, item == null ? new ItemStack(Material.AIR) : item));
            });

            PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, tracker.getEntityId());
            packet.getSlotStackPairLists().write(0, wrappedChanges);
            sendPacket(packet, players);
        }   else    {
            changes.forEach((slot, item) -> {
                PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
                packet.getIntegers().write(0, tracker.getEntityId());
                packet.getItemSlots().write(0, slot);
                packet.getItemModifier().write(0, item == null ? new ItemStack(Material.AIR) : item);

                sendPacket(packet, players);
            });
        }
    }

    @Override
    public void sendMetadata(EntityInfo tracker, Collection<Player> players) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

        packet.getIntegers().write(0, tracker.getEntityId());

        WrappedDataWatcher dataWatcher = EntityUtil.getActualDataWatcher(tracker.getEntity()); // Use the Entity's actual data watcher, not ProtocolLib's method which gives us a dummy
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityVelocity(EntityInfo tracker, Vector newVelocity, Collection<Player> players) {
        // Rotate the velocity back to the origin of the portal
        Vector entityVelocity = tracker.getRotation().transform(newVelocity);
        // Avoid integer overflows by limiting these values to 3.9
        entityVelocity = MathUtil.min(entityVelocity, new Vector(3.9, 3.9, 3.9));
        entityVelocity = MathUtil.max(entityVelocity, new Vector(-3.9, -3.9, -3.9));

        // Multiply by 8000 to convert the velocity into the integer representation. (jeb, just use a float)
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_VELOCITY);
        StructureModifier<Integer> integers = packet.getIntegers();
        integers.write(0, tracker.getEntityId());
        integers.write(1, (int) (entityVelocity.getX() * 8000.0D));
        integers.write(2, (int) (entityVelocity.getY() * 8000.0D));
        integers.write(3, (int) (entityVelocity.getZ() * 8000.0D));

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityAnimation(EntityInfo tracker, Collection<Player> players, AnimationType animationType) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);

        logger.finer("Sending animation packet of type %s", animationType);
        StructureModifier<Integer> integers = packet.getIntegers();
        integers.write(0, tracker.getEntityId());
        integers.write(1, animationType.getNmsId());

        sendPacket(packet, players);
    }

    @Override
    public void sendEntityPickupItem(EntityInfo tracker, EntityInfo pickedUp, Collection<Player> players) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.COLLECT);

        StructureModifier<Integer> integers = packet.getIntegers();
        integers.write(0, pickedUp.getEntityId());
        integers.write(1, tracker.getEntityId());
        integers.write(2, ((Item) pickedUp.getEntity()).getItemStack().getAmount());

        sendPacket(packet, players);
    }

    private void sendPacket(PacketContainer packet, Collection<Player> players) {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        try {
            for (Player player : players) {
                protocolManager.sendServerPacket(player, packet);
            }
        }   catch(InvocationTargetException ex) {
            throw new RuntimeException("Failed to send packet", ex);
        }
    }
}
