package com.lauriethefish.betterportals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

// Stores all of the attributes required for one direction of a portal
// Two of these should be created per portal, one for the effect on each side
public class PortalPos {
    private BetterPortals pl;
    // Corners of the portals collision box
    public Vector portalBL;
    public Vector portalTR;

    // The origin position and orientation of the portal
    public Location portalPosition;
    public PortalDirection portalDirection;

    // The destination position and orientation of the portal
    public Location destinationPosition;
    public PortalDirection destinationDirection;

    // The size of the portal's gateway in blocks
    public Vector portalSize;

    // Array of the blocks at the destination of the portal that need checking for visibility
    public ArrayList<BlockRaycastData> currentBlocks;
    private int ticksSinceLastRebuild = Integer.MAX_VALUE;

    public Collection<Entity> nearbyEntitiesOrigin = null;
    public Collection<Entity> nearbyEntitiesDestination = null;
    private int ticksSinceLastEntityCheck = Integer.MAX_VALUE;

    // Offsets for checking if a block needs to be rendered
    private static final int[][] offsets = new int[][]{
        new int[]{1, 0, 0},
        new int[]{-1, 0, 0},
        new int[]{0, 1, 0},
        new int[]{0, -1, 0},
        new int[]{0, 0, 1},
        new int[]{0, 0, -1},
    };

    // Constructor to generate the collision box for a given portal
    // NOTE: The portalPosition must be the EXACT center of the portal on the x, y and z
    public PortalPos(BetterPortals pl, Location portalPosition, PortalDirection portalDirection, 
                    Location destinationPosition, PortalDirection destinationDirection, Vector portalSize) {
        Vector portalPosVec = portalPosition.toVector();

        this.portalPosition = portalPosition;
        this.portalDirection = portalDirection;
        this.destinationPosition = destinationPosition;
        this.destinationDirection = destinationDirection;
        this.portalSize = portalSize;
        this.pl = pl;
        
        // Divide the size by 2 so it is the correct amount to subtract from the center to reach each corner
        // Then orient it so that is on the z if the portal is north/south
        Vector orientedSize = VisibilityChecker.orientVector(portalDirection, portalSize.clone().multiply(0.5));

        // Get the bottom left and top right blocks by subtracting our modified size
        Vector bottomLeftBlock = portalPosVec.clone().subtract(orientedSize);
        Vector topRightBlock = portalPosVec.clone().add(orientedSize);

        // Get the correct collision box from the config
        Vector collisionBox = pl.config.portalCollisionBox;

        // Set the portals collision box. NOTE: The east/west collision box is different to the north/south one
        portalBL = bottomLeftBlock.clone().subtract(VisibilityChecker.orientVector(portalDirection, collisionBox));
        portalTR = topRightBlock.clone().add(VisibilityChecker.orientVector(portalDirection, collisionBox));
    }

    // Forces this portal to recheck for entities next tick
    public void forceEntityUpdate()  {
        ticksSinceLastEntityCheck = Integer.MAX_VALUE;
    }

