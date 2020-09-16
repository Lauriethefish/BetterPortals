package com.lauriethefish.betterportals.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.BlockRaycastData;
import com.lauriethefish.betterportals.Config;
import com.lauriethefish.betterportals.ReflectUtils;
import com.lauriethefish.betterportals.math.Matrix;
import com.lauriethefish.betterportals.multiblockchange.MultiBlockChangeManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Stores all of the attributes required for one direction of a portal
// Two of these should be created per portal, one for the effect on each side
public class PortalPos {
    private BetterPortals pl;

    // The origin position and orientation of the portal
    public Location portalPosition;
    public PortalDirection portalDirection;

    // The destination position and orientation of the portal
    public Location destinationPosition;
    public PortalDirection destinationDirection;

    private Matrix originToDestination;
    private Matrix rotateToDestination;

    private Matrix destinationToOrigin;
    private Matrix rotateToOrigin;

    // Size of the plane the makes up the portal radius from the centerpoint of the portal
    public Vector planeRadius;

    // The size of the portal's gateway on the X and Y
    public Vector portalSize;

    // Array of the blocks at the destination of the portal that need checking for visibility
    public ArrayList<BlockRaycastData> currentBlocks;
    private int ticksSinceLastRebuild = Integer.MAX_VALUE;

    public Collection<Entity> nearbyEntitiesOrigin = null;
    public Collection<Entity> nearbyEntitiesDestination = null;
    private int ticksSinceLastEntityCheck = Integer.MAX_VALUE;

    public boolean anchored;

    // Constructor to generate the collision box for a given portal
    // NOTE: The portalPosition must be the EXACT center of the portal on the x, y and z
    public PortalPos(BetterPortals pl, Location portalPosition, PortalDirection portalDirection, 
                    Location destinationPosition, PortalDirection destinationDirection, Vector portalSize, boolean anchored) {
        this.pl = pl;
        this.portalPosition = portalPosition;
        this.portalDirection = portalDirection;
        this.destinationPosition = destinationPosition;
        this.destinationDirection = destinationDirection;
        this.portalSize = portalSize;
        this.anchored = anchored;

        rotateToDestination = Matrix.makeRotation(portalDirection, destinationDirection);
        rotateToOrigin = Matrix.makeRotation(destinationDirection, portalDirection);

        // Matrix that takes a coordinate at the origin of the portal, and rotates and translates it to the destination
        originToDestination = Matrix.makeTranslation(destinationPosition.toVector())
                                .multiply(rotateToDestination)
                                .multiply(Matrix.makeTranslation(portalPosition.toVector().multiply(-1.0)));
        
        destinationToOrigin = Matrix.makeTranslation(portalPosition.toVector())
                                .multiply(rotateToOrigin)
                                .multiply(Matrix.makeTranslation(destinationPosition.toVector().multiply(-1.0)));
        
        // Divide the size by 2 so it is the correct amount to subtract from the center to reach each corner
        // Then orient it so that is on the z if the portal is north/south
        this.planeRadius = portalDirection.swapVector(portalSize.clone().multiply(0.5).add(pl.config.portalCollisionBox));
    }

    // Loads all of the values for this portal from the data file
    public PortalPos(BetterPortals pl, PortalStorage storage, ConfigurationSection sect)  {
        this(pl, 
            storage.loadLocation(sect.getConfigurationSection("portalPosition")),
            PortalDirection.valueOf(sect.getString("portalDirection")),
            storage.loadLocation(sect.getConfigurationSection("destinationPosition")),
            PortalDirection.valueOf(sect.getString("destinationDirection")),
            storage.loadPortalSize(sect.getConfigurationSection("portalSize")), 
            sect.getBoolean("anchored"));
    }

    // Saves all of the values for this portal into sect
    public void save(PortalStorage storage, ConfigurationSection sect)   {
        storage.setLocation(sect.createSection("portalPosition"), portalPosition);
        sect.set("portalDirection", portalDirection.toString());
        storage.setLocation(sect.createSection("destinationPosition"), destinationPosition);
        sect.set("destinationDirection", destinationDirection.toString());
        storage.setPortalSize(sect.createSection("portalSize"), portalSize);
        sect.set("anchored", anchored);
    }

    // Forces this portal to recheck for entities next tick
    public void forceEntityUpdate()  {
        ticksSinceLastEntityCheck = Integer.MAX_VALUE;
    }

