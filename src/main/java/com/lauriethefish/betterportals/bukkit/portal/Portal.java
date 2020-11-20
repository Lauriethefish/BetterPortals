package com.lauriethefish.betterportals.bukkit.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.BlockRaycastData;
import com.lauriethefish.betterportals.bukkit.BlockRotator;
import com.lauriethefish.betterportals.bukkit.Config;
import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import com.lauriethefish.betterportals.bukkit.math.MathUtils;
import com.lauriethefish.betterportals.bukkit.multiblockchange.ChunkCoordIntPair;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.selection.PortalSelection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

// Stores all of the attributes required for one direction of a portal
// Two of these should be created per portal, one for the effect on each side
public class Portal implements ConfigurationSerializable    {
    private BetterPortals pl;

    @Getter private PortalPosition originPos;
    @Getter private PortalPosition destPos;

    @Getter private PortalTransformations locTransformer;

    // Used to rotate blocks on the other side of the portal to this direction
    private BlockRotator blockRotator;

    // Size of the plane the makes up the portal radius from the centerpoint of the portal
    @Getter private Vector planeRadius;

    // The size of the portal's gateway on the X and Y
    private Vector portalSize;

    private int lastActive = -2;
    private int ticksSinceActivation = 0;

    @Getter List<BlockRaycastData> currentBlocks;
    @Getter private Map<Entity, Vector> nearbyEntitiesOrigin = null;
    @Getter private Collection<Entity> nearbyEntitiesDestination = null;

    private Set<ChunkCoordIntPair> destinationChunks = new HashSet<>();

    private boolean anchored;

    // Used in unsafe mode to run block updates inside the async task
    @Getter AtomicBoolean queueBlockUpdate = new AtomicBoolean();

    @Getter private UUID owner; // Who created this portal. This is null for nether portals

    // Constructor to generate the collision box for a given portal
    // NOTE: The portalPosition must be the EXACT center of the portal on the x, y and z
    public Portal(BetterPortals pl, PortalPosition originPos, PortalPosition destPos, Vector portalSize, boolean anchored, UUID owner) {
        this.pl = pl;
        this.originPos = originPos;
        this.destPos = destPos;
        this.portalSize = portalSize;
        this.anchored = anchored;
        this.owner = owner;
        locTransformer = new PortalTransformations(originPos, destPos);

        // Find the chunks around the destination of the portal
        Vector boxSize = new Vector(pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
        Location boxBL = destPos.getLocation().subtract(boxSize);
        Location boxTR = destPos.getLocation().add(boxSize);
        ChunkCoordIntPair.areaIterator(boxBL, boxTR).addAll(destinationChunks);
        
        // Divide the size by 2 so it is the correct amount to subtract from the center to reach each corner
        // Then orient it so that is on the z if the portal is north/south
        this.planeRadius = originPos.getDirection().swapVector(portalSize.clone().multiply(0.5).add(pl.config.portalCollisionBox));
        this.blockRotator = BlockRotator.newInstance(this);
    }
    
    // Constructor to make a portal link between two selections
    public Portal(BetterPortals pl, PortalSelection origin, PortalSelection destination, Player creator)  {
        this(pl, origin.getPortalPosition(), destination.getPortalPosition(), 
                 origin.getPortalSize(), true, creator.getUniqueId());
    }

    // Loads this portal from a YAML file (required for ConfigurationSerializable)
    @Setter private static BetterPortals serializationInstance; // We need an instance of the plugin to create the portal during deserialization, this is the only reason this field exists
    public Portal(Map<String, Object> map) {
        this(serializationInstance,
            (PortalPosition) map.get("originPos"),
            (PortalPosition) map.get("destPos"),
            (Vector) map.get("size"),
            (boolean) map.get("anchored"),
            ((map.get("owner") == null) ? null : UUID.fromString((String) map.get("owner")))
        );
    }

    // Saves this portal to a YAML formatted configuration (portals.yml in this case)
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("originPos", originPos);
        result.put("destPos", destPos);
        result.put("size", portalSize);
        result.put("anchored", anchored);
        if(owner != null) {
            result.put("owner", owner.toString());
        }

        return result;
    }

