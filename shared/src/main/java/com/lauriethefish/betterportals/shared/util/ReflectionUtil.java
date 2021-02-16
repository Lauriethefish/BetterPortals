package com.lauriethefish.betterportals.shared.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Convenience methods for handling reflection for cross-version NMS support.
 * Note: All exceptions while handling reflective operations in this class are boxed as {@link ReflectionException}.
 */
public class ReflectionUtil {
    public static class ReflectionException extends RuntimeException    {
        public ReflectionException(Throwable cause) {
            super(cause);
        }

        public ReflectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

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

    /**
     * Cache classes, fields and methods for performance
     */
    private static final Map<String, Class<?>> classCache = new ConcurrentHashMap<>();
    private static final Map<FieldInfo, Field> fieldCache = new ConcurrentHashMap<>();
    private static final Map<MethodInfo, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Fetches a class, or returns the cached version if cached.
     * @param name The fully qualified name of the class
     * @return The fetched class.
     * @throws ReflectionException If finding the class failed.
     */
    public static @NotNull Class<?> findClass(@NotNull String name) {
        Class<?> cached = classCache.get(name);
        if(cached != null) {return cached;}

        try {
            Class<?> fetched = Class.forName(name);
            classCache.put(name, fetched);
            return fetched;
        }   catch(ReflectiveOperationException ex) {
            throw new ReflectionException("Failed to fetch class", ex);
        }
    }

    /**
     * Finds a field, or returns the cached version.
     * @param parentClass The class that the field is in
     * @param name The name of the field
     * @return The fetched field.
     * @throws ReflectionException If the field could not be found
     */
    public static @NotNull Field findField(@NotNull Class<?> parentClass, @NotNull String name) {
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
        throw new ReflectionException(new NoSuchFieldException("Field " + name + " does not exist in " + parentClass.getName()));
    }

    /**
     * Finds a method in a class, or returns the cached version.
     * @param parentClass The class that the method is in
     * @param name The name of the method
     * @param argTypes Each argument type of the method
     * @return The fetched method
     * @throws ReflectionException If the method could not be found.
     */
    public static @NotNull Method findMethod(@NotNull Class<?> parentClass, @NotNull String name, @NotNull Class<?>[] argTypes) {
        MethodInfo methodInfo = new MethodInfo(parentClass, name, argTypes);

        Method cached = methodCache.get(methodInfo);
        if (cached != null) {
            return cached;
        }

        Class<?> klass = parentClass;
        while (klass != null) {
            try {
                Method fetched = klass.getDeclaredMethod(name, argTypes);
                fetched.setAccessible(true);
                methodCache.put(methodInfo, fetched);
                return fetched;
            } catch (NoSuchMethodException ex) {
                klass = klass.getSuperclass();
            }
        }

        // If the method wasn't found in any superclass, throw an exception
        throw new ReflectionException(new NoSuchMethodException("Method " + name + " does not exist in " + parentClass.getName()));
    }

    /**
     * Finds a method in a class, with no arguments and the name given.
     * @param parentClass The class that the method is in
     * @param name The name of the method
     * @return The fetched method
     * @throws ReflectionException If the method cannot be found
     */
    public static @NotNull Method findMethod(@NotNull Class<?> parentClass, @NotNull String name) {
        return findMethod(parentClass, name, new Class[0]);
    }

    /**
     * Finds the correct method in the class, then runs it and returns the result.
     * @param instance The Object to run this method on, or <code>null</code> if a static method/
     * @param klass The class of <code>instance</code>, or another subclass. Used if you need to run a specific private method that the superclass has with the same name.
     * @param name The name of the method
     * @param argTypes The argument types of the method to call. <i>NOT</i> of <code>args</code>.
     * @param args The arguments to pass to the method
     * @return The result of the method, or <code>null</code> if declared <code>void</code> or returns null.
     * @throws ReflectionException If the method cannot be found, or if a checked exception is thrown while it runs
     */
    public static Object runMethod(@Nullable Object instance, @NotNull Class<?> klass, @NotNull String name, @NotNull Class<?>[] argTypes, @Nullable Object[] args) {
        Method method = findMethod(klass, name, argTypes);
        try {
            return method.invoke(instance, args);
        }   catch(InvocationTargetException ex) {
            if(ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }   else    {
                throw new ReflectionException("Checked exception was caught while running method reflectively", ex.getCause());
            }
        }   catch(ReflectiveOperationException ex) {
            throw new ReflectionException("Reflective method invocation error", ex);
        }
    }

