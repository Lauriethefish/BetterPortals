package com.lauriethefish.betterportals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.math.MathUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class ReflectUtils {
    private static String minecraftClassPath = null;
    private static String craftbukkitClassPath = null;
    // To increase performance, store fields, classes and methods for later
    private static Map<String, Class<?>> classCache = new HashMap<>();
    private static Map<String, Field> fieldCache = new HashMap<>();
    private static Map<String, Method> methodCache = new HashMap<>();

    public static boolean isLegacy = getIfLegacy();
    public static Material portalMaterial = getPortalMaterial();

    private static boolean getIfLegacy() {
        // Test if this is a legacy version by checking if blocks have the getBlockData
        // method, which was added in 1.13
        try {
            Block.class.getMethod("getBlockData", new Class[] {});
            return false;
        } catch (NoSuchMethodException ignored) {
            return true;
        }
    }

    // We need to use a new implementation of PacketPlayOutMultiBlockChange if we
    // want to support 1.16.2
    // To decide if we are on 1.16.2+, we check to see if the MultiBlockChangeInfo
    // class exists (this is not the case on the newer version)
    public static boolean useNewMultiBlockChangeImpl = getMcClass("PacketPlayOutMultiBlockChange$MultiBlockChangeInfo", false) == null;

    // Checks to see if we need to use the new PacketPlayOutEntityEquipment
    // (the new version sends multiple armor pieces in one packet)
    public static boolean useNewEntityEquipmentImpl = getIfNewEntityEquipmentImpl();
    private static boolean getIfNewEntityEquipmentImpl() {
        try {
            Class<?> type = getMcClass("PacketPlayOutEntityEquipment").getDeclaredField("b").getType();
            return !type.equals(getMcClass("EnumItemSlot"));
        } catch(NoSuchFieldException | SecurityException ex)    {
            ex.printStackTrace();
            return false;
        }
    }

    // If using 1.13 or below, we have to use the old PacketPlayOutSpawnEntity constructor, and the old PacketPlayOutRelEntityMove
    public static boolean useNewEntitySpawnAndMoveImpl = getIfNewEntitySpawnAndMoveImpl();
    private static boolean getIfNewEntitySpawnAndMoveImpl() {
        try {
            getMcClass("PacketPlayOutSpawnEntity").getConstructor(new Class[]{getMcClass("Entity")});
            return true;
        }   catch(NoSuchMethodException ex) {
            return false;
        }
    }

    // BaseBlockPosition uses the E field for the Z coordinate on 1.16 and up
    public static boolean useNewBaseBlockPositionImpl = getIfNewBaseBlockPositionImpl();
    private static boolean getIfNewBaseBlockPositionImpl()  {
        try {
            getMcClass("BaseBlockPosition").getDeclaredField("e");
            return true;
        }   catch(NoSuchFieldException ex)  {
            return false;
        }
    }

    public static Class<?> getClass(String path, boolean printErrors)   {
        // Find if we have a cached version of this class
        Class<?> cachedClass = classCache.get(path);
        if(cachedClass == null) {
            // If we don't, find the class, then add it to the cache
            try {
                Class<?> cla = Class.forName(path);
                classCache.put(path, cla);
                return cla;
            }   catch(ClassNotFoundException ex)    {
                if(printErrors) {
                    ex.printStackTrace();
                }
                return null;
            }
        }   else    {
            return cachedClass;
        }
    }

    public static Class<?> getClass(String path)   {
        return getClass(path, true);
    }
    
    // Tries to find the given NMS class, without version dependence
    public static Class<?> getMcClass(String path, boolean printErrors)  {
        // Find the path of NMS classes if we haven't already
        if(minecraftClassPath == null)  {
            minecraftClassPath = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        }

        return getClass(minecraftClassPath + path, printErrors);
    }

    public static Class<?> getMcClass(String path)  {
        return getMcClass(path, true);
    }

    // Tries to find the given CraftBukkit class, without version dependence
    public static Class<?> getBukkitClass(String path, boolean printErrors)  {
        // Find the path of NMS classes if we haven't already
        if(craftbukkitClassPath == null)  {
            craftbukkitClassPath = "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        }

        return getClass(craftbukkitClassPath + path, printErrors);
    }

    public static Class<?> getBukkitClass(String path)  {
        return getBukkitClass(path, true);
    }

    // Attempts to find a field in the field cache, and gets it using reflection if it doesn't exist
    private static Field findField(Object obj, String name, Class<?> cla) {
        String fullName = String.format(String.format("%s.%s", cla.getName(), name));
        Field field = fieldCache.get(fullName);
        if(field == null)   { // Test if it is in the cache
            while(cla != null && field == null) { // Keep looping until no superclass can be found
                try {
                    field = cla.getDeclaredField(name);
                }   catch(NoSuchFieldException ex)  {
                    cla = cla.getSuperclass();
                    if(cla == null)    { // Print the exception if no field was found, even in the highest superclass
                        ex.printStackTrace();
                    }
                }
            }
            fieldCache.put(fullName, field); // Add it to the cache
        }
        return field;
    }
    
    // Attempts to find the method from the cache, and gets it using reflection if it doesn't exist
    private static Method findMethod(Object obj, Class<?> cla, String name, Class<?>[] params) {
        String fullName = String.format(String.format("%s.%s", cla.getName(), name));
        Method method = methodCache.get(fullName);
        if(method == null)   { // Test if it is in the cache
            Class<?> currentClass = cla;
            while(currentClass != null && method == null) { // Keep looping until no superclass can be found
                try {
                    method = currentClass.getDeclaredMethod(name, params);
                }   catch(NoSuchMethodException ex)  {
                    currentClass = currentClass.getSuperclass();
                    if(currentClass == null)    { // Print the exception if no method was found, even in the highest superclass
                        ex.printStackTrace();
                    }
                }
            }
            methodCache.put(fullName, method); // Add it to the cache
        }
        return method;
    }

    // Creates and retrieves the portal material depending on version
    private static Material getPortalMaterial()  {
        if(getIfLegacy())   {
            return Material.valueOf("PORTAL");
        }   else    {
            return Material.NETHER_PORTAL;
        }
    }

    // Converts any NMS type that extends BaseBlockPosition to a Bukkit Vector.
    private static Class<?> blockPosClass = getMcClass("BaseBlockPosition");
    public static Vector blockPositionToVector(Object pos)  {
        // In newer versions of the game, the E field stores the Z coordinate
        if(useNewBaseBlockPositionImpl) {
            return new Vector(
                (int) ReflectUtils.getField(pos, blockPosClass, "a"),
                (int) ReflectUtils.getField(pos, blockPosClass, "b"),
                (int) ReflectUtils.getField(pos, blockPosClass, "e")
            );
        }   else    {
            return new Vector(
                (int) ReflectUtils.getField(pos, blockPosClass, "a"),
                (int) ReflectUtils.getField(pos, blockPosClass, "b"),
                (int) ReflectUtils.getField(pos, blockPosClass, "c")
            );
        }
    }

    // Sets a field in the given object, even if it is private (class is automatically determined)
    public static void setField(Object obj, String name, Object value)    {
        setField(obj, obj.getClass(), name, value);
    }

    // Call this method if you need to override the class, for instance if there are two private fields with the same name
    public static void setField(Object obj, Class<?> cla, String name, Object value)    {
        try {
            Field field = findField(obj, name, cla);
            field.setAccessible(true);
            field.set(obj, value);
        }   catch(IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    // Gets the value of the named field. Will return null if the field doesn't exist, or is of the wrong type
    public static Object getField(Object obj, Class<?> cla, String name)   {
        try {
            Field field = findField(obj, name, cla);
            field.setAccessible(true);
            return field.get(obj);
        }   catch(IllegalAccessException | ClassCastException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object getField(Object obj, String name)  {
        return getField(obj, obj.getClass(), name);
    }

    // Runs the names method on the given object with the given args
    public static Object runMethod(Object obj, Class<?> cla, String name, Class<?>[] params, Object[] args)    {
        try {
            Method method = findMethod(obj, cla, name, params);
            method.setAccessible(true);
            Object result = method.invoke(obj, args);
            return result;
        }   catch(InvocationTargetException | IllegalAccessException ex)   {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object runMethod(Object obj, String name, Class<?>[] params, Object[] args)       {
        return runMethod(obj, obj.getClass(), name, params, args);
    }

    // Makes a new instance of the given NMS class name and args
    public static Object newInstance(Class<?> cla, Class<?>[] argTypes, Object[] args)    {
        try {
            Constructor<?> contructor = cla.getDeclaredConstructor(argTypes);
            contructor.setAccessible(true);
            return contructor.newInstance(args);
        }   catch(NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object newInstance(String className, Class<?>[] argTypes, Object[] args)    {
        return newInstance(getMcClass(className), argTypes, args);
    }

    // Use this if you don't need any arguments in the constructor
    public static Object newInstance(Class<?> cla)  {
        return newInstance(cla, new Class<?>[]{}, new Object[]{});
    }

    public static Object newInstance(String className)    {
        return newInstance(getMcClass(className));
    }

    public static Object runMethod(Object obj, String name)  {
        return runMethod(obj, name, new Class[]{}, new Object[]{});
    }

    // Store the EnumDirection variants for quicker lookup
    public static Map<Vector, Object> enumDirectionVariants = getEnumDirectionVariants();
    private static Map<Vector, Object> getEnumDirectionVariants()   {
        Class<?> enumDirection = getMcClass("EnumDirection");
        
        // Loop through each variant of the EnumDirection class
        Map<Vector, Object> variants = new HashMap<>();
        for(Object constant : enumDirection.getEnumConstants()) {
            // Find the BlockPosition that represents this variant's direction, then convert it to a vector
            Vector direction = blockPositionToVector(ReflectUtils.getField(constant, "m"));
            variants.put(direction, constant); // Add it to the map
        }

        return variants;
    }

    // Gets the NMS EnumDirection object from a direction vector
    public static Object getEnumDirection(Vector dir)   {
        // Round the direction, since otherwise it is usually slightly off, causing this function to return null
        return enumDirectionVariants.get(MathUtils.round(dir));
    }
}