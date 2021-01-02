package com.lauriethefish.betterportals.bukkit.chunkloading.chunkpos;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class ChunkAreaIterator implements Iterator<ChunkPosition>, Iterable<ChunkPosition>, Cloneable {
    private ChunkPosition low;
    private ChunkPosition high;
    private ChunkPosition currentPos;

    ChunkAreaIterator(ChunkPosition low, ChunkPosition high) {
        this.low = low;
        this.high = high;
        currentPos = low.clone();
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

    // Revoves all of the chunks in this area to a set
    public void removeAll(Set<ChunkPosition> set) {
        while (this.hasNext()) {
            set.remove(this.next());
        }
    }

    @Override
    public Iterator<ChunkPosition> iterator() {
        return this;
    }
}