    /**
     * Finds the correct method in the class, then runs it and returns the result.
     * @param instance The Object to run this method on.
     * @param name The name of the method
     * @param argTypes The argument types of the method to call. <i>NOT</i> of <code>args</code>.
     * @param args The arguments to pass to the method
     * @return The result of the method, or <code>null</code> if declared <code>void</code> or returns null.
     * @throws ReflectionException If the method cannot be found, or if a checked exception is thrown while it runs
     */
    public static Object runMethod(@NotNull Object instance, @NotNull String name, @NotNull Class<?>[] argTypes, @Nullable Object... args) {
        return runMethod(instance, instance.getClass(), name, argTypes, args);
    }

    /**
     * Runs a method with zero arguments on <code>instance</code>.
     * @param instance The Object to run this method on, or <code>null</code> if a static method.
     * @param name The name of the method
     * @return The result of the method, or <code>null</code> if declared <code>void</code> or returns null.
     * @throws ReflectionException If the method cannot be found, or if a checked exception is thrown while it runs.
     */
    public static Object runMethod(@NotNull Object instance, @NotNull String name) {
        return runMethod(instance, name, new Class[0]);
    }

    /**
     * Gets the value of a field with any access modifier.
     * @param instance The instance to get the field on, or <code>null</code> if it's static.
     * @param klass The class to find the field on - can be used to get a specific private method if a superclass has one with the same name.
     * @param name The name of the field
     * @return The value of the field.
     * @throws ReflectionException If the field cannot be found
     */
    public static Object getField(@Nullable Object instance, @NotNull Class<?> klass, @NotNull String name) {
        Field field = findField(klass, name);
        try {
            return field.get(instance);
        }   catch(ReflectiveOperationException ex) {
            throw new ReflectionException("Reflective field get error", ex);
        }
    }

    /**
     * Gets the value of a field with any access modifier.
     * @param instance The instance to get the field on, or <code>null</code> if it's static.
     * @param name The name of the field
     * @return The value of the field.
     * @throws ReflectionException If the field cannot be found
     */
    public static Object getField(@NotNull Object instance, @NotNull String name) {
        return getField(instance, instance.getClass(), name);
    }

    /**
     * Sets the value of a field with any access modifier.
     * @param instance The instance to set the field to, or <code>null</code> if it's static.
     * @param klass The class to find the field on - can be used to get a specific private method if a superclass has one with the same name.
     * @param name The name of the field
     * @param value The value to set the field to
     * @throws ReflectionException If the field cannot be found
     */
    public static void setField(@Nullable Object instance, @NotNull Class<?> klass, @NotNull String name, @Nullable Object value) {
        Field field = findField(klass, name);
        try {
            field.set(instance, value);
        }   catch(ReflectiveOperationException ex) {
            throw new ReflectionException("Reflective field set error", ex);
        }
    }

    /**
     * Sets the value of a field with any access modifier.
     * @param instance The instance to set the field to, or <code>null</code> if it's static.
     * @param name The name of the field
     * @param value The value to set the field to
     * @throws ReflectionException If the field cannot be found
     */
    public static void setField(@NotNull Object instance, @NotNull String name, @Nullable Object value) {
        setField(instance, instance.getClass(), name, value);
    }

    /**
     * Creates a new instance of <code>klass</code> with the specified arguments.
     * @param klass The class to create an instance of.
     * @param argTypes The constructor's argument types.
     * @param args The arguments to pass to the constructor
     * @return The new instance
     * @throws ReflectionException If the constructor wasn't found, or any other reflective exception.
     */
    public static @NotNull Object newInstance(@NotNull Class<?> klass, @NotNull Class<?>[] argTypes, @Nullable Object... args) {
        try {
            Constructor<?> constructor = klass.getDeclaredConstructor(argTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        }   catch(ReflectiveOperationException ex) {
            throw new ReflectionException("Reflective object instantiation error", ex);
        }
    }

    /**
     * Creates a new instance of <code>klass</code>, it should have a no-args constructor.
     * @param klass The class to create an instance of.
     * @return The new instance
     * @throws ReflectionException If the class doesn't have a no-args constructor, or for any other reason why reflection may fail.
     */
    public static @NotNull Object newInstance(@NotNull Class<?> klass) {
        return newInstance(klass, new Class[0]);
    }
}
