package com.lauriethefish.betterportals.bukkit.commands;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.Config;
import com.lauriethefish.betterportals.bukkit.PlayerData;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.selection.PortalSelection;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

public class MainCommand implements CommandExecutor {
    private BetterPortals pl;
    private Config config;

    public MainCommand(BetterPortals pl) {
        this.pl = pl;
        this.config = pl.config;
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
                sender.sendMessage(config.getErrorMessage("notEnoughPerms"));
                return false;
            }

            pl.reloadConfig(); // First reload the config file, since reloading the plugin doesn't do this apparently
            PluginManager pm = pl.getServer().getPluginManager();
            pm.disablePlugin(pl);
            pm.enablePlugin(pl);

            sender.sendMessage(config.getChatMessage("reload"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(config.getErrorMessage("mustBePlayer"));
            return false;
        }
        Player player = (Player) sender;
        PlayerData playerData = pl.getPlayerData(player);

        // Add the wand to the player's inventory if they ran the wand command
        if(subcommand.equals("wand"))   {
            if(!sender.hasPermission("betterportals.wand"))   {
                sender.sendMessage(config.getErrorMessage("notEnoughPerms"));
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
                sender.sendMessage(config.getErrorMessage("notEnoughPerms"));
                return false;
            }

            // If no selection has been made, tell the player
            PortalSelection selection = playerData.getSelection();
            if(selection == null || !selection.hasBothPoints())   {
                player.sendMessage(config.getErrorMessage("mustMakeSelection"));
                return false;
            }

            // Check if the selection was valid
            if(!selection.isValid())    {
                player.sendMessage(config.getErrorMessage("invalidSelection"));
                return false;
            }

            // Set either the origin or destination selection
            if(setOrigin)   {
                playerData.setOriginSelection(selection);
                player.sendMessage(config.getChatMessage("originPortalSet"));
            }   else    {
                playerData.setDestinationSelection(selection);
                player.sendMessage(config.getChatMessage("destPortalSet"));
            }
            // Reset the selection to null, since it is now used as the origin/destination
            playerData.setSelection(null);
            return true;
        }

        if (subcommand.equals("link")) {
            if(!sender.hasPermission("betterportals.link"))   {
                sender.sendMessage(config.getErrorMessage("notEnoughPerms"));
                return false;
            }

            PortalSelection originSelection = playerData.getOriginSelection();
            PortalSelection destinationSelection = playerData.getDestinationSelection();
            // Check that both sides of the portal have been selected
            if(originSelection == null || destinationSelection == null) {
                player.sendMessage(config.getErrorMessage("mustSelectBothSides"));
                return false;
            }

            // If the selected origin and destination have different sizes, tell the player
            if(!originSelection.getPortalSize().equals(destinationSelection.getPortalSize()))   {
                player.sendMessage(config.getErrorMessage("differentSizes"));
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
            
            pl.registerPortal(new Portal(pl, originSelection, destinationSelection, player));
            
            // Find if we also need to link the destination back to the origin
            boolean linkTwoWay = false;
            if(args.length >= 2)    {
                linkTwoWay = Boolean.valueOf(args[1]);
            }
            
            if(linkTwoWay)  {
                pl.registerPortal(new Portal(pl, destinationSelection, originSelection, player));
            }


            player.sendMessage(config.getChatMessage("portalsLinked"));
            return true;
        }

        // Removes the closest portal to the player
        if(subcommand.equals("remove")) {
            if(!sender.hasPermission("betterportals.remove"))   {
                sender.sendMessage(config.getErrorMessage("notEnoughPerms"));
                return false;
            }

            boolean removeOtherSide = true; // Whether or not we will remove any portals coming back to this portal
            if(args.length >= 2)    {
                removeOtherSide = Boolean.valueOf(args[1]);
            }

            // Find the closest portal within 20 blocks
            Portal portal = pl.findClosestPortal(player.getLocation(), 20.0);
            // Send an error if there is no portal close enough
            if(portal == null)  {
                player.sendMessage(config.getErrorMessage("noPortalCloseEnough"));
                return false;
            }

            // If this portal isn't owned by the player, and they don't have permission to remove the portals of others, don't remove the portal
            if(!player.getUniqueId().equals(portal.getOwner()) && !player.hasPermission("betterportals.remove.others")) {
                player.sendMessage(config.getErrorMessage("portalNotOwnedByPlayer"));
                return false;
            }

            // Remove it, and send a message to the player
            portal.remove(removeOtherSide);
            player.sendMessage(config.getChatMessage("portalRemoved"));
            return true;
        }

        // Used for linking a position on this server with a cross-server portal to an external server.
        if(subcommand.equals("linkexternal")) {
            // Check the argument length
            if(args.length != 7) {
                player.sendMessage(config.getErrorMessage("wrongNumberOfArgs"));
                return false;
            }

            if(playerData.getOriginSelection() == null) {
                player.sendMessage(config.getErrorMessage("mustSelectOrigin"));
                return false;
            }
            // Find the origin pos and size from the player's selection
            PortalPosition originPos = playerData.getOriginSelection().getPortalPosition();
            Vector size = playerData.getOriginSelection().getPortalSize();

            // Find all the various parameters of the destination
            PortalPosition destPos;
            try {
                Vector destLoc = new Vector(Double.parseDouble(args[1]), Double.parseDouble(args[2]), Double.parseDouble(args[3]));
                String destServer = args[4];
                String destWorldName = args[5];
                PortalDirection destDirection = PortalDirection.valueOf(args[6]);
                destPos = new PortalPosition(destLoc, destDirection, destServer, destWorldName);
            }   catch(IllegalArgumentException ex) {
                player.sendMessage(config.getErrorMessage("invalidArgs"));
                return false;
            }
            
            // Register the portal with the plugin
            pl.registerPortal(new Portal(pl, originPos, destPos, size, true, player.getUniqueId()));
            player.sendMessage(config.getChatMessage("portalsLinked"));
            return true;
        }

        player.sendMessage(config.getErrorMessage("unknownCommand"));
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
        sender.sendMessage(ChatColor.GRAY + "- bp linkexternal <destX> <destY> <destZ> <destServer> <destWorldName> <destDirection>");
    }
}