    // Updates the two lists of neaby entities, if it is time to
    public void updateNearbyEntities()   {
        if(ticksSinceLastEntityCheck < pl.config.entityCheckInterval)  {
            ticksSinceLastEntityCheck++;
            return;
        }
        
        ticksSinceLastEntityCheck = 0;
        
        nearbyEntitiesOrigin = portalPosition.getWorld().getNearbyEntities(portalPosition, pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
        nearbyEntitiesDestination = destinationPosition.getWorld().getNearbyEntities(destinationPosition, pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
    }

    public boolean checkOriginAndDestination()  {
        PortalPos destination = pl.rayCastingSystem.portals.get(destinationPosition);
        // Remove the portal if either the origin or destination is broken
        if(destination != null && !(checkIfStillActive() && destination.checkIfStillActive())) {
            remove();
            return false;
        }
        return true;
    }

    // Checks if the portal has been broken
    // This is used to remove the portal from the plugins list of active portals
    public boolean checkIfStillActive() {
        // If the portal is anchored, don't remove it
        if(anchored)    {
            return true;
        }

        // Get the offset from the portals absolute center to the top left and bottom right corners of the portal blocks
        Vector subAmount = portalDirection.swapVector(portalSize.clone().multiply(0.5).add(new Vector(0.0, 0.0, 0.5)));
        WorldBorder border = portalPosition.getWorld().getWorldBorder();

        // Check if the block at the centre of the portal is a portal block
        return portalPosition.getBlock().getType() == ReflectUtils.portalMaterial &&
                // Check that the bottom left and top right of the portal are both inside the worldborder,
                // since portals outside the worldborder should be broken
                border.isInside(portalPosition.clone().subtract(subAmount)) &&
                border.isInside(portalPosition.clone().add(subAmount));
    }

    public Location moveOriginToDestination(Location loc)   {
        return originToDestination.transform(loc.toVector()).toLocation(destinationPosition.getWorld());
    }

    public Vector moveOriginToDestination(Vector vec)   {
        return originToDestination.transform(vec);
    }

    public Location moveDestinationToOrigin(Location loc)   {
        return destinationToOrigin.transform(loc.toVector()).toLocation(portalPosition.getWorld());
    }

    public Vector moveDestinationToOrigin(Vector vec)   {
        return destinationToOrigin.transform(vec);
    }

    public Vector rotateToOrigin(Vector dir)    {
        return rotateToOrigin.transform(dir);
    }

    public Vector rotateToDestination(Vector dir)    {
        return rotateToDestination.transform(dir);
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

    // Offsets for checking if a block needs to be rendered
    private static final Vector[] offsets = new Vector[]    {
        new Vector(1, 0, 0),
        new Vector(-1, 0, 0),
        new Vector(0, 1, 0),
        new Vector(0, -1, 0),
        new Vector(0, 0, 1),
        new Vector(0, 0, -1),
    };

    public void removePortalBlocks(Player player)    {
        setPortalBlocks(player, false);
    }

    public void recreatePortalBlocks(Player player)    {
        setPortalBlocks(player, true);
    }
    
    // Sends a packet to the player setting the portal blocks to air (if reset is false), or back to what they were (if reset is true)
    private void setPortalBlocks(Player player, boolean reset)  {
        MultiBlockChangeManager manager = MultiBlockChangeManager.createInstance(player);

        Vector actualSize = portalDirection.swapVector(portalSize);
        Vector blockBL = portalPosition.toVector().subtract(actualSize.multiply(0.5));

        // Loop through each block of the portal, and set them to either air or back to portal
        Object nmsAirData = BlockRaycastData.getNMSData(Material.AIR);
        for(int x = 0; x < portalSize.getX(); x++)  {
            for(int y = 0; y < portalSize.getY(); y++)  {
                Vector offset = portalDirection.swapVector(new Vector(x, y, 0.0));
                Location position = blockBL.toLocation(portalPosition.getWorld()).add(offset);
                
                // Add the changes to our manager
                if(reset)   {
                    manager.addChange(position, BlockRaycastData.getNMSData(position.getBlock()));
                }   else    {
                    manager.addChange(position, nmsAirData);
                }
            }
        }

        manager.sendChanges(); // Send the packet to the player
    }

    // Checks if the location is on the plane made by the portal window
    // This is used because entities in line with the portal should not be rendered
    public boolean positionInlineWithOrigin(Location loc)  {
        return portalDirection.swapLocation(loc).getZ() == portalDirection.swapLocation(portalPosition).getZ();
    }

    public boolean positionInlineWithDestination(Location loc)  {
        return destinationDirection.swapLocation(loc).getZ() == destinationDirection.swapLocation(destinationPosition).getZ();
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
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    Location originLoc = portalPosition.clone().add(x, y, z);
                    // Skip blocks directly in line with the portal
                    if(positionInlineWithOrigin(originLoc)) {
                        continue;
                    }

                    boolean edge = x == config.maxXZ || x == config.minXZ || z == config.maxXZ || z == config.minXZ || y == config.maxY || y == config.minY;
                    Location destLoc = moveOriginToDestination(originLoc);
                    
                    // First check if the block is visible from any neighboring block
                    boolean transparentBlock = false;
                    for(Vector offset : offsets) {
                        Location blockPos = destLoc.clone().add(offset);

                        if(!blockPos.getBlock().getType().isOccluding())    {
                            transparentBlock = true;
                            break;
                        }
                    }

                    // If the block is bordered by at least one transparent block, add it to the list
                    if(transparentBlock)    {
                        newBlocks.add(new BlockRaycastData(originLoc, destLoc, edge));
                    }
                }
            }
        }
        currentBlocks = newBlocks;
    }
}