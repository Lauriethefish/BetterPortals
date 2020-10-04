package com.lauriethefish.betterportals.events;

import com.lauriethefish.betterportals.BetterPortals;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

// This event is called whenever a player tries to use a portal
// If it is a nether portal, we cancel the event, since our plugin deals with the portals in a different way
public class EntityPortal implements Listener   {
    private BetterPortals pl;
    public EntityPortal(BetterPortals pl)   {
        this.pl = pl;
    }

    // Don't cancel the events if the world is disabled
    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        if(pl.config.isWorldDisabled(event.getFrom()))  {return;}

        // First check that the portal is a nether portal, so that end portals/gateways still work
        // Since this event has no getCause method, we just check if a portal is very close
        if(pl.findClosestPortal(event.getFrom(), 5.0) != null)  {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if(pl.config.isWorldDisabled(event.getFrom()))  {return;}

        // Check that the portal is a nether portal
        if(event.getCause() == TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
        }
    }
}