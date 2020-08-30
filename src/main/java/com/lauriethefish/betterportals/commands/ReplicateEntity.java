package com.lauriethefish.betterportals.commands;

import java.util.HashSet;
import java.util.Set;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ReplicateEntity implements CommandExecutor {
    private BetterPortals pl;
    public ReplicateEntity(BetterPortals pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        PlayerData playerData = pl.players.get(player.getUniqueId());
        
        Set<Entity> set = new HashSet<>();
        
        Location playerLoc = player.getLocation();
        Entity closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for(Entity entity : player.getWorld().getEntities())   {
            if(entity instanceof Player)    {
                continue;
            }

            double distance = playerLoc.distance(entity.getLocation());
            if(distance < closestDistance)  {
                closest = entity;
                closestDistance = distance;
            }
        }

        set.add(closest);

        playerData.entityManipulator.swapReplicatedEntities(set, new Vector(0.0, 5.0, 0.0));

        return true;
    }
    
}