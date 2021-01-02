package com.lauriethefish.betterportals.bukkit.chunkloading.chunkpos;

import org.bukkit.Location;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

// ChunkAreaIterator that goes in a simple square
public class ChunkAreaIterator implements Iterator<ChunkPosition>, Cloneable {
    private ChunkPosition low;
    private ChunkPosition high;
    private ChunkPosition currentPos;

    // Makes an inclusive spiral chunk iterator, starting at the midpoint of these two chunks
    public ChunkAreaIterator(ChunkPosition low, ChunkPosition high) {
        this.low = low;
        this.high = high;
        currentPos = low.clone();
    }

    public ChunkAreaIterator(Location low, Location high) {
        this(new ChunkPosition(low), new ChunkPosition(high));
    }

    @Override
    public boolean hasNext() {
        return currentPos.x < high.x || currentPos.z < high.z;
    }

    @Override
    public ChunkPosition next() {
        if (currentPos.x < high.x) {
            currentPos.x++; // If we are not at the end of a row, move us 1 across
        } else if (currentPos.z < high.z) { // If we are at the end of a row, but there a columns left
            // Increment the column, and set the row to the start
            currentPos.z++;
            currentPos.x = low.x;
        } else {
            throw new NoSuchElementException();
        }

        return currentPos.clone();
    }

    // Returns a new area iterator with the same initial parameters as this one
    @Override
    public ChunkAreaIterator clone() {
        return new ChunkAreaIterator(low, high);
    }

    // Adds all of the chunks in this area to a set
    public void addAll(Set<ChunkPosition> set) {
        while (this.hasNext()) {
            set.add(this.next());
        }
    }

    // Removes all of the chunks in this area from a set
    public void removeAll(Set<ChunkPosition> set) {
        while (this.hasNext()) {
            set.remove(this.next());
        }
    }
}
