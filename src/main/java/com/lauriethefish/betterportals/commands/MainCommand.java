package com.lauriethefish.betterportals.commands;

import com.lauriethefish.betterportals.BetterPortals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;

import net.md_5.bungee.api.ChatColor;

public class MainCommand implements CommandExecutor {
    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GREEN + "BetterPortals" + ChatColor.GRAY + "]" + ChatColor.GREEN + " ";

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
            PluginManager pm = pl.getServer().getPluginManager();
            pm.disablePlugin(pl);
            pm.enablePlugin(pl);

            sender.sendMessage(PREFIX + " Reloaded plugin");
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