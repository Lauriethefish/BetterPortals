package com.lauriethefish.betterportals.commands;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PortalDirection;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

// This class is not commented (apart from here), because it is unused and was only used for debugging
public class SpawnPortal implements CommandExecutor {

    private BetterPortals pl;
    public SpawnPortal(BetterPortals pl)    {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))   {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command!");
            return false;
        }

        Player player = (Player) sender;
        if(args.length != 1)    {
            player.sendMessage(ChatColor.RED + "Error: Wrong number of arguments. Usage: /spawnportal <eastwest/northsouth>");
            return false;
        }

        String directionStr = args[0].toLowerCase();
        PortalDirection direction;

        if(directionStr.equals("eastwest")) {
            direction = PortalDirection.EAST_WEST;
        }   else if(directionStr.equals("northsouth"))   {
            direction = PortalDirection.NORTH_SOUTH;
        }   else    {
            player.sendMessage(ChatColor.RED + "Error: Invalid argument. Usage: /spawnportal <eastwest/northsouth>");
            return false;
        }
        Location suitableLoc = pl.spawningSystem.findSuitablePortalLocation(player.getLocation(), direction);
        pl.spawningSystem.spawnPortal(suitableLoc, direction);

        player.sendMessage(ChatColor.GREEN + "Portal spawned successfully");
        player.sendMessage(ChatColor.GREEN + String.format("Location: %s", suitableLoc));
        return true;
    }
    
}