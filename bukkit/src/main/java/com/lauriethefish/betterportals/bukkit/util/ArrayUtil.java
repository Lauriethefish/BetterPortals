package com.lauriethefish.betterportals.bukkit.util;

import java.util.Arrays;

public class ArrayUtil {
    /**
     * Removes the first element from <code>array</code>.
     * @param array Array to have its first element removed.
     * @param <T> Type of the array.
     * @return A new array with the first element removed.
     * @throws IllegalArgumentException if the array is empty.
     */
    public static <T> T[] removeFirstElement(T[] array) {
        if(array.length == 0) {throw new IllegalArgumentException("Cannot remove element of empty array");}
        return Arrays.copyOfRange(array, 1, array.length);
    }

    /**
     * Removes the last element from <code>array</code>.
     * @param array Array to have its last element removed.
     * @param <T> Type of the array.
     * @return A new array with the last element removed.
     * @throws IllegalArgumentException if the array is empty.
     */
    public static <T> T[] removeLastElement(T[] array) {
        if(array.length == 0) {throw new IllegalArgumentException("Cannot remove element of empty array");}
        return Arrays.copyOf(array, array.length - 1);
    }
}
