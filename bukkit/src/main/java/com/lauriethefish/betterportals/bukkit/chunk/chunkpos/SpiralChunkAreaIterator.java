package com.lauriethefish.betterportals.bukkit.chunk.chunkpos;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

// Chunk area iterator that goes in a spiral from the centre of the low and high coordinates
public class SpiralChunkAreaIterator implements Iterator<ChunkPosition> {
    private static final ChunkPosition[] directions = new ChunkPosition[]   {
        new ChunkPosition(null, 1, 0),
        new ChunkPosition(null, 0, 1),
        new ChunkPosition(null, -1, 0),
        new ChunkPosition(null, 0, -1)
    };

    private final ChunkPosition currentPos;
    private final ChunkPosition low;
    private final ChunkPosition high;

    private int currentDirection = 0; // Current direction as an index of the above array
    private final ChunkPosition currentLength = new ChunkPosition(1, 1);

    private int movesLeft = 1;

    /**
     * Makes an inclusive spiral chunk iterator, starting at the midpoint of these two chunks
     * @param low Bottom-left chunk position, not axis aligned
     * @param high Top right chunk position, not axis aligned
     */
    public SpiralChunkAreaIterator(@NotNull ChunkPosition low, @NotNull ChunkPosition high) {
        if(low.world != high.world) {
            throw new IllegalArgumentException("The two positions must be in the same world");
        }

        currentPos = new ChunkPosition(low.world, (low.x + high.x) / 2, (low.z + high.z) / 2);
        this.low = low;
        this.high = high;
    }

    /**
     * Makes an iterator over the chunks between these locations, including the chunks that they're in
     * @param a Position A, not axis aligned
     * @param b Position B, not axis aligned
     */
    public SpiralChunkAreaIterator(Location a, Location b) {
        this(new ChunkPosition(a), new ChunkPosition(b));
    }

    @Override
    public boolean hasNext() {
        return !(currentPos.x > high.x || currentPos.z > high.z || currentPos.x < low.x || currentPos.z < low.z);
    }

    @Override
    public ChunkPosition next() {
        ChunkPosition result = currentPos.clone();

        if(movesLeft == 0) {
            // Increment the current length if going right or up
            if(currentDirection % 2 == 0) {
                movesLeft = currentLength.z;
                currentLength.x += 1;
            }   else if(currentDirection % 2 == 1) {
                movesLeft = currentLength.x;
                currentLength.z += 1;
            }

            // Switch to the next direction
            currentDirection += 1;
            if(currentDirection == directions.length) {currentDirection = 0;}
        }

        // Move to the next square
        movesLeft--;

        currentPos.x += directions[currentDirection].x;
        currentPos.z += directions[currentDirection].z;
        return result; // Return a copy of the current position
    }
}
