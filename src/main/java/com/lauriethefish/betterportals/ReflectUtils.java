package com.lauriethefish.betterportals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public class ReflectUtils {
    private static String minecraftClassPath = null;
    private static String craftbukkitClassPath = null;
    // To increase performance, store the class name mappings for later
    private static Map<String, Class<?>> classCache = new HashMap<>();
    
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
    
    // Sets a field in the given object, even if it is private
    public static void setField(Object obj, String name, Object value)    {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        }   catch(NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    // Gets the value of the named field. Will return null if the field doesn't exist, or is of the wrong type
    public static Object getField(Object obj, Class<?> cla, String name)   {
        try {
            Field field = cla.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(obj);
        }   catch(NoSuchFieldException | IllegalAccessException | ClassCastException ex) {
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
            Method method = cla.getDeclaredMethod(name, params);
            method.setAccessible(true);
            Object result = method.invoke(obj, args);
            return result;
        }   catch(InvocationTargetException | IllegalAccessException | NoSuchMethodException ex)   {
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