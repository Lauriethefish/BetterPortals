package com.lauriethefish.betterportals.commands;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

// Testing command for the entity manipulator
public class FakeDeleteEntity implements CommandExecutor    {
    private BetterPortals pl;
    public FakeDeleteEntity(BetterPortals pl)   {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)    {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
            return false;
        }

        Player player = (Player) sender;
        PlayerData playerData = pl.players.get(player.getUniqueId());

        if(args.length == 1)    {
            playerData.entityManipulator.swapHiddenEntities(new HashSet<UUID>());
            return true;
        }

        List<Entity> nearbyEntities = player.getNearbyEntities(20.0, 20.0, 20.0);
        Entity closestEntity = nearbyEntities.get(0);
        for(Entity entity : nearbyEntities) {
            if(entity.getLocation().distance(player.getLocation()) < closestEntity.getLocation().distance(player.getLocation()))    {
                closestEntity = entity;
            }
        }

        playerData.entityManipulator.addHiddenEntity(closestEntity.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Removed entity!");

        return true;
    }
}