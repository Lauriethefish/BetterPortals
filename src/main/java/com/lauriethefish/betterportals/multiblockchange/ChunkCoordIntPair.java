package com.lauriethefish.betterportals.multiblockchange;

import java.util.Objects;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.util.Vector;

// A wrapper over the coordinates of a chunk that automatically converts them to the NMS version
public class ChunkCoordIntPair {
    public int x;
    public int z;
    public ChunkCoordIntPair(Vector location) {
        x = location.getBlockX() >> 4;
        z = location.getBlockZ() >> 4;
    }

    // Makes an NMS ChunkCoordIntPair from this object
    public Object toNMS()   {
        return ReflectUtils.newInstance("ChunkCoordIntPair", new Class[]{int.class, int.class}, new Object[]{x, z});
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
    public int hashCode() {
        return Objects.hash(x, z);
    }
}