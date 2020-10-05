package com.lauriethefish.betterportals.multiblockchange;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// A wrapper over the coordinates of a chunk that automatically converts them to the NMS version
// This can also just be used to hold the coordinates of a chunk
@Getter 
@Setter
public class ChunkCoordIntPair {
    public World world; // This can be null
    public int x;
    public int z;

    public ChunkCoordIntPair(World world, int chunkX, int chunkZ) {
        this.world = world;
        x = chunkX;
        z = chunkZ;
    }

    public ChunkCoordIntPair(Location location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
        world = location.getWorld();
    }

    public ChunkCoordIntPair(Vector location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
    }

    public ChunkCoordIntPair(Chunk chunk)   {
        x = chunk.getX();
        z = chunk.getZ();
        world = chunk.getWorld();
    }

    public Chunk getChunk() {
        return world.getChunkAt(x, z);
    }

    public static Set<ChunkCoordIntPair> findArea(Location a, Location b)   {
        // Find the coordinates of the two locations
        ChunkCoordIntPair low = new ChunkCoordIntPair(a);
        ChunkCoordIntPair high = new ChunkCoordIntPair(b);

        // Loop through all the chunks between them and add them to the set
        Set<ChunkCoordIntPair> result = new HashSet<>();
        for(int z = low.z; z <= high.z; z++)    {
            for(int x = low.x; x <= high.x; x++)    {
                result.add(new ChunkCoordIntPair(a.getWorld(), x, z));   
            }
        }
        return result;
    }

    // Makes an NMS ChunkCoordIntPair from this object
    public Object toNMS()   {
        return ReflectUtils.newInstance("ChunkCoordIntPair", new Class[]{int.class, int.class}, new Object[]{x, z});
    }

    // Finds if this chunk has been generated before. This has the side effect of loading the chunk if it exists
    // Throws NullPointerException if world is null
    public boolean isGenerated()    {
        return world.loadChunk(x, z, false);
    }

    // Gets the location of the bottom left position of this chunk
    public Location getBottomLeft() {
        return new Location(world, x * 16, 0, z * 16);
    }

    // Automatically generated
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ChunkCoordIntPair)) {
            return false;
        }
        ChunkCoordIntPair chunkCoordIntPair = (ChunkCoordIntPair) o;
        return x == chunkCoordIntPair.x && z == chunkCoordIntPair.z;
    }

    @Override
    public String toString()    {
        return String.format("x: %d, z: %d", x, z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}