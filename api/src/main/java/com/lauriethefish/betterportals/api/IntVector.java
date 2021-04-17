package com.lauriethefish.betterportals.api;

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
    public IntVector(@NotNull Location location) {
        this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Creates a new instance using the block coordinates of <code>vector</code>.
     * @param vector Coordinates for this block
     */
    public IntVector(@NotNull Vector vector) {
        this(vector.getX(), vector.getBlockY(), vector.getBlockZ());
    }

    /**
     * Converts this instance into a Bukkit {@link BlockVector}
     * @return The Bukkit vector.
     */
    public @NotNull BlockVector toVector() {
        return new BlockVector(x, y, z);
    }

    /**
     * Gets the coordinate at the center of the block represented by this vector.
     * @return The center coordinate of the block
     */
    public @NotNull Vector getCenterPos() {
        return new Vector(x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * Adds the coordinates of <code>other</code> to this instance,
     * @param other The vector to add to
     * @return A new instance.
     */
    public @NotNull IntVector add(@NotNull IntVector other) {
        return new IntVector(
            x + other.x,
            y + other.y,
            z + other.z
        );
    }

    /**
     * Adds <code>x</code>, <code>y</code> and <code>z</code> and this vector's coordinates to a new vector.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return A new instance
     */
    public @NotNull IntVector add(int x, int y, int z) {
        return new IntVector(
            this.x + x,
            this.y + y,
            this.z + z
        );
    }

    /**
     * Subtracts the coordinates of <code>other</code> from this instance,
     * @param other The vector to add to
     * @return A new instance.
     */
    public @NotNull IntVector subtract(@NotNull IntVector other) {
        return new IntVector(
            x - other.x,
            y - other.y,
            z - other.z
        );
    }

    /**
     * Subtracts <code>x</code>, <code>y</code> and <code>z</code> from this vector's coordinates and puts the result into a new vector.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return A new instance
     */
    public @NotNull IntVector subtract(int x, int y, int z) {
        return new IntVector(
                this.x - x,
                this.y - y,
                this.z - z
        );
    }

    /**
     * Gets the block at the coordinates of this Vector, in world.
     * Throws an error if this is not a valid block position
     * @param world World to get the block in
     * @return The block at this position
     */
    public @NotNull Block getBlock(@NotNull World world) {
        return world.getBlockAt(x, y, z);
    }

    /**
     * Converts this instance into a Location
     * @param world World for the Location
     * @return The Location
     */
    public @NotNull Location toLocation(@NotNull World world) {
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

    public @NotNull IntVector clone() {
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
