package com.lauriethefish.betterportals.bukkit.math;

import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Represents the position of a block as the integer coordinates.
 * We don't use the ProtocolLib wrapper as I ideally don't want to use that everywhere.
 */
@Getter
@Setter
public class IntVector implements Cloneable, Serializable {
    private static final long serialVersionUID = 1;

    private int x;
    private int y;
    private int z;

    public IntVector(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a new instance rounding down the double coordinates.
     */
    public IntVector(double x, double y, double z) {
        this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Creates a new instance using the block coordinates of <code>location</code>.
     * @param location Coordinates for this block
     */
    public IntVector(Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Creates a new instance using the block coordinates of <code>vector</code>.
     * @param vector Coordinates for this block
     */
    public IntVector(Vector vector) {
        this(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
    }

    /**
     * Converts this instance into a Bukkit {@link BlockVector}
     * @return The Bukkit vector.
     */
    public BlockVector toVector() {
        return new BlockVector(x, y, z);
    }

    /**
     * Gets the coordinate at the center of the block represented by this vector.
     * @return The center coordinate of the block
     */
    @NotNull
    public Vector getCenterPos() {
        return new Vector(x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Adds the coordinates of <code>other</code> to this instance,
     * @param other The vector to add to
     * @return A new instance.
     */
    @NotNull
    public IntVector add(@NotNull IntVector other) {
        return new IntVector(
            x + other.x,
            y + other.y,
            z + other.z
        );
    }

    /**
     * Subtracts the coordinates of <code>other</code> from this instance,
     * @param other The vector to add to
     * @return A new instance.
     */
    @NotNull
    public IntVector subtract(@NotNull IntVector other) {
        return new IntVector(
            x - other.x,
            y - other.y,
            z - other.z
        );
    }

    /**
     * Gets the block at the coordinates of this Vector, in world.
     * Throws an error if this is not a valid block position
     * @param world World to get the block in
     * @return The block at this position
     */
    @NotNull
    public Block getBlock(@NotNull World world) {
        return world.getBlockAt(x, y, z);
    }

    /**
     * Gets the wrapper over the block at this coordinate's data.
     * @param world World to get the data in
     * @return The data wrapper
     */
    @NotNull
    public BlockData getData(@NotNull World world) {
        return BlockData.create(getBlock(world));
    }

    /**
     * Converts this instance into a Location
     * @param world World for the Location
     * @return The Location
     */
    @NotNull
    public Location toLocation(@NotNull World world) {
        return new Location(world, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof IntVector)) {return false;}

        IntVector otherVector = (IntVector) other;
        return otherVector.x == x && otherVector.y == y && otherVector.z == z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;

        return result;
    }

    public IntVector clone() {
        try {
            return (IntVector) super.clone();
        }   catch(CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public String toString() {
        return String.format("(%d, %d, %d)", x, y, z);
    }
}
