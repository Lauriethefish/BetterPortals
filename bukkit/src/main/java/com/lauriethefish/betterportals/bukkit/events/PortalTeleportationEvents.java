package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.config.PortalSpawnConfig;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * We cancel regular nether portal teleportation since we override it.
 */
public class PortalTeleportationEvents implements Listener {
    private final PortalSpawnConfig spawnConfig;
    private final IPortalManager portalManager;
    private final MessageConfig messageConfig;

    @Inject
    public PortalTeleportationEvents(IEventRegistrar eventRegistrar, IPortalManager portalManager, PortalSpawnConfig spawnConfig, MessageConfig messageConfig) {
        this.portalManager = portalManager;
        this.messageConfig = messageConfig;
        this.spawnConfig = spawnConfig;

        eventRegistrar.register(this);
    }

    private boolean isPluginPortal(@NotNull Entity entity) {
        Vector maxPortalSize = spawnConfig.getMaxPortalSize();
        double portalExistenceRadius = Math.max(maxPortalSize.getX(), maxPortalSize.getY()) + 2;

        return portalManager.findClosestPortal(entity.getLocation(), portalExistenceRadius) != null;
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        Material blockType = event.getEntity().getLocation().getBlock().getType();

        boolean isNetherPortal = blockType != Material.END_PORTAL && blockType != Material.END_GATEWAY;
        if(isPluginPortal(event.getEntity()) && isNetherPortal) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {

        boolean isNetherPortal = event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL;
        if(isPluginPortal(event.getPlayer()) && isNetherPortal) {
            event.setCancelled(true);
        }   else if(isNetherPortal) {
            if(spawnConfig.isWorldDisabled(event.getFrom().getWorld()) || spawnConfig.isWorldDisabled(event.getTo().getWorld())) {
                return;
            }

            // Send a warning for vanilla nether portals, since players might not realise that they need relighting
            String warning = messageConfig.getWarningMessage("vanillaPortal");
            if(!warning.isEmpty()) {
                event.getPlayer().sendMessage(warning);
            }
        }
    }
}
