package com.lauriethefish.betterportals.commands;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.portal.Portal;
import com.lauriethefish.betterportals.selection.PortalSelection;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import net.md_5.bungee.api.ChatColor;

public class MainCommand implements CommandExecutor {
    private BetterPortals pl;
    private final String notEnoughPerms = ChatColor.RED + "You do not have permission to use this command!";

    public MainCommand(BetterPortals pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If the user didn't give any arguments, print the help screen
        if (args.length == 0) {
            showHelpScreen(sender);
            return false;
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("help")) {
            showHelpScreen(sender);
            return true;
        }

        if (subcommand.equals("reload")) {
            if(!sender.hasPermission("betterportals.reload"))   {
                sender.sendMessage(notEnoughPerms);
                return false;
            }

            pl.reloadConfig(); // First reload the config file, since reloading the plugin doesn't do this
                               // apparently
            PluginManager pm = pl.getServer().getPluginManager();
            pm.disablePlugin(pl);
            pm.enablePlugin(pl);

            sender.sendMessage(pl.getChatPrefix() + " Reloaded plugin");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to use this command");
            return false;
        }
        Player player = (Player) sender;
        PlayerData playerData = pl.players.get(player.getUniqueId());

        // Add the wand to the player's inventory if they ran the wand command
        if(subcommand.equals("wand"))   {
            if(!sender.hasPermission("betterportals.wand"))   {
                sender.sendMessage(notEnoughPerms);
                return false;
            }

            player.getInventory().addItem(pl.getPortalWand());
            return true;
        }

        // Handle /bp origin/destination
        boolean setOrigin = subcommand.equals("origin");
        boolean setDestination = subcommand.equals("destination");
        if(setOrigin || setDestination)  {
            if(!sender.hasPermission("betterportals.select"))   {
                sender.sendMessage(notEnoughPerms);
                return false;
            }

            // If no selection has been made, tell the player
            PortalSelection selection = playerData.getSelection();
            if(selection == null || !selection.hasBothPoints())   {
                player.sendMessage(ChatColor.RED + "You must make a selection first");
                return false;
            }

            // Check if the selection was valid
            if(!selection.isValid())    {
                player.sendMessage(ChatColor.RED + "Invalid selection! Portal selections must be on the two corners of a portal");
                return false;
            }

            // Set either the origin or destination selection
            if(setOrigin)   {
                playerData.setOriginSelection(selection);
                player.sendMessage(pl.getChatPrefix() + "Origin portal set");
            }   else    {
                playerData.setDestinationSelection(selection);
                player.sendMessage(pl.getChatPrefix() + "Destination portal set");
            }
            // Reset the selection to null, since it is now used as the origin/destination
            playerData.setSelection(null);
            return true;
        }

        if (subcommand.equals("link")) {
            if(!sender.hasPermission("betterportals.link"))   {
                sender.sendMessage(notEnoughPerms);
                return false;
            }

            PortalSelection originSelection = playerData.getOriginSelection();
            PortalSelection destinationSelection = playerData.getDestinationSelection();
            // Check that both sides of the portal have been selected
            if(originSelection == null || destinationSelection == null) {
                player.sendMessage(ChatColor.RED + "You must select both sides of a portal first. For info, run /bp help");
                return false;
            }

            // If the selected origin and destination have different sizes, tell the player
            if(!originSelection.getPortalSize().equals(destinationSelection.getPortalSize()))   {
                player.sendMessage(ChatColor.RED + "The origin and destination portal must be of the same size");
                return false;
            }

            // Swap around the portal if the user specified
            boolean invert = false;
            if(args.length >= 3)    {
                invert = Boolean.valueOf(args[2]);
            }
            if(invert)  {
                destinationSelection.invertDirection();
            }
            
            pl.registerPortal(new Portal(pl, originSelection, destinationSelection));
            
            // Find if we also need to link the destination back to the origin
            boolean linkTwoWay = false;
            if(args.length >= 2)    {
                linkTwoWay = Boolean.valueOf(args[1]);
            }
            
            if(linkTwoWay)  {
                pl.registerPortal(new Portal(pl, destinationSelection, originSelection));
            }

            player.sendMessage(pl.getChatPrefix() + "Portals linked successfully");
            return true;
        }

        // Removes the closest portal to the player
        if(subcommand.equals("remove")) {
            if(!sender.hasPermission("betterportals.remove"))   {
                sender.sendMessage(notEnoughPerms);
                return false;
            }

            boolean removeOtherSide = true; // Whether or not we will remove any portals coming back to this portal
            if(args.length >= 2)    {
                removeOtherSide = Boolean.valueOf(args[1]);
            }

            // Find the closest portal within 20 blocks
            Portal portal = pl.findClosestPortal(player.getLocation(), 20.0);
            if(portal == null)  {
                player.sendMessage(ChatColor.RED + "No portal close enough found");
                return false;
            }

            // Remove it, and send a message to the player
            portal.remove(removeOtherSide);
            player.sendMessage(pl.getChatPrefix() + "Portal removed");
            return true;
        }

        player.sendMessage(ChatColor.RED + "Unknown Command. For help, run /bp help");
        return false;
    }

    // Prints a help screen showing all the current subcommands
    private void showHelpScreen(CommandSender sender)   {
        sender.sendMessage(ChatColor.GREEN + "Commands: ");
        sender.sendMessage(ChatColor.GRAY + "- bp help: Shows this screen");
        sender.sendMessage(ChatColor.GRAY + "- bp reload: Reloads the config file");
        sender.sendMessage(ChatColor.GRAY + "- bp remove [remove destination]");
        sender.sendMessage(ChatColor.GRAY + "- bp wand");
        sender.sendMessage(ChatColor.GRAY + "- bp origin");
        sender.sendMessage(ChatColor.GRAY + "- bp destination");
        sender.sendMessage(ChatColor.GRAY + "- bp link [2 way] [invert]");
    }
}