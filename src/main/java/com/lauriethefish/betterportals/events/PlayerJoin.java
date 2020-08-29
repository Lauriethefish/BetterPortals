package com.lauriethefish.betterportals.events;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

// Deals with creating a player's PlayerData when they join the game
public class PlayerJoin implements Listener {
    private BetterPortals pl;
    public PlayerJoin(BetterPortals pl) {
        this.pl = pl;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Retrieve the PlayerData from the plugins list of registered players
        Player player = event.getPlayer();
        PlayerData data = pl.players.get(player.getUniqueId());

        // If the data does not exist yet, make a new PlayerData and add it to the list,
        // Otherwise, reset the player's surrounding block states as they have just logged in
        // and all of the ghost blocks need to be sent again
        if(data == null)    {
            data = new PlayerData(player, pl);
            pl.players.put(player.getUniqueId(), data);
        }   else    {
            data.resetPlayer(player);
        }
    }
}