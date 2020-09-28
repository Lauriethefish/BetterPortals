package com.lauriethefish.betterportals.selection;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

// Handles clicking with the portal wand
public class WandInteract implements Listener   {
    private BetterPortals pl;
    public WandInteract(BetterPortals pl)   {
        this.pl = pl;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // If the item used wasn't the portal wand, return
        ItemStack item = event.getItem();
        if(item == null || !pl.isPortalWand(item))  {return;}

        // If the player did not right click or left click a block, return
        Action action = event.getAction();
        if(!(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK))  {return;}

        Player player = event.getPlayer();
        // Check that the player has permission to select things
        if(!player.hasPermission("betterportals.wand")) {return;}

        // Cancel the event if we clicked with the portal wand
        event.setCancelled(true);
        PlayerData playerData = pl.getPlayerData(player);

        // Call the method in the player for selecting a block
        playerData.makeSelection(event.getClickedBlock().getLocation(), action);
    }
}
