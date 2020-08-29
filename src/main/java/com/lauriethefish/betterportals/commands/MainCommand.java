package com.lauriethefish.betterportals.commands;

import java.util.HashMap;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class MainCommand implements CommandExecutor {
    private static final String chatPrefix = ChatColor.GRAY + "[" + ChatColor.GREEN + "BetterPortals" + ChatColor.GRAY + "]" + ChatColor.GREEN + " ";

    private BetterPortals pl;
    public MainCommand(BetterPortals pl)   {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)    {
        // If the user didn't give any arguments, print the help screen
        if(args.length == 0)    {
            showHelpScreen(sender);
            return false;
        }

        String subcommand = args[0];
        if(subcommand.equals("help"))   {
            showHelpScreen(sender);
            return true;
        }

        if(subcommand.equals("reload")) {
            // Reset each player's block states, as the portal size may have changed
            for(PlayerData player : pl.players.values())    {
                if(player.player.isOnline())    {
                    // We don't use resetSurroundingBlockStates here, since that would send packets that change the blocks back, causing the portal to flicker
                    player.surroundingPortalBlockStates = new HashMap<>();
                }
            }

            // Reload the config, printing an error if it fails
            pl.reloadConfig(); // First reload the file on disc
            if(!pl.loadConfig())    { // Then load all the values
                sender.sendMessage(chatPrefix + ChatColor.RED + "Error reloading the config file. Please check config.yml for errors.");
            }   else    {
                sender.sendMessage(chatPrefix + "Config reloaded.");
            }
        }

        return true;
    }

    // Prints a help screen showing all the current subcommands
    private void showHelpScreen(CommandSender sender)   {
        sender.sendMessage(ChatColor.GREEN + "Commands: ");
        sender.sendMessage(ChatColor.GRAY + "- bp help: Shows this screen");
        sender.sendMessage(ChatColor.GRAY + "- bp reload: Reloads the config file");
    }
}