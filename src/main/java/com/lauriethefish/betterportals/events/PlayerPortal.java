package com.lauriethefish.betterportals.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

// This event is called whenever a player tries to use a portal
// In this instance, we just cancel the event, since our plugin deals with the portals in a different way

public class PlayerPortal implements Listener   {
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }
}