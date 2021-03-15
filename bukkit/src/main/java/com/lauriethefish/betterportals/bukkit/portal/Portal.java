package com.lauriethefish.betterportals.bukkit.portal;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.api.PortalPosition;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.chunk.chunkloading.PortalChunkLoader;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.entity.IEntityTeleportManager;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.entity.PortalEntityListFactory;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformations;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformationsFactory;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Portal implements IPortal, ConfigurationSerializable {
    @Getter private final UUID id;
    @Getter private final UUID ownerId;
    @Getter private String name;

    private final IPortalManager portalManager;
    private final Logger logger;

    @Getter private final PortalPosition originPos;
    @Getter private final PortalPosition destPos;
    @Getter private final Vector size;
    @Getter private final boolean isCrossServer;
    @Getter private final boolean isCustom;

    @Getter private final PortalTransformations transformations;
    @Getter private final IViewableBlockArray viewableBlocks;

    @Getter private final IPortalEntityList entityList;
    @Getter private final IEntityTeleportManager entityTeleportationManager;
    private final PortalChunkLoader chunkLoader;

    private int ticksSinceActivated = -1;
    private int ticksSinceViewActivated = -1;

    @Inject
    public Portal(IPortalManager portalManager, PortalEntityListFactory entityListFactory, IViewableBlockArray.Factory viewableBlockArrayFactory,
                  IEntityTeleportManager.Factory teleportManagerFactory, PortalChunkLoader chunkLoader, MiscConfig miscConfig,
                  Logger logger, PortalTransformationsFactory transformationsFactory,
                  @Assisted("originPos") PortalPosition originPos, @Assisted("destPos") PortalPosition destPos,
                  @Assisted Vector size, @Assisted boolean isCustom,
                  @Assisted("id") UUID id, @Nullable @Assisted("ownerId") UUID ownerId, @Nullable @Assisted("name") String name) {
        this.portalManager = portalManager;
        this.logger = logger;
        this.originPos = originPos;
        this.destPos = destPos;
        this.size = size;
        this.isCrossServer = destPos.isExternal();
        this.isCustom = isCustom;
        // We do not need to get the destination entities if viewing entities through portals is disabled, or if entity support is disabled
        this.entityList = entityListFactory.create(this, !isCrossServer && miscConfig.isEntitySupportEnabled());
        this.entityTeleportationManager = teleportManagerFactory.create(this);
        this.chunkLoader = chunkLoader;
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;

        this.transformations = transformationsFactory.create(this);
        this.viewableBlocks = viewableBlockArrayFactory.create(this);
    }

    @Override
    public void onUpdate() {
        // Remove the portal if it is invalid
        if(!isStillValid()) {
            remove(true);
        }
        entityList.update(ticksSinceActivated);
        entityTeleportationManager.update();

        ticksSinceActivated++;
    }

    @Override
    public void onViewUpdate() {
        viewableBlocks.update(ticksSinceViewActivated);

        ticksSinceViewActivated++;
    }

    @Override
    public void onActivate() {
        logger.fine("Portal was activated");
        chunkLoader.forceloadPortalChunks(destPos);
        ticksSinceActivated = 0;
    }

    @Override
    public void onDeactivate() {
        logger.fine("Portal was deactivated");
        chunkLoader.unforceloadPortalChunks(destPos);
        viewableBlocks.reset();
        ticksSinceActivated = -1;
    }

    @Override
    public void onViewActivate() {
        logger.fine("Portal was view-activated");
        ticksSinceViewActivated = 0;
    }

    @Override
    public void onViewDeactivate() {
        logger.fine("Portal was view-deactivated");
        ticksSinceViewActivated = -1;
    }

    @Override
    public void remove(boolean removeOtherDirection) {
        portalManager.removePortal(this);
        originPos.getLocation().getBlock().setType(Material.AIR);
        if(removeOtherDirection) {
            portalManager.removePortalsAt(destPos.getLocation());
            // Break the portal blocks at the destination for nether portals
            if(isNetherPortal()) {
                destPos.getLocation().getBlock().setType(Material.AIR);
            }
        }
    }

    @Override
    public String getPermissionPath() {
        if(isCustom) {
            if(getName() != null) {
                return ".custom." + getName();
            }   else    {
                return "";
            }
        }   else    {
            return ".nether." + originPos.getWorldName();
        }
    }

    @Override
    public boolean isRegistered() {
        return portalManager.getPortalById(id) != null;
    }

    @Override
    public void setName(@Nullable String newName) {
        if(isNetherPortal()) throw new IllegalStateException("Cannot set name of nether portal");

        name = newName;
    }

    private boolean isStillValid() {
        // Custom portals shouldn't remove themselves if the portal blocks are broken
        if(isCustom) {return true;}

        // For nether portals, the portal blocks must have the type of the portal material
        return      originPos.getLocation().getBlock().getType() == MaterialUtil.PORTAL_MATERIAL
                &&  destPos.getLocation().getBlock().getType() == MaterialUtil.PORTAL_MATERIAL;
    }

    @NotNull
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<>();
        result.put("originPos", originPos);
        result.put("destPos", destPos);
        result.put("size", size);
        result.put("anchored", isCustom);
        result.put("id", id.toString());
        if(ownerId != null) {result.put("owner", ownerId.toString());}
        if(name != null) {result.put("name", name);}

        return result;
    }

    // We have to inject this statically unfortunately, no real other choice :/
    @Inject private static Factory deserializationFactory;

    public static Portal valueOf(Map<String, Object> map) {
        // We have to null check this and replace it with a random ID since it didn't exist in all prior versions
        String idString = (String) map.get("id");
        UUID id = idString == null ? UUID.randomUUID() : UUID.fromString(idString);
        // Portals don't always have an owner
        String ownerIdString = (String) map.get("ownerId");
        UUID ownerId = ownerIdString == null ? null : UUID.fromString(ownerIdString);

        return (Portal) deserializationFactory.create(
                (PortalPosition) map.get("originPos"),
                (PortalPosition) map.get("destPos"),
                (Vector) map.get("size"),
                (boolean) map.get("anchored"),
                id,
                ownerId,
                (String) map.get("name")
        );
    }
}
