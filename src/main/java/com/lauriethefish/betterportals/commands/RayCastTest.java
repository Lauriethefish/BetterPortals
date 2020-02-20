package com.lauriethefish.betterportals.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

// This class is not commented (apart from here), because it is unused and was only used for debugging
public class RayCastTest implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.RED + "Sorry, this command was for testing only and is now unimplemented");
        return false;
    }
}