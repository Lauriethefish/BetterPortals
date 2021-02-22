package com.lauriethefish.betterportals.bukkit.block.rotation;

import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import org.jetbrains.annotations.NotNull;

/**
 * Responsible for rotating blocks from the destination to the origin (e.g. torches)
 * Since the destination and origin of a portal aren't necessarily in the same direction
 * Some blocks can't be rotated to the correct angle, so these just get automatically left as they are
 * This is an interface as we use BlockData in 1.13 and up, and MaterialData in 1.12
 */
public interface IBlockRotator {
    /**
     * Rotates <code>data</code> by <code>matrix</code>, or returns the origin if it isn't possible to rotate this data to the preferred rotation.
     * @param matrix The matrix to rotate by
     * @param data The data to rotate
     * @return A new data that is rotated, or the origin if unchanged.
     */
    @NotNull BlockData rotateByMatrix(@NotNull Matrix matrix, @NotNull BlockData data);
}
