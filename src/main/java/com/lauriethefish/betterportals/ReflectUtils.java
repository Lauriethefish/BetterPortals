package com.lauriethefish.betterportals;

import java.lang.reflect.Field;

public class ReflectUtils {
    
    // Sets a field in the given object, even if it is private
    public static void setField(Object obj, Class<?> cla, String name, Object value)    {
        try {
            Field field = cla.getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        }   catch(NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    // Gets the value of the named field. Will return null if the field doesn't exist, or is of the wrong type
    @SuppressWarnings("unchecked")
    public static <T> T getField(Object obj, Class<?> cla, String name)   {
        try {
            Field field = cla.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(obj);
        }   catch(NoSuchFieldException | IllegalAccessException | ClassCastException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}