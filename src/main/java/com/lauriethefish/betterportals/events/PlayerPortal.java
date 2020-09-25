package com.lauriethefish.betterportals.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

// This event is called whenever a player tries to use a portal
// If it is a nether portal, we cancel the event, since our plugin deals with the portals in a different way

public class PlayerPortal implements Listener   {
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        // First check that the portal is a nether portal, so that end portals/gateways still work
        if(event.getCause() == TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
        }
    }
}