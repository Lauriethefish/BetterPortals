package com.lauriethefish.betterportals.bukkit.portal.spawning;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.chunk.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.bukkit.chunk.generation.IChunkGenerationChecker;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.config.WorldLink;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Finds valid existing portal frames within a chunk to avoid respawning portals when one already exists.
 * Valid existing portal frames are defined as follows:
 * <li>A portal block is one not on the corners, but on the rest of the frame or inside of the portal.</li>
 * <li>At least {@link ExistingPortalChecker#VALIDITY_THRESHOLD} proportion of these blocks must be obsidian/air blocks if inside the frame.</li>
 * <li>There must be no portals within the configured number of blocks.</li>
 * <li>There aren't any portals within {@link PortalSpawnConfig#getMinimumPortalSpawnDistance()} blocks.</li>
 */
@Singleton
public class ExistingPortalChecker implements IChunkChecker    {
    /**
     * Percentage of obsidian blocks/air blocks that must be present for it to be considered a valid portal frame
     */
    private static final double VALIDITY_THRESHOLD = 0.85;

    private static final PortalDirection[] CHECKED_DIRECTIONS = new PortalDirection[] {
            PortalDirection.NORTH,
            PortalDirection.EAST
    };
    private static final Vector[] XZ_CHECK_OFFSETS = new Vector[] {
        new Vector(0.0, 0.0, 0.0),
        new Vector(1.0, 0.0, 0.0),
        new Vector(-1.0, 0.0, 0.0),
        new Vector(0.0, 0.0, -1.0),
        new Vector(0.0, 0.0, 1.0),
    };

    private final IPortalManager portalManager;
    private final PortalSpawnConfig spawnConfig;
    private final IChunkGenerationChecker generationChecker;

    @Inject
    public ExistingPortalChecker(IPortalManager portalManager, PortalSpawnConfig spawnConfig, IChunkGenerationChecker generationChecker) {
        this.portalManager = portalManager;
        this.spawnConfig = spawnConfig;
        this.generationChecker = generationChecker;
    }

    @Override
    public PortalSpawnPosition findClosestInChunk(@NotNull ChunkPosition chunk, @NotNull PortalSpawningContext context) {
        if(!generationChecker.isChunkGenerated(chunk)) {
            return null;
        }

        int frameSize = context.getSize().getBlockY() + 2;
        Collection<Location> obsidianBlocks = searchForObsidianBlocks(chunk, frameSize, context.getWorldLink());

        PortalSpawnPosition closestPosition = null;
        double closestDistance = Double.POSITIVE_INFINITY;

        for(Location block : obsidianBlocks) {
            // Because the above may only check every frameSize(th) block, we have to check the surrounding areas for portals here
            // This ends up being faster, since most chunks have very little obsidian in them
            for(int yOffset = -frameSize; yOffset <= frameSize; yOffset++) {
                // We also must check surrounding blocks on the X/Z for portals, since otherwise it wouldn't work if the portal corners don't exist.
                for(Vector offset : XZ_CHECK_OFFSETS) {
                    Location offsetBlock = block.clone().add(offset);
                    offsetBlock.setY(offsetBlock.getY() + yOffset);
                    // Check the distance here to speed it up a bit if a location gets found
                    double distance = offsetBlock.distance(context.getPreferredLocation());
                    if (distance >= closestDistance) {continue;}

                    for (PortalDirection direction : CHECKED_DIRECTIONS) {
                        if (validPortalExists(offsetBlock, direction, context.getSize())) {
                            closestPosition = new PortalSpawnPosition(offsetBlock, context.getSize(), direction);
                            closestDistance = distance;
                        }
                    }
                }
            }
        }

        return closestPosition;
    }

    /**
     * We first do a (relatively) quick check for all the obsidian blocks in the chunk to make this faster.
     * @param yIncrement We can skip some of the blocks in the chunk for larger portals, because they are so tall we can skip even 1 out of 5 blocks and we'll always hit at least one obsidian
     * @param worldLink Used for the minimum and maximum spawn height
     * @return The list of obsidian blocks
     */
    private Collection<Location> searchForObsidianBlocks(ChunkPosition chunkPos, int yIncrement, WorldLink worldLink) {
        Collection<Location> result = new ArrayList<>();

        Chunk chunk = chunkPos.getChunk();
        // The yIncrement is used to skip checking some Y levels as for 5 tall portals, we only need to check every fifth Y coordinate to guarantee that we hit one of the obsidian blocks
        // Then we can just search the surrounding area for portal positions afterwards
        // This helps performance a ton
        for(int y = worldLink.getMinSpawnY(); y < worldLink.getMaxSpawnY(); y += yIncrement) {
            for(int z = 0; z < 16; z += 1) {
                for(int x = 0; x < 16; x += 1) {
                    Block block = chunk.getBlock(x, y, z);
                    if(block.getType() == Material.OBSIDIAN) {
                        result.add(block.getLocation());
                    }
                }
            }
        }

        return result;
    }

    /**
     * 
     * @param location The bottom left corner of the portal (lowest of the coordinates)
     * @param direction Direction of the portal
     * @param size Size to test
     * @return Whether a portal frame already exists at <code>location</code>.
     */
    private boolean validPortalExists(Location location, PortalDirection direction, Vector size) {
        // We need the size of the actual portal frame, not the portal window
        size = new Vector(size.getX() + 1.0, size.getY() + 1.0, 0.0);

        // Find which blocks of the portal frame are correct
        int blocks = 0;
        int validBlocks = 0;
        for(int x = 0; x <= size.getX(); x++) {
            for(int y = 0; y <= size.getY(); y++) {
                // Corner blocks don't need to be taken into account - these aren't required
                if((x == 0 && y == 0) || (x == size.getX() && y == 0) || (x == 0 && y == size.getY()) || (x == size.getX() && y == size.getY())) {
                    continue;
                }

                boolean isFrame = x == 0 || y == 0 || x == size.getX() || y == size.getY();

                Vector offset = direction.swapVector(new Vector(x, y, 0.0));

                Location blockPos = location.clone().add(offset);

                blocks++;
                Material type = blockPos.getBlock().getType();
                // Frame blocks must only be obsidian, interior blocks can be air or portal blocks
                if(isFrame) {
                    if(type == Material.OBSIDIAN) {
                        validBlocks++;
                    }
                }   else    {
                    if(type == Material.AIR || type == MaterialUtil.PORTAL_MATERIAL) {
                        validBlocks++;
                    }
                }
            }
        }

        double percentageValid = (double) validBlocks / (double) blocks;
        boolean isValid = percentageValid >= VALIDITY_THRESHOLD;
        // Make sure that there aren't any other portals too close
        boolean isFarEnoughSpaced = portalManager.findClosestPortal(location, spawnConfig.getMinimumPortalSpawnDistance()) == null;
        // Don't spawn portals outside the world border!
        boolean isInsideWorldBorder = location.getWorld().getWorldBorder().isInside(location);

        return isInsideWorldBorder && isFarEnoughSpaced && isValid;
    }
}
