package com.lauriethefish.betterportals.bukkit.util.nms;

import com.comphenix.protocol.wrappers.EnumWrappers.Direction;
import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience functions for handling NMS direction enums
 */
public class RotationUtil {
    private static final Map<Direction, Vector> directionToVector = new HashMap<>();
    private static final Map<Vector, Direction> vectorToDirection = new HashMap<>();

    private static final Map<Integer, Direction> idToDirection = new HashMap<>();
    private static final Map<Direction, Integer> directionToId = new HashMap<>();

    static {
        directionToVector.put(Direction.DOWN, new Vector(0.0, -1.0, 0.0));
        directionToVector.put(Direction.UP, new Vector(0.0, 1.0, 0.0));
        directionToVector.put(Direction.EAST, new Vector(1.0, 0.0, 0.0));
        directionToVector.put(Direction.WEST, new Vector(-1.0, 0.0, 0.0));
        directionToVector.put(Direction.NORTH, new Vector(0.0, 0.0, -1.0));
        directionToVector.put(Direction.SOUTH, new Vector(0.0, 0.0, 1.0));

        directionToVector.forEach((variant, direction) -> vectorToDirection.put(direction, variant));

        idToDirection.put(0, Direction.DOWN);
        idToDirection.put(1, Direction.UP);
        idToDirection.put(2, Direction.NORTH);
        idToDirection.put(3, Direction.SOUTH);
        idToDirection.put(4, Direction.WEST);
        idToDirection.put(5, Direction.EAST);

        idToDirection.forEach((id, direction) -> directionToId.put(direction, id));
    }

    /**
     * Converts the NMS EnumDirection variant into a direction vector
     * @param direction The direction to convert
     * @return The direction normal of this variant
     */
    @NotNull
    public static Vector getVector(@NotNull Direction direction) {
        return directionToVector.get(direction);
    }

    /**
     * Converts a direction normal into the NMS EnumDirection.
     * The vector is automatically normalised and then rounded
     * @param vector The vector to convert
     * @return The NMS direction variant, null if there isn't one
     */
    @Nullable
    public static Direction getDirection(@NotNull Vector vector) {
        vector = MathUtil.round(vector.clone().normalize());
        return vectorToDirection.get(vector);
    }

    /**
     * Gets the ID of the NMS EnumDirection.
     * (there is one ID per direction, this is used in some packets)
     * @param direction The direction to get the ID of
     * @return The direction ID
     */
    public static int getId(@NotNull Direction direction) {
        return directionToId.get(direction);
    }

    /**
     * Converts the direction ID into the NMS EnumDirection.
     * @param id The ID to convert
     * @return The {@link Direction} with ID <code>id</code>, or null if there is none.
     */
    @Nullable
    public static Direction getDirection(int id) {
        return idToDirection.get(id);
    }

    /**
     * Rotates the EnumDirection by the given Matrix.
     * @param direction The direction to rotate
     * @param matrix Matrix to rotate by
     * @return The rotated direction, or null if the matrix rotated <code>direction</code> into an unmapped direction.
     */
    @Nullable
    public static Direction rotateBy(Direction direction, Matrix matrix) {
        Vector finalDir = matrix.transform(getVector(direction));
        return getDirection(finalDir);
    }

    /**
     * Emulates MathHelper.d(float) in the game's code.
     * This is used for some, but not all, rotations in packets.
     * Some packets just directly multiply by 256 then divide by 360, then cast from float -> int -> byte, and avoid the clamping.
     * @param angle Angle to convert
     * @return Integer "clamped" value.
     */
    public static int getPacketRotationInt(float angle) {
        float limited = angle * 256.0f / 360.0f;
        int clamped = (int) limited;

        return limited < clamped ? clamped - 1 : clamped;
    }
}
