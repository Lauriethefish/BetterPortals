package com.lauriethefish.betterportals.bukkit.portal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;

import org.bukkit.Location;

// Structure for storing portals in maps based on their chunks
public class PortalChunkMap implements Iterable<Portal> {
    private Map<ChunkCoordIntPair, Map<Location, Portal>> map = new HashMap<>();

    // Iterator for looping through the Chunk -> Location -> Portal map
    public class PortalChunkMapIterator implements Iterator<Portal> {
        private Iterator<Map<Location, Portal>> chunkMapIterator = PortalChunkMap.this.map.values().iterator();
        private Map<Location, Portal> currentMap;
        private Iterator<Portal> currentIterator;

        @Override
        public boolean hasNext() {
            // If we have another chunk map to iterate, or there are elements left in this chunk map
            return currentIterator.hasNext() || chunkMapIterator.hasNext();
        }

        @Override
        public Portal next() {
            // If we are out of portals in this chunk, find the next chunk portal map and get a new iterator
            if(!currentIterator.hasNext()) {
                currentMap = chunkMapIterator.next();
                currentIterator = currentMap.values().iterator();
            }

            return currentIterator.next(); // Find the next protal
        }

        @Override
        public void remove() {
            currentIterator.remove(); // Remove the entry in this chunk map
            if(currentMap.isEmpty()) { // If the chunk map is empty, remove that too
                chunkMapIterator.remove();
            }
        }
    }

    public void addPortal(Portal portal) {
        // Find the location and chunk that the portal is in
        Location portalPos = portal.getOriginPos();
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(portalPos);

        // Check for an existing map for this chunk
        Map<Location, Portal> chunkMap = map.get(chunkPos);
        if (chunkMap == null) {
            chunkMap = new HashMap<>();
            map.put(chunkPos, chunkMap); // Add a new one if there isn't one already
        }

        chunkMap.put(portalPos, portal); // Add the portal to the chunk portal map
    }

    public void removePortal(Portal portal) {
        removePortal(portal.getOriginPos());
    }

    public void removePortal(Location loc) {
        // Find the location and chunk that the portal is in
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(loc);

        Map<Location, Portal> chunkMap = map.get(chunkPos);
        if(chunkMap == null) {return;}
        chunkMap.remove(loc);

        if(map.isEmpty()) {map.remove(chunkPos);} // Remove the chunk map if it is empty
    }

    // Returns the portal with its origin at location
    public Portal getPortal(Location location) {
        return map.get(new ChunkCoordIntPair(location)).get(location);
    }

    // Finds the closest portal within any range
    public Portal findClosestPortal(Location preferredPos) {
        return findClosestPortalLongDistance(preferredPos, Double.POSITIVE_INFINITY);
    }

    // Slow implementation for finding the closest portal that loops through all portals
    public Portal findClosestPortalLongDistance(Location preferredPos, double minDistance) {
        Portal closestPortal = null;
        double closestDistance = minDistance;

        for (Portal portal : this) {
            // Ignore portals that are in the wrong world
            if (portal.getOriginPos().getWorld() != preferredPos.getWorld()) {
                continue;
            }

            // If this portal is closer than the current closest, change the current closest
            double distance = portal.getOriginPos().distance(preferredPos);
            if (distance < closestDistance) {
                closestPortal = portal;
                closestDistance = distance;
            }
        }

        return closestPortal;
    }

    // Returns the total number of portals in all chunks
    public int totalCount() {
        int total = 0;
        for(Map<Location, Portal> chunkMap : map.values()) {
            total += chunkMap.size();
        }

        return total;
    }

    @Override
    public Iterator<Portal> iterator() {
        return new PortalChunkMapIterator();
    }
}
