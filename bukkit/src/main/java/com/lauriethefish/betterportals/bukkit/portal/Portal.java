package com.lauriethefish.betterportals.bukkit.portal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.chunkloading.chunkpos.ChunkPosition;
import com.lauriethefish.betterportals.bukkit.multiblockchange.MultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.network.BlockDataArrayRequest;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.CachedViewableBlocksArray;
import com.lauriethefish.betterportals.bukkit.portal.blockarray.SerializableBlockData;
import com.lauriethefish.betterportals.bukkit.selection.PortalSelection;
import com.lauriethefish.betterportals.network.TeleportPlayerRequest;
import com.lauriethefish.betterportals.network.Response.RequestException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
    private RenderConfig renderConfig;

    @Getter private PortalPosition originPos;
    @Getter private PortalPosition destPos;

    @Getter private PortalTransformations locTransformer;
    @Getter private PortalUpdateManager updateManager;

    // Size of the plane the makes up the portal radius from the centerpoint of the portal
    @Getter private Vector planeRadius;

    // The size of the portal's gateway on the X and Y
    private Vector portalSize;

    @Getter private Map<Entity, Vector> nearbyEntitiesOrigin = null;
    @Getter private Collection<Entity> nearbyEntitiesDestination = null;

    private Set<ChunkPosition> destinationChunks = new HashSet<>();

    private boolean anchored;

    @Getter private UUID owner; // Who created this portal. This is null for nether portals

    // Constructor to generate the collision box for a given portal
    // NOTE: The portalPosition must be the EXACT center of the portal on the x, y and z
    public Portal(BetterPortals pl, PortalPosition originPos, PortalPosition destPos, Vector portalSize, boolean anchored, UUID owner) {
        this.pl = pl;
        this.renderConfig = pl.getLoadedConfig().getRendering();
        this.originPos = originPos;
        this.destPos = destPos;
        this.portalSize = portalSize;
        this.anchored = anchored;
        this.owner = owner;
        this.locTransformer = new PortalTransformations(originPos, destPos);
        this.updateManager = new PortalUpdateManager(pl, this);

        // Find the chunks around the destination of the portal
        Vector boxSize = new Vector(renderConfig.getMaxXZ(), renderConfig.getMaxY(), renderConfig.getMaxXZ());
        Location boxBL = destPos.getLocation().subtract(boxSize);
        Location boxTR = destPos.getLocation().add(boxSize);
        ChunkPosition.areaIterator(boxBL, boxTR).addAll(destinationChunks);
        
        // Divide the size by 2 so it is the correct amount to subtract from the center to reach each corner
        // Then orient it so that is on the z if the portal is north/south
        this.planeRadius = originPos.getDirection().swapVector(portalSize.clone().multiply(0.5).add(renderConfig.getCollisionBox()));
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

    // Updates the two lists of neaby entities
    void updateNearbyEntities()   {
        Collection<Entity> nearbyEntities = originPos.getWorld()
                    .getNearbyEntities(originPos.getLocation(), renderConfig.getMaxXZ(), renderConfig.getMaxY(), renderConfig.getMaxXZ());

        // Store the entity and last location in a hash map
        Map<Entity, Vector> newOriginEntites = new HashMap<>();
        for(Entity entity : nearbyEntities) {
            // Copy existing locations to the new map
            Vector existingLocation = nearbyEntitiesOrigin == null ? null : nearbyEntitiesOrigin.get(entity);
            newOriginEntites.put(entity, existingLocation);
        }
        nearbyEntitiesOrigin = newOriginEntites;

        if(pl.getLoadedConfig().isEntitySupportEnabled())   {
            nearbyEntitiesDestination = destPos.getWorld()
                        .getNearbyEntities(destPos.getLocation(), renderConfig.getMaxXZ(), renderConfig.getMaxY(), renderConfig.getMaxXZ());
        }
    }

    // Teleports entities through the portal if they walk through
    void checkEntityTeleportation() {
        Iterator<Map.Entry<Entity, Vector>> iter = nearbyEntitiesOrigin.entrySet().iterator();
        World originWorld = originPos.getWorld();
        while(iter.hasNext())   {
            Map.Entry<Entity, Vector> entry = iter.next();

            Entity entity = entry.getKey();
            Vector lastKnownLocation = entry.getValue();

            // If the entity isn't in the same world, we skip it
            if(entity.getWorld() != originWorld)   {
                iter.remove();
                continue;
            }

            Vector actualLocation = entity.getLocation().toVector();
            // Teleport the entity if it walked through a portal
            PlaneIntersectionChecker teleportChecker = new PlaneIntersectionChecker(actualLocation, this);
            if(!(entity instanceof Player) && lastKnownLocation != null && teleportChecker.checkIfVisibleThroughPortal(lastKnownLocation))  {
                teleportEntity(entity);
                getNearbyEntitiesDestination().add(entity);
                iter.remove();
            }

            // Set the location back to the actual location
            entry.setValue(actualLocation);
        }
    }

    // Teleports an entity from the origin to the destination
    public void teleportEntity(Entity entity)  {
        pl.logDebug("Teleporting entity");
        // Save their velocity for later
        Vector entityVelocity = entity.getVelocity().clone();
        // Move them to the other portal
        Location newLoc = locTransformer.moveToDestination(entity.getLocation());
        newLoc.setDirection(locTransformer.rotateToDestination(entity.getLocation().getDirection()));

        // If the portal is cross-server, call the teleportCrossServer function.
        // This function should only be called on cross-server portals with a player - never with an entity
        if(isCrossServer()) {
            teleportCrossServer((Player) entity, newLoc);
        }   else    {
            entity.teleport(newLoc);
        
            // Set their velocity back to what it was
            entity.setVelocity(locTransformer.rotateToDestination(entityVelocity));
        }
    }

    private void teleportCrossServer(Player player, Location newLoc) {
        // Make a TeleportPlayerRequest to teleport the player to the right place on another server
        pl.logDebug("Requesting player to be teleported across servers!");
        TeleportPlayerRequest request = new TeleportPlayerRequest(player.getUniqueId(),
                    destPos.getServerName(), destPos.getWorldName(),
                    newLoc.getX(), newLoc.getY(), newLoc.getZ(),
                    newLoc.getYaw(), newLoc.getPitch());
        
        // Send the correct request.
        try {
            pl.logDebug("Sending teleport player request for player %s", player.getUniqueId());
            pl.getNetworkClient().sendRequest(request);
        } catch (RequestException ex) {
            ex.printStackTrace();
        }

        pl.removePlayer(player); // Avoid the player being teleported multiple times
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

        if(removeDestination && !isCrossServer())   { // We cannot remove the destination block for cross-server portals
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
        Object nmsAirData = new SerializableBlockData(Material.AIR).getNmsData();
        for(int x = 0; x < portalSize.getX(); x++)  {
            for(int y = 0; y < portalSize.getY(); y++)  {
                Vector offset = originPos.getDirection().swapVector(new Vector(x, y, 0.0));
                Location position = blockBL.toLocation(originPos.getWorld()).add(offset);
                
                // Add the changes to our manager
                if(reset)   {
                    manager.addChange(position, new SerializableBlockData(position.getBlock()).getNmsData());
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

    public boolean isCrossServer() {
        return destPos.isExternal();
    }

    BlockDataArrayRequest createBlockDataRequest() {
        return new BlockDataArrayRequest(originPos, destPos);
    }

    // Returns the currently fetched viewable blocks array. Update should be called before this point
    public CachedViewableBlocksArray getCachedViewableBlocksArray() {
        return pl.getBlockArrayProcessor().getCachedArray(createBlockDataRequest());
    }

    void updateCurrentBlocks() {
        // Send a request to the PortalBlockArrayProcessor
        pl.getBlockArrayProcessor().updateBlockArray(createBlockDataRequest());
    }

    void forceloadDestinationChunks() {
        pl.logDebug("Forceloading destination chunks");
        pl.getChunkLoader().forceLoadAllPos(destinationChunks.iterator());
    }

    void unforceloadDestinationChunks() {
        pl.logDebug("Unforceloading destination chunks");
        pl.getChunkLoader().unForceLoadAllPos(destinationChunks.iterator());
    }
}