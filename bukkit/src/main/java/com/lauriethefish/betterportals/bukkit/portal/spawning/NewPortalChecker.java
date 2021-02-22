package com.lauriethefish.betterportals.bukkit.portal.spawning;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.config.WorldLink;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds new valid portal positions in a chunk
 *
 * Valid portal spawn positions are defined here as follows:
 * <li>The portal base must be solid blocks</li>
 * <li>The area above the portal base, but not the portal roof (the top of the frame), must be air blocks.</li>
 * <li>There aren't any portals within {@link PortalSpawnConfig#getMinimumPortalSpawnDistance()} blocks.</li>
 * These rules are on the line of the portal frame, as well as the two blocks going through the portal (represented as the Z below)
 */
@Singleton
public class NewPortalChecker implements IChunkChecker  {
    private static final PortalDirection[] CHECKED_DIRECTIONS = new PortalDirection[] {
            PortalDirection.NORTH,
            PortalDirection.EAST
    };

    private final IPortalManager portalManager;
    private final PortalSpawnConfig spawnConfig;

    @Inject
    public NewPortalChecker(IPortalManager portalManager, PortalSpawnConfig spawnConfig) {
        this.portalManager = portalManager;
        this.spawnConfig = spawnConfig;
    }

    @Override
    public @Nullable PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context) {
        PortalSpawnPosition currentClosest = null;
        double closestDistance = Double.POSITIVE_INFINITY;

        WorldLink link = context.getWorldLink();
        for(int y = link.getMinSpawnY(); y < link.getMaxSpawnY(); y++) {
            for(int x = 0; x < 16; x++) {
                for(int z = 0; z < 16; z++) {
                    Location blockPos = chunk.getBottomLeft().add(x, y, z);
                    // Do this here to avoid the expensive check if at-all possible
                    double distance = blockPos.distance(context.getPreferredLocation());
                    if(distance >= closestDistance) {continue;}

                    // Make sure to check both directions for a valid spawn position
                    for(PortalDirection direction : CHECKED_DIRECTIONS) {
                        if(isValidPortalSpawnPosition(blockPos, direction, context.getSize())) {
                            closestDistance = distance;
                            currentClosest = new PortalSpawnPosition(blockPos, context.getSize(), direction);
                        }
                    }
                }
            }
        }

        return currentClosest;
    }

    public boolean isValidPortalSpawnPosition(Location location, PortalDirection direction, Vector size) {
        size = new Vector(size.getX() + 1, size.getY() + 1, 0.0);

        for(int z = -1; z <= 1; z++) {
            for (int x = 0; x <= size.getX(); x++) {
                for (int y = 0; y <= size.getY(); y++) {
                    Vector frameRelativePos = new Vector(x, y, z);

                    Location blockPos = location.clone().add(direction.swapVector(frameRelativePos));
                    Material type = blockPos.getBlock().getType();

                    boolean isFrame = x == 0 || y == 0 || x == size.getX() || y == size.getY();

                    if ((!isFrame) && !MaterialUtil.isAir(type)) { // Portal block positions must be air
                        return false;
                    }
                    if (y == 0 && (!type.isSolid())) { // The floor blocks must be solid
                        return false;
                    }
                }
            }
        }

        // Make sure that there aren't any other portals too close
        boolean isFarEnoughSpaced = portalManager.findClosestPortal(location, spawnConfig.getMinimumPortalSpawnDistance()) == null;
        // Don't spawn portals outside the world border!
        boolean isInsideWorldBorder = location.getWorld().getWorldBorder().isInside(location);

        return isFarEnoughSpaced && isInsideWorldBorder;
    }
}
