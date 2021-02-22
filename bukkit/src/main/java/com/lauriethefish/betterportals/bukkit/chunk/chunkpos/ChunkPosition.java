package com.lauriethefish.betterportals.bukkit.chunk.chunkpos;

import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Objects;

/**
 * Has some various utility functions for dealing with chunk coordinates
 */
@Getter 
@Setter
public class ChunkPosition implements Cloneable {
    public World world; // This can be null
    public int x;
    public int z;

    public ChunkPosition(World world, int chunkX, int chunkZ) {
        this.world = world;
        x = chunkX;
        z = chunkZ;
    }

    public ChunkPosition(int chunkX, int chunkZ) {
        x = chunkX;
        z = chunkZ;
    }

    public ChunkPosition(Location location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
        world = location.getWorld();
    }

    public ChunkPosition(Vector position) {
        x = position.getBlockX() >> 4;
        z = position.getBlockZ() >> 4;
    }

    public ChunkPosition(Chunk chunk)   {
        x = chunk.getX();
        z = chunk.getZ();
        world = chunk.getWorld();
    }

    public ChunkCoordIntPair toProtocolLib() {
        return new ChunkCoordIntPair(x, z);
    }

    public Chunk getChunk() {
        return world.getChunkAt(x, z);
    }

    public boolean isLoaded() {
        return world.isChunkLoaded(x, z);
    }

    /**
     * Finds the position at the bottom-left of this chunk.
     * @return The position of the block at the bottom-left of this chunk.
     */
    public Location getBottomLeft() {
        return new Location(world, x * 16, 0, z * 16);
    }

    public Location getCenterPos() {
        return getBottomLeft().add(8, 128, 8);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ChunkPosition)) {
            return false;
        }
        ChunkPosition chunkPosition = (ChunkPosition) o;
        return x == chunkPosition.x && z == chunkPosition.z && world == chunkPosition.world;
    }

    @Override
    public String toString()    {
        return String.format("x: %d, z: %d", x, z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public ChunkPosition clone() {
        try {
            return (ChunkPosition) super.clone();
        }   catch(CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }
}