    // Called every tick when the portal is in a loaded chunk
    private boolean pendingPlayerUpdate;
    public void mainUpdate() {

        pendingPlayerUpdate = true;
    }

    // Called every tick whenever a player is within the activation distance
    public void activatedByPlayerUpdate() {
        if(!pendingPlayerUpdate) {return;}
        pendingPlayerUpdate = false;

        
    }

    public void update(int currentTick)    {
        // If it has been longer than one tick since the portal was active, set the activation time to now
        int timeSinceLastActive = currentTick - lastActive;
        if(timeSinceLastActive > 1)    {
            // Load the chunks on the other side when the portal is activated
            for(ChunkCoordIntPair chunk : destinationChunks)    {
                // Force load the chunk if this is supported in the current minecraft version
                if(ReflectUtils.useNewChunkLoadingImpl) {
                    chunk.getChunk().setForceLoaded(true);
                }   else    {
                    chunk.getChunk().load();
                }
            }

            ticksSinceActivation = 0;
        }   else if(timeSinceLastActive == 0)   {
            return;
        }
        lastActive = currentTick;

        // Since this portal is active, add it to the new force loaded chunks
        pl.getPortalUpdator().keepChunksForceLoaded(destinationChunks);

        // Update the entities and blocks if we need to
        if(ticksSinceActivation % pl.config.entityCheckInterval == 0)   {
            updateNearbyEntities();
        }
        if(ticksSinceActivation % pl.config.portalBlockUpdateInterval == 0)   {
            if(pl.config.unsafeMode)    {
                queueBlockUpdate.set(true);
            }   else    {
                findCurrentBlocks();
            }
        }
        ticksSinceActivation++;
    }

    // Updates the two lists of neaby entities
    private void updateNearbyEntities()   {
        Collection<Entity> nearbyEntities = originPos.getWorld()
                    .getNearbyEntities(originPos.getLocation(), pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);

        // Store the entity and last location in a hash map
        Map<Entity, Vector> newOriginEntites = new HashMap<>();
        for(Entity entity : nearbyEntities) {
            // Copy existing locations to the new map
            Vector existingLocation = nearbyEntitiesOrigin == null ? null : nearbyEntitiesOrigin.get(entity);
            newOriginEntites.put(entity, existingLocation);
        }
        nearbyEntitiesOrigin = newOriginEntites;

        if(pl.config.enableEntitySupport)   {
            nearbyEntitiesDestination = destPos.getWorld()
                        .getNearbyEntities(destPos.getLocation(), pl.config.maxXZ, pl.config.maxY, pl.config.maxXZ);
        }
    }

    // Teleports an entity from the origin to the destination
    public void teleportEntity(Entity entity)  {
        // Save their velocity for later
        Vector playerVelocity = entity.getVelocity().clone();
        // Move them to the other portal
        Location newLoc = locTransformer.moveToDestination(entity.getLocation());
        newLoc.setDirection(locTransformer.rotateToDestination(entity.getLocation().getDirection()));

        entity.teleport(newLoc);
        
        // Set their velocity back to what it was
        entity.setVelocity(locTransformer.rotateToDestination(playerVelocity));
    }

    public boolean checkOriginAndDestination()  {
        Portal destination = pl.getPortal(destPos);
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
        Vector subAmount = originPos.getDirection().swapVector(portalSize.clone().multiply(0.5).add(new Vector(0.0, 0.0, 0.5)));
        WorldBorder border = originPos.getWorld().getWorldBorder();

        // Check if the block at the centre of the portal is a portal block
        return originPos.getBlock().getType() == ReflectUtils.portalMaterial &&
                // Check that the bottom left and top right of the portal are both inside the worldborder,
                // since portals outside the worldborder should be broken
                border.isInside(originPos.getLocation().subtract(subAmount)) &&
                border.isInside(originPos.getLocation().add(subAmount));
    }

    public void remove()    {
        remove(true);
    }

