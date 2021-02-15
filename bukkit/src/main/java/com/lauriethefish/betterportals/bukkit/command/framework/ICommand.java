package com.lauriethefish.betterportals.bukkit.command.framework;

import org.bukkit.command.CommandSender;

public interface ICommand {
    // pathToCall is the path of the command before reaching this point, space separated.
    // For instance, /bp link would be "/bp link ", and the bp parent command would get "/bp "
    // Returns true if successful, false otherwise
    boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException;
}
