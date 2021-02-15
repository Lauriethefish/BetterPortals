package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

// We cancel regular nether portal teleportation since we override it.
public class PortalTeleportationEvents implements Listener {
    @Inject
    public PortalTeleportationEvents(JavaPlugin pl) {
        pl.getServer().getPluginManager().registerEvents(this, pl);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        // The event has no getCause method, so we'll just have to use this botch unfortunately
        Entity entity = event.getEntity();
        //if(entity.getLocation().getBlock().getType() == MaterialUtil.PORTAL_MATERIAL) {
            event.setCancelled(true); // TODO
        //}
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if(event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
        }
    }
}
