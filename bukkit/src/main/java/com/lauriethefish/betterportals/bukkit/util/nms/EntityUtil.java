package com.lauriethefish.betterportals.bukkit.util.nms;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class EntityUtil {
    private static final Method GET_HANDLE;
    private static final Field DATA_WATCHER;
    private static final Field ENTITY_YAW;
    private static final boolean USE_DIRECT_ENTITY_PACKET;
    private static Method GET_ENTITY_SPAWN_PACKET = null;
    private static final Constructor<?> ENTITY_TRACKER_ENTRY_NEW;

    static {
        Class<?> NMS_ENTITY = MinecraftReflectionUtil.findNMSClass("Entity");

        GET_HANDLE = ReflectionUtil.findMethod(MinecraftReflectionUtil.findCraftBukkitClass("entity.CraftEntity"), "getHandle");
        DATA_WATCHER = ReflectionUtil.findField(NMS_ENTITY, "datawatcher");
        ENTITY_YAW = ReflectionUtil.findField(NMS_ENTITY, "yaw");

        // On newer versions of the game, the Entity NMS class have an abstract method for getting the correct spawn packet that is overridden by every entity.
        if(VersionUtil.isMcVersionAtLeast("1.14")) {
            USE_DIRECT_ENTITY_PACKET = true;
            ENTITY_TRACKER_ENTRY_NEW = null;

            Class<?> NMS_PACKET = MinecraftReflectionUtil.findNMSClass("Packet");
            for(Method method : NMS_ENTITY.getMethods()) {
                if(method.getReturnType().equals(NMS_PACKET) && Modifier.isAbstract(method.getModifiers())) {
                    GET_ENTITY_SPAWN_PACKET = method;
                    break;
                }
            }

            if(GET_ENTITY_SPAWN_PACKET == null) {
                throw new RuntimeException("Unable to find method to get entity spawn packet");
            }
        }   else    {
            // On older versions, we create a dummy EntityTrackerEntry and use that to generate our packet
            USE_DIRECT_ENTITY_PACKET = false;

            Class<?> TRACKER_ENTRY = MinecraftReflectionUtil.findNMSClass("EntityTrackerEntry");

            try {
                ENTITY_TRACKER_ENTRY_NEW = TRACKER_ENTRY.getConstructor(NMS_ENTITY, int.class, int.class, int.class, boolean.class);
            }   catch(NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }

            GET_ENTITY_SPAWN_PACKET = ReflectionUtil.findMethod(TRACKER_ENTRY, "e");
        }
    }

    /**
     * ProtocolLib unfortunately doesn't provide any methods for getting the <i>actual</i> {@link WrappedDataWatcher} of an entity.
     * {@link WrappedDataWatcher#WrappedDataWatcher(Entity)} doesn't do this - it returns a new empty {@link WrappedDataWatcher} for this entity.
     * This function wraps the entities actual watcher in the ProtocolLib wrapper.
     * @param entity The entity to wrap the data watcher of
     * @return The wrapped data watcher
     */
    @NotNull
    public static WrappedDataWatcher getActualDataWatcher(@NotNull Entity entity) {
        try {
            Object nmsEntity = GET_HANDLE.invoke(entity);
            Object nmsDataWatcher = DATA_WATCHER.get(nmsEntity);
            return new WrappedDataWatcher(nmsDataWatcher);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Getting a valid spawn packet that works correctly for a specific {@link Entity} is surprisingly difficult.
     * This method uses some NMS to get the correct spawn packet.
     * @param entity The entity to get the spawn packet of
     * @return A container with the valid packet, or <code>null</code> since some entities can't be spawned with a packet.
     */
    public static @Nullable PacketContainer getRawEntitySpawnPacket(@NotNull Entity entity) {
        try {
            Object nmsEntity = GET_HANDLE.invoke(entity);
            if(USE_DIRECT_ENTITY_PACKET) {
                return PacketContainer.fromPacket(GET_ENTITY_SPAWN_PACKET.invoke(nmsEntity));
            }   else    {
                // Create a dummy tracker entry
                Object trackerEntry = ENTITY_TRACKER_ENTRY_NEW.newInstance(nmsEntity, 0, 0, 0, false);
                Object nmsPacket = GET_ENTITY_SPAWN_PACKET.invoke(trackerEntry);
                if(nmsPacket == null) {return null;}

                return PacketContainer.fromPacket(nmsPacket);
            }
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Bukkit for some reason returns the head rotation instead of the actual yaw for living entities, which causes some issues with
     * entities' rotation looking incorrect while moving.
     * @param entity The entity to get the location of
     * @return A direction vector with the actual entity yaw
     */
    @NotNull
    public static Vector getActualEntityDirection(@NotNull Entity entity) {
        try {
            Object nmsEntity = GET_HANDLE.invoke(entity);

            float yaw = (float) ENTITY_YAW.get(nmsEntity); // Use the NMS yaw field
            Location entityLoc = entity.getLocation();
            entityLoc.setYaw(yaw);
            return entityLoc.getDirection();

        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