    // Updates the two lists of neaby entities, if it is time to
    public void updateNearbyEntities()   {
        if(ticksSinceLastEntityCheck < pl.config.entityCheckInterval)  {
            ticksSinceLastEntityCheck++;
        }
        ticksSinceLastEntityCheck = 0;
        
        nearbyEntitiesOrigin = portalPosition.getWorld().getNearbyEntities(portalPosition, pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
        nearbyEntitiesDestination = destinationPosition.getWorld().getNearbyEntities(destinationPosition, pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
    }

    // Transforms the vector input, which should be relative to the center of the portal,
    // to world coordinates at the destination of the portal
    public Vector applyTransformationsDestination(Vector input) {
        // If the two portals are facing different ways then the x and z coordinates have to be flipped
        // and one of them must be inverted. This is equivilient to a rotation about the origin of 90 degrees
        if(portalDirection != destinationDirection) {
            if(portalDirection == PortalDirection.EAST_WEST)    {
                input = new Vector(input.getZ() * -1.0, input.getY(), input.getX());
            }   else    {
                input = new Vector(input.getZ(), input.getY(), input.getX() * -1.0);
            }
        }

        // Add the destinationPosition to the input to transform the vector
        return input.add(destinationPosition.toVector());
    }

    // Transforms the vector input, which should be relative to the center of the portal
    // to world coordinates at the origin of the portal
    public Vector applyTransformationsOrigin(Vector input) {
        return input.add(portalPosition.toVector());
    }

    public boolean checkOriginAndDestination()  {
        PortalPos destination = pl.rayCastingSystem.portals.get(destinationPosition);
        // Remove the portal if either the origin or destination is broken
        if(!(checkIfStillActive() && destination.checkIfStillActive())) {
            remove();
            return false;
        }
        return true;
    }

    // Checks if the portal has been broken
    // This is used to remove the portal from the plugins list of active portals
    public boolean checkIfStillActive() {
        // Get the offset from the portals absolute center to the top left and bottom right corners of the portal blocks
        Vector subAmount = VisibilityChecker.orientVector(portalDirection, portalSize.clone().multiply(0.5).add(new Vector(0.0, 0.0, 0.5)));
        WorldBorder border = portalPosition.getWorld().getWorldBorder();

        // Check if the block at the centre of the portal is a portal block
        return portalPosition.getBlock().getType() == ReflectUtils.getPortalMaterial() &&
                // Check that the bottom left and top right of the portal are both inside the worldborder,
                // since portals outside the worldborder should be broken
                border.isInside(portalPosition.clone().subtract(subAmount)) &&
                border.isInside(portalPosition.clone().add(subAmount));
    }

    // Removes this portal, and its destination portal, from the map in PlayerRayCast
    public void remove()    {
        // Remove both from the map
        Map<Location, PortalPos> map = pl.rayCastingSystem.portals;
        map.remove(portalPosition);
        map.remove(destinationPosition);

        // Remove the portal blocks
        portalPosition.getBlock().setType(Material.AIR);
        destinationPosition.getBlock().setType(Material.AIR);
    }

    // Finds if the given offset from the portal is see through, returns true if an edge block or outside
    private boolean isSurroundingBlockTransparent(double x, double y, double z, int[] offset)    {
        double maxXZ = pl.config.maxXZ; double minXZ = pl.config.minXZ;
        double maxY = pl.config.maxY; double minY = pl.config.minY;
        x += offset[0]; y += offset[1]; z += offset[2];

        // Check if the block is outside
        if(x >= maxXZ || x <= minXZ || z >= maxXZ || z <= minXZ || y <= minY || y >= maxY)  {
            return false;
        }

        Block block = destinationPosition.clone().add(x, y, z).getBlock();
        return !block.getType().isSolid() || block.isLiquid(); // Return true if the block was transparent
    }

    // Loops through the blocks at the destination position, and finds the ones that aren't obscured by other solid blocks
    public void findCurrentBlocks()  {
        Config config = pl.config;

        // Make sure that the portal only updates its blocks if it is the correct time
        if(ticksSinceLastRebuild < config.portalBlockUpdateInterval)    {
            ticksSinceLastRebuild++;
            return;
        }
        ticksSinceLastRebuild = 0;

        ArrayList<BlockRaycastData> newBlocks = new ArrayList<>();
        // Loop through all blocks around the portal
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
        if(portalDirection == PortalDirection.EAST_WEST && z == 0.0) {continue;}
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    if(portalDirection == PortalDirection.NORTH_SOUTH && x == 0.0) {continue;}

                    boolean edge = x == config.maxXZ || x == config.minXZ || z == config.maxXZ || z == config.minXZ || y == config.maxY || y == config.minY;

                    Location originLoc = portalPosition.clone().add(x, y, z);
                    Location destLoc = destinationPosition.clone().add(x, y, z);
                    
                    // First check if the block is visible from any neighboring block
                    boolean allSolid = true;
                    for(int[] offset : offsets) {
                        if(isSurroundingBlockTransparent(x, y, z, offset))  {
                            allSolid = false;
                            break;
                        }
                    }

                    // If the block is bordered by at least one transparent block, add it to the list
                    if(!allSolid)    {
                        newBlocks.add(new BlockRaycastData(originLoc, destLoc, edge));
                    }
                }
            }
        }
        currentBlocks = newBlocks;
    }
}