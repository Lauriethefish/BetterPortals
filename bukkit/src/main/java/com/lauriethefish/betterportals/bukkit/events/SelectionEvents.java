package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.bukkit.player.selection.IPortalSelection;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SelectionEvents implements Listener {
    private final IPlayerDataManager playerDataManager;
    private final MessageConfig messageConfig;

    @Inject
    public SelectionEvents(IEventRegistrar eventRegistrar, IPlayerDataManager playerDataManager, MessageConfig messageConfig) {
        this.playerDataManager = playerDataManager;
        this.messageConfig = messageConfig;

        eventRegistrar.register(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if(item == null || !messageConfig.isPortalWand(item))  {return;}

        // We only care about left or right clicks
        Action action = event.getAction();
        if(!(action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK))  {return;}

        Player player = event.getPlayer();
        if(!player.hasPermission("betterportals.wand")) {return;} // Avoid allowing those without permissions to get the wand to use it
        event.setCancelled(true);

        Location blockPos = Objects.requireNonNull(event.getClickedBlock(), "Clicked block was null despite being a block event, this should never happen").getLocation();

        // Set either position A or B depending on right/left click
        IPlayerData playerData = Objects.requireNonNull(playerDataManager.getPlayerData(player), "Player in event had no registered player data");
        IPortalSelection selection = playerData.getSelection().getCurrentlySelecting();
        if(action == Action.LEFT_CLICK_BLOCK) {
            selection.setPositionA(blockPos);
            player.sendMessage(messageConfig.getChatMessage("setPosA"));
        }   else {
            selection.setPositionB(blockPos);
            player.sendMessage(messageConfig.getChatMessage("setPosB"));
        }


    }
}
