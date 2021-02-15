package com.lauriethefish.betterportals.shared.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionUtil {
    private static class MethodInfo {
        private final Class<?> klass;
        private final String name;
        private final Class<?>[] args;

        public MethodInfo(Class<?> klass, String name, Class<?>[] args) {
            this.klass = klass;
            this.name = name;
            this.args = args;
        }

        @Override
        public int hashCode() {
            return Objects.hash(klass, name, args);
        }
    }

    private static class FieldInfo {
        private final Class<?> klass;
        private final String name;

        public FieldInfo(Class<?> klass, String name) {
            this.klass = klass;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(klass, name);
        }
    }

    // Cache classes, fields and methods for performance
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final Map<FieldInfo, Field> fieldCache = new ConcurrentHashMap<>();
    private static final Map<MethodInfo, Method> methodCache = new ConcurrentHashMap<>();

    // In the below methods, ReflectiveOperationExceptions are simply boxed as RuntimeExceptions
    // This is done mostly for convenience.

    // Fetches a class, caching previous times it has been fetched
    public static Class<?> findClass(String name) {
        Class<?> cached = classCache.get(name);
        if(cached != null) {return cached;}

        try {
            Class<?> fetched = Class.forName(name);
            classCache.put(name, fetched);
            return fetched;
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException("Reflective operation failed", ex);
        }
    }

    // Finds a field, caching it for next time the same field is requested
    // This will check super classes for the field as well
    public static Field findField(Class<?> parentClass, String name) {
        FieldInfo fieldInfo = new FieldInfo(parentClass, name);

        Field cached = fieldCache.get(fieldInfo);
        if(cached != null) {return cached;}

        Class<?> klass = parentClass;
        // Keep looping until there are no superclasses left
        while(klass != null) {
            try {
                Field fetched = klass.getDeclaredField(name);
                fetched.setAccessible(true);
                fieldCache.put(fieldInfo, fetched);
                return fetched;
            } catch (NoSuchFieldException ex) {
                klass = klass.getSuperclass(); // If the field is not found, try to find it in the next class up
            }
        }

        // If the field wasn't found in any superclass, throw an exception
        throw new RuntimeException(new NoSuchFieldException("Field " + name + " does not exist in " + parentClass.getName()));
    }

    public static Method findMethod(Class<?> parentClass, String name, Class<?>[] argTypes) {
        MethodInfo methodInfo = new MethodInfo(parentClass, name, argTypes);

        Method cached = methodCache.get(methodInfo);
        if(cached != null) {return cached;}

        Class<?> klass = parentClass;
        while(klass != null) {
            try {
                Method fetched = klass.getDeclaredMethod(name, argTypes);
                fetched.setAccessible(true);
                methodCache.put(methodInfo, fetched);
                return fetched;
            }   catch(NoSuchMethodException ex) {
                klass = klass.getSuperclass();
            }
        }

        // If the method wasn't found in any superclass, throw an exception
        throw new RuntimeException(new NoSuchMethodException("Method " + name + " does not exist in " + parentClass.getName()));
    }

    public static Method findMethod(Class<?> parentClass, String name) {
        return findMethod(parentClass, name, new Class[0]);
    }

    // Runs a public or private method
    public static Object runMethod(Object instance, Class<?> klass, String name, Class<?>[] argTypes, Object[] args) {
        Method method = findMethod(klass, name, argTypes);
        try {
            return method.invoke(instance, args);
        }   catch(InvocationTargetException ex) {
            throw new RuntimeException(ex.getCause()); // Forward on exceptions during the method invocation
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException("Reflective method invocation error", ex);
        }
    }

    public static Object runMethod(Object instance, String name, Class<?>[] argTypes, Object... args) {
        return runMethod(instance, instance.getClass(), name, argTypes, args);
    }

    public static Object runMethod(Object instance, String name) {
        return runMethod(instance, name, new Class[0]);
    }

    // Gets a public or private field
    public static Object getField(Object instance, Class<?> klass, String name) {
        Field field = findField(klass, name);
        try {
            return field.get(instance);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException("Reflective field get error", ex);
        }
    }

    public static Object getField(Object instance, String name) {
        return getField(instance, instance.getClass(), name);
    }

    // Sets a public or private field
    public static void setField(Object instance, Class<?> klass, String name, Object value) {
        Field field = findField(klass, name);
        try {
            field.set(instance, value);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException("Reflective field set error", ex);
        }
    }

    // Instantiates a new instance of klass using the specified parameters
    // Returns the new object
    public static Object newInstance(Class<?> klass, Class<?>[] argTypes, Object... args) {
        try {
            Constructor<?> ctor = klass.getDeclaredConstructor(argTypes);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException("Reflective object instantiation error", ex);
        }
    }

    public static Object newInstance(Class<?> klass) {
        return newInstance(klass, new Class[0]);
    }
}