    // Removes this portal, and its destination portal (if set), from the map
    public void remove(boolean removeDestination)    {
        // Remove the portals from the map, and remove any leftover portal blocks
        pl.unregisterPortal(this);
        originPos.getBlock().setType(Material.AIR);

        if(removeDestination)   {
            pl.unregisterPortal(destPos);
            destPos.getBlock().setType(Material.AIR);
        }
    }

    public void removePortalBlocks(Player player)    {
        setPortalBlocks(player, false);
    }

    public void recreatePortalBlocks(Player player)    {
        setPortalBlocks(player, true);
    }
    
    // Sends a packet to the player setting the portal blocks to air (if reset is false), or back to what they were (if reset is true)
    private void setPortalBlocks(Player player, boolean reset)  {
        MultiBlockChangeManager manager = MultiBlockChangeManager.createInstance(player);

        Vector actualSize = originPos.getDirection().swapVector(portalSize);
        Vector blockBL = originPos.getVector().subtract(actualSize.multiply(0.5));

        // Loop through each block of the portal, and set them to either air or back to portal
        Object nmsAirData = BlockRaycastData.getNMSData(Material.AIR);
        for(int x = 0; x < portalSize.getX(); x++)  {
            for(int y = 0; y < portalSize.getY(); y++)  {
                Vector offset = originPos.getDirection().swapVector(new Vector(x, y, 0.0));
                Location position = blockBL.toLocation(originPos.getWorld()).add(offset);
                
                // Add the changes to our manager
                if(reset)   {
                    manager.addChange(position, BlockRaycastData.getNMSData(position.getBlock().getState()));
                }   else    {
                    manager.addChange(position, nmsAirData);
                }
            }
        }

        manager.sendChanges(); // Send the packet to the player
    }

    public boolean isCustom()   {
        return anchored;
    }

    // Loops through the blocks at the destination position, and finds the ones that aren't obscured by other solid blocks
    public void findCurrentBlocks()  {
        Config config = pl.config;

        List<BlockRaycastData> newBlocks = new ArrayList<>();

        // Loop through the surrounding blocks, and check which ones are occluding
        boolean[] occlusionArray = new boolean[config.totalArrayLength];
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    Location originLoc = MathUtils.moveToCenterOfBlock(originPos.getLocation().add(x, y, z));
                    Location position = locTransformer.moveToDestination(originLoc);
                    occlusionArray[config.calculateBlockArrayIndex(x, y, z)] = position.getBlock().getType().isOccluding();
                }
            }
        }

        // Check to see if each block is fully obscured, if not, add it to the list
        for(double z = config.minXZ; z <= config.maxXZ; z++) {
            for(double y = config.minY; y <= config.maxY; y++) {
                for(double x = config.minXZ; x <= config.maxXZ; x++) {
                    int arrayIndex = config.calculateBlockArrayIndex(x, y, z);

                    Location originLoc = MathUtils.moveToCenterOfBlock(originPos.getLocation().add(x, y, z));
                    Location destLoc = locTransformer.moveToDestination(originLoc);
                    // Skip blocks directly in line with the portal
                    if(originPos.isInLine(originLoc)) {continue;}
                    
                    // First check if the block is visible from any neighboring block
                    boolean transparentBlock = false;
                    for(int offset : config.surroundingOffsets) {
                        int finalIndex = arrayIndex + offset;
                        if(finalIndex < 0 || finalIndex >= config.totalArrayLength) {
                            continue;
                        }

                        if(!occlusionArray[finalIndex])  {
                            transparentBlock = true;
                            break;
                        }
                    }

                    // If the block is bordered by at least one transparent block, add it to the list
                    if(transparentBlock)    {
                        boolean edge = x == config.maxXZ || x == config.minXZ || z == config.maxXZ || z == config.minXZ || y == config.maxY || y == config.minY;
                        newBlocks.add(new BlockRaycastData(blockRotator, originLoc, destLoc, edge));
                    }
                }
            }
        }
        currentBlocks = newBlocks;
    }
}