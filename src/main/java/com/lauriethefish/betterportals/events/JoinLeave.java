package com.lauriethefish.betterportals.events;

import com.lauriethefish.betterportals.BetterPortals;

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
        pl.addPlayer(event.getPlayer());
    }

    // Remove any players leaving the game
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)    {
        pl.removePlayer(event.getPlayer());
    }
}