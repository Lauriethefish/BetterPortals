package com.lauriethefish.betterportals.events;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

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
        pl.players.put(player.getUniqueId(), new PlayerData(pl, player));
    }

    // Remove any players leaving the game
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)    {
        pl.players.remove(event.getPlayer().getUniqueId());
    }
}