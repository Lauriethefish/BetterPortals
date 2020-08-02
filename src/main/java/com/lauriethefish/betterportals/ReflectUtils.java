package com.lauriethefish.betterportals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class ReflectUtils {
    private static String minecraftClassPath = null;
    private static String craftbukkitClassPath = null;
    // To increase performance, store fields, classes and methods for later
    private static Map<String, Class<?>> classCache = new HashMap<>();
    private static Map<String, Field> fieldCache = new HashMap<>();
    private static Map<String, Method> methodCache = new HashMap<>();
    private static Material portalMaterial = null;

    private static Boolean isLegacy = null;
    public static boolean getIfLegacy() {
        // Test if this is a legacy version by checking if blocks have the getBlockData method, which was added in 1.13
        if(isLegacy == null)    {
            try {
                Block.class.getMethod("getBlockData", new Class[]{});
                isLegacy = false;
            }   catch(NoSuchMethodException ignored) {
                isLegacy = true;
            }
        }
        return isLegacy;
    }
    
    // Tries to find the given NMS class, without version dependence
    public static Class<?> getMcClass(String path)  {
        // Find the path of NMS classes if we haven't already
        if(minecraftClassPath == null)  {
            minecraftClassPath = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        }

        // Find if we have a cached version of this class
        Class<?> cachedClass = classCache.get(path);
        if(cachedClass == null) {
            // If we don't, find the class, then add it to the cache
            try {
                String fullPath = minecraftClassPath + path;
                Class<?> cla = Class.forName(fullPath);
                classCache.put(fullPath, cla);
                return cla;
            }   catch(ClassNotFoundException ex)    {
                ex.printStackTrace();
                return null;
            }
        }   else    {
            return cachedClass;
        }
    }
    // Tries to find the given CraftBukkit class, without version dependence
    public static Class<?> getBukkitClass(String path)  {
        // Find the path of NMS classes if we haven't already
        if(craftbukkitClassPath == null)  {
            craftbukkitClassPath = "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] + ".";
        }

        // Find if we have a cached version of this class
        Class<?> cachedClass = classCache.get(path);
        if(cachedClass == null) {
            // If we don't, find the class, then add it to the cache
            try {
                String fullPath = craftbukkitClassPath + path;
                Class<?> cla = Class.forName(fullPath);
                classCache.put(fullPath, cla);
                return cla;
            }   catch(ClassNotFoundException ex)    {
                ex.printStackTrace();
                return null;
            }
        }   else    {
            return cachedClass;
        }
    }

    // Attempts to find a field in the field cache, and gets it using reflection if it doesn't exist
    private static Field findField(Object obj, String name) {
        String fullName = String.format(String.format("%s.%s", obj.getClass().getName(), name));
        Field field = fieldCache.get(fullName);
        if(field == null)   { // Test if it is in the cache
            Class<?> currentClass = obj.getClass();
            while(currentClass != null && field == null) { // Keep looping until no superclass can be found
                try {
                    field = currentClass.getDeclaredField(name);
                }   catch(NoSuchFieldException ex)  {
                    currentClass = currentClass.getSuperclass();
                    if(currentClass == null)    { // Print the exception if no field was found, even in the highest superclass
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
    public static Material getPortalMaterial()  {
        if(portalMaterial == null)  {
            if(getIfLegacy())   {
                portalMaterial = Material.valueOf("PORTAL");
            }   else    {
                portalMaterial = Material.NETHER_PORTAL;
            }
        }
        return portalMaterial;
    }

    // Sets a field in the given object, even if it is private
    public static void setField(Object obj, String name, Object value)    {
        try {
            Field field = findField(obj, name);
            field.setAccessible(true);
            field.set(obj, value);
        }   catch(IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    // Gets the value of the named field. Will return null if the field doesn't exist, or is of the wrong type
    public static Object getField(Object obj, Class<?> cla, String name)   {
        try {
            Field field = findField(obj, name);
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
}