package com.lauriethefish.betterportals.commands;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PortalDirection;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
        if(args.length != 3)    {
            return false;
        }

        String directionStr = args[0];
        PortalDirection direction;

        Vector portalSize = new Vector(Double.parseDouble(args[1]), Double.parseDouble(args[2]), 0.0);

        if(directionStr.equalsIgnoreCase("eastwest")) {
            direction = PortalDirection.EAST_WEST;
        }   else if(directionStr.equalsIgnoreCase("northsouth"))   {
            direction = PortalDirection.NORTH_SOUTH;
        }   else    {
            return false;
        }
        Location suitableLoc = pl.spawningSystem.findSuitablePortalLocation(player.getLocation(), direction, portalSize);
        pl.spawningSystem.spawnPortal(suitableLoc, direction, portalSize);

        player.sendMessage(ChatColor.GREEN + "Portal spawned successfully");
        player.sendMessage(ChatColor.GREEN + String.format("Location: %s", suitableLoc));
        return true;
    }
    
}