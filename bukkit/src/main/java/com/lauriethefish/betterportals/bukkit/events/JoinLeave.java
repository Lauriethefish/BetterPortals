package com.lauriethefish.betterportals.bukkit.events;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Deals with creating a player's PlayerData when they join the game
public class JoinLeave implements Listener {
    private BetterPortals pl;
    public JoinLeave(BetterPortals pl) {
        this.pl = pl;
    }

    // Add a new PlayerData for joining players
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Find if we need to teleport the player while joining, since they went through a portal
        Location teleportPos = pl.getTeleportPosOnJoin(player);
        pl.logDebug("Checking teleport on join for player %s", player.getUniqueId());
        if(teleportPos != null) {
            pl.logDebug("Teleporting player %s on join", player.getUniqueId());
            player.teleport(teleportPos);
        }

        pl.addPlayer(player);
    }

    // Remove any players leaving the game
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)    {
        pl.removePlayer(event.getPlayer());
    }
}