package com.lauriethefish.betterportals.bukkit.block.rotation;

import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlockFaceUtil {
    private static final Map<Vector, BlockFace> vectorToBlockFace = new HashMap<>();

    static {
        // Add the direction of each enum variant to the map
        for (BlockFace variant : BlockFace.class.getEnumConstants()) {
            vectorToBlockFace.put(getDirection(variant), variant);
        }
    }

    // Not all versions have BlockFace.getDirection, so we implement it like this
    @NotNull
    private static Vector getDirection(BlockFace face) {
        Vector direction = new Vector(face.getModX(), face.getModY(), face.getModZ());
        direction.normalize();
        return direction;
    }

    /**
     * Rotates <code>face</code> by <code>matrix</code> by converting it into a direction.
     * @param face Block face to rotate
     * @param matrix Matrix to rotate by
     * @return The rotated face, or null if there is none
     */
    @Nullable
    public static BlockFace rotateFace(BlockFace face, Matrix matrix) {
        Vector oldRotation = getDirection(face);
        Vector newRotation = MathUtil.round(matrix.transform(oldRotation)); // Round since transforming via the matrix won't yield a perfect answer
        return vectorToBlockFace.get(newRotation);
    }
}
