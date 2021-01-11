package com.lauriethefish.betterportals.bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lauriethefish.betterportals.bukkit.math.MathUtils;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager_1_16_2;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager_Old;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager_Tuinity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Cancellable;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ReflectUtils {
    private static String minecraftClassPath = null;
    private static String craftbukkitClassPath = null;
    // To increase performance, store fields, classes and methods for later
    private static Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static Map<String, Field> fieldCache = new ConcurrentHashMap<>();
    private static Map<String, Method> methodCache = new ConcurrentHashMap<>();

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

    // Different server implementations and versions change PacketPlayOutMultiBlockChange
    // Here we find the correct version of the manager class to instantiate
    public static Class<?> multiBlockChangeImpl = findMultiBlockChangeImpl();
    private static Class<?> findMultiBlockChangeImpl()  {
        if(getMcClass("PacketPlayOutMultiBlockChange$MultiBlockChangeInfo", false) != null)    {
            return MultiBlockChangeManager_Old.class;
        }   else if(List.class.isAssignableFrom(ReflectUtils.findField(null, "b", getMcClass("PacketPlayOutMultiBlockChange"), true).getType()))    {
            // When using Tuinity, the fields of PacketPlayOutMultiBlockChange are modified to use a List instead of just an array
            return MultiBlockChangeManager_Tuinity.class;
        }   else    {
            return MultiBlockChangeManager_1_16_2.class;
        }
    }

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

    // If we cannot cancel ChunkUnloadEvent (this is true in newer versions), then we use a different method to forceload chunks
    public static boolean useNewChunkLoadingImpl = !Cancellable.class.isAssignableFrom(ChunkUnloadEvent.class);
    public static boolean sendBedPackets = ReflectUtils.getMcClass("PacketPlayOutBed", false) != null;

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
    public static Field findField(Object obj, String name, Class<?> cla, boolean printErrors) {
        String fullName = String.format("%s.%s", cla.getName(), name);
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
            field.setAccessible(true);
            fieldCache.put(fullName, field); // Add it to the cache
        }
        return field;
    }

    public static Field findField(Object obj, String name, Class<?> cla) {
        return findField(obj, name, cla, true);
    }

    // Attempts to find the method from the cache, and gets it using reflection if it doesn't exist
    public static Method findMethod(Class<?> cla, String name, Class<?>[] params, boolean printErrors) {
        String fullName = String.format("%s.%s", cla.getName(), name);
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
            method.setAccessible(true);
            methodCache.put(fullName, method); // Add it to the cache
        }
        return method;
    }

    public static Method findMethod(Class<?> cla, String name, Class<?>[] params) {
        return findMethod(cla, name, params, true);
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
    public static Vector blockPositionToVector(Object pos)  {
        return new Vector(
            (int) ReflectUtils.runMethod(pos, "getX"),
            (int) ReflectUtils.runMethod(pos, "getY"),
            (int) ReflectUtils.runMethod(pos, "getZ")
        );
    }

    // Sets a field in the given object, even if it is private (class is automatically determined)
    public static void setField(Object obj, String name, Object value)    {
        setField(obj, obj.getClass(), name, value);
    }

    // Call this method if you need to override the class, for instance if there are two private fields with the same name
    public static void setField(Object obj, Class<?> cla, String name, Object value)    {
        try {
            Field field = findField(obj, name, cla);
            field.set(obj, value);
        }   catch(IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    // Gets the value of the named field. Will return null if the field doesn't exist, or is of the wrong type
    public static Object getField(Object obj, Class<?> cla, String name)   {
        try {
            Field field = findField(obj, name, cla);
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
            Method method = findMethod(cla, name, params);
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

    public static Object createBlockPosition(Location location) {
        return createBlockPosition(location.toVector());
    }

    public static Object createBlockPosition(Vector vec)    {
        return newInstance("BlockPosition", new Class[]{int.class, int.class, int.class},
                                            new Object[]{vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()});
    }

    private static Map<Vector, BlockFace> vectorToBlockFace = getBlockFaces();
    private static Map<Vector, BlockFace> getBlockFaces()   {
        // Loop through each variant of BlockFace, and add it to the map
        Map<Vector, BlockFace> map = new HashMap<>();
        for(Object variant : ReflectUtils.getClass("org.bukkit.block.BlockFace").getEnumConstants())    {
            BlockFace face = (BlockFace) variant;
            map.put(getDirection(face), face);
        }
        return map;
    }

    // There is no getDirection method on BlockFace in version 1.12.2, so we use this instead
    public static Vector getDirection(BlockFace face)  {
        return new Vector(face.getModX(), face.getModY(), face.getModZ()).normalize();
    }

    public static BlockFace getBlockFace(Vector direction)  {
        return vectorToBlockFace.get(direction);
    }

    public static ItemStack addItemNBTTag(ItemStack item, String key, String value) {
        Class<?> craftItemStack = ReflectUtils.getBukkitClass("inventory.CraftItemStack");
        // Get the native minecraft version of the item stack
		Object nmsItem = ReflectUtils.runMethod(null, craftItemStack, "asNMSCopy", new Class[]{ItemStack.class}, new Object[]{item});

		// Get the NBT tag, or create one if the item doesn't have one
		Object itemTag = ((boolean) ReflectUtils.runMethod(nmsItem, "hasTag")) ? ReflectUtils.runMethod(nmsItem, "getTag") : ReflectUtils.newInstance("NBTTagCompound");
        
        Object stringTag = ReflectUtils.newInstance("NBTTagString", new Class[]{String.class}, new Object[]{value});
        ReflectUtils.runMethod(itemTag, "set", new Class[]{String.class, ReflectUtils.getMcClass("NBTBase")}, new Object[]{key, stringTag}); // Set the value

        // Return the bukkit version of the itemstack
		return (ItemStack) ReflectUtils.runMethod(null, craftItemStack, "asBukkitCopy", new Class[]{ReflectUtils.getMcClass("ItemStack")}, new Object[]{nmsItem});
    }

    // Gets the value of a String NBT tag from the item
	// Returns null if it doesn't exist
	public static String getItemNbtTag(ItemStack item, String key)	{
        Class<?> craftItemStack = ReflectUtils.getBukkitClass("inventory.CraftItemStack");
		// Get the NMS itemstack
		Object nmsItem = ReflectUtils.runMethod(null, craftItemStack, "asNMSCopy", new Class[]{ItemStack.class}, new Object[]{item});

		if(!(boolean) ReflectUtils.runMethod(nmsItem, "hasTag")) {return null;} // Return null if it has no NBT data
		Object itemTag = ReflectUtils.runMethod(nmsItem, "getTag"); // Otherwise, get the item's NBT tag

		return (String) ReflectUtils.runMethod(itemTag, "getString", new Class[]{String.class}, new Object[]{key}); // Return the value of the key
    }
    
    // This works fine on modern versions
    public static Object getNMSData(Material mat)  {
        Object block = ReflectUtils.runMethod(null, ReflectUtils.getBukkitClass("util.CraftMagicNumbers"), "getBlock", new Class[]{Material.class}, new Object[]{mat});
        return ReflectUtils.getField(block, "blockData");
    }

    // Uses the handle method of a CraftBlockData to get the NMS data
    public static Object getNMSData(BlockData data) {
        return ReflectUtils.runMethod(data, ReflectUtils.getBukkitClass("block.data.CraftBlockData"), "getState", new Class[]{}, new Object[]{});
    }

    // This won't work properly on non-legacy versions!
    @SuppressWarnings("deprecation")
    public static Object getNMSData(Material mat, byte data)    {
        int combinedId = mat.getId() + (data << 12);
        return ReflectUtils.runMethod(null, ReflectUtils.getMcClass("Block"), "getByCombinedId", new Class[]{int.class}, new Object[]{combinedId});
    }

    private static Method getDataMethod = findGetDataMethod();
    private static Method findGetDataMethod()  {
        if(ReflectUtils.isLegacy)   {
            return ReflectUtils.findMethod(ReflectUtils.getMcClass("Block"), "getByCombinedId", new Class[]{int.class});
        }   else    {
            return ReflectUtils.findMethod(ReflectUtils.getBukkitClass("block.CraftBlockState"), "getHandle", new Class[]{});
        }
    }

    // Finds the NMS IBlockData from a bukkit block state
    @SuppressWarnings("deprecation")
    public static Object getNMSData(BlockState state)   {
        try {
            // Use the combinedId to get IBlockData in legacy versions, or just use getHandle on a BlockState in modern versions
            if(ReflectUtils.isLegacy)    {
                int combined = state.getType().getId() + (state.getRawData() << 12);
                return getDataMethod.invoke(null, combined);
            }   else    {
                return getDataMethod.invoke(state);
            }
        }   catch(ReflectiveOperationException ex)  {
            ex.printStackTrace();
            return null;
        }
    }

    // The combined ID of IBlockData is only used for serialization, since this is what PacketDataSerializer uses.
    // It's also not really possible to easily get back the right IBlockData from a material and data value, since some modern blocks get messed up/colours changed.
    private static Method getCombinedIdMethod = findMethod(getMcClass("Block"), "getCombinedId", new Class[]{getMcClass("IBlockData")});
    private static Method getFromCombinedIdMethod = findMethod(getMcClass("Block"), "getByCombinedId", new Class[]{int.class});
    public static int NMSDataToCombinedId(Object blockData) {
        try {
            return (int) getCombinedIdMethod.invoke(null, blockData);
        }   catch(ReflectiveOperationException ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static Object combinedIdToNMSData(int combinedId) {
        try {
            return getFromCombinedIdMethod.invoke(null, combinedId);
        }   catch(ReflectiveOperationException ex) {
            ex.printStackTrace();
            return -1;
        }
    }
}