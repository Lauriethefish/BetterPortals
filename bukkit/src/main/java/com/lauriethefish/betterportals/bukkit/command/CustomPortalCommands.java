package com.lauriethefish.betterportals.bukkit.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandException;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandTree;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.*;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class CustomPortalCommands {
    private static final String[] EASTER_EGG_NAMES = new String[]{"dinnerbone"};
    private static final double MODIFY_DISTANCE = 20.0;

    private final IPortalManager portalManager;
    private final MessageConfig messageConfig;
    private final IPortal.Factory portalFactory;

    @Inject
    public CustomPortalCommands(CommandTree commandTree, IPortalManager portalManager, MessageConfig messageConfig, IPortal.Factory portalFactory) {
        this.portalManager = portalManager;
        this.messageConfig = messageConfig;
        this.portalFactory = portalFactory;

        commandTree.registerCommands(this);
    }

    private @NotNull IPortal getClosestPortal(Player player) throws CommandException {
        IPortal portal = portalManager.findClosestPortal(player.getLocation(), MODIFY_DISTANCE);
        if(portal == null) {
            throw new CommandException(messageConfig.getErrorMessage("noPortalCloseEnough"));
        }

        return portal;
    }

    @Command
    @Path("betterportals/remove")
    @RequiresPermissions("betterportals.remove")
    @RequiresPlayer
    @Aliases({"delete", "del"})
    @Description("Removes the nearest portal within 20 blocks of the player")
    @Argument(name = "removeDestination?", defaultValue = "true")
    public boolean deleteNearest(Player player, boolean removeDestination) throws CommandException {
        IPortal portal = getClosestPortal(player);

        // If the player doesn't own the portal, and doesn't have permission to remove portals that aren't theirs, don't remove
        if(!player.hasPermission("betterportals.remove.others") && !player.getUniqueId().equals(portal.getOwnerId())) {
            throw new CommandException(messageConfig.getErrorMessage("removeNotOwnedByPlayer"));
        }

        portalManager.removePortal(portal);
        // We can't remove the destination on cross-server portals
        if(removeDestination && !portal.isCrossServer()) {
            Location destPosition = portal.getDestPos().getLocation();
            portalManager.removePortalsAt(destPosition);
        }

        player.sendMessage(messageConfig.getChatMessage("portalRemoved"));
        return true;
    }

    @Command
    @Path("betterportals/setOrigin")
    @Aliases("origin")
    @RequiresPermissions("betterportals.select")
    @RequiresPlayer
    @Description("Sets the current portal wand selection as your origin position")
    public boolean setOrigin(IPlayerData playerData) throws CommandException    {
        playerData.getSelection().trySelectOrigin();
        playerData.getPlayer().sendMessage(messageConfig.getChatMessage("originPortalSet"));
        return true;
    }

    @Command
    @Path("betterportals/setDestination")
    @Aliases({"destination", "dest"})
    @RequiresPermissions("betterportals.select")
    @RequiresPlayer
    @Description("Sets the current portal wand selection as your destination position")
    public boolean setDestination(IPlayerData playerData) throws CommandException    {
        playerData.getSelection().trySelectDestination();
        playerData.getPlayer().sendMessage(messageConfig.getChatMessage("destPortalSet"));
        return true;
    }

    @Command
    @Path("betterportals/linkPortals")
    @Aliases("link")
    @RequiresPermissions("betterportals.link")
    @RequiresPlayer
    @Description("Links the origin and destination portal together")
    @Argument(name = "twoWay?", defaultValue = "false")
    @Argument(name = "invert?", defaultValue = "false")
    public boolean linkPortals(IPlayerData playerData, String twoWayStr, String invertStr) throws CommandException  {
        boolean twoWay = twoWayStr.equalsIgnoreCase("true") || twoWayStr.equalsIgnoreCase("twoWay") || twoWayStr.equalsIgnoreCase("dual");
        boolean invert = invertStr.equalsIgnoreCase("true") || invertStr.equalsIgnoreCase("invert");

        playerData.getSelection().tryCreateFromSelection(playerData.getPlayer(), twoWay, invert);
        playerData.getPlayer().sendMessage(messageConfig.getChatMessage("portalsLinked"));
        return true;
    }

    @Command
    @Path("betterportals/linkExternalPortals")
    @Aliases("linkexternal")
    @RequiresPermissions("betterportals.linkexternal")
    @RequiresPlayer
    @Description("Links the origin selection on this server with a destination on another server")
    @Argument(name = "invert?", defaultValue = "false")
    public boolean linkExternalPortals(IPlayerData playerData, boolean invert) throws CommandException  {
        playerData.getSelection().tryCreateFromExternalSelection(playerData.getPlayer(), invert);
        playerData.getPlayer().sendMessage(messageConfig.getChatMessage("portalsLinked"));
        return true;
    }

    @Command
    @Path("betterportals/wand")
    @RequiresPermissions("betterportals.wand")
    @RequiresPlayer
    @Description("Gives you the wand for selecting portals")
    public boolean getPortalWand(Player player) {
        player.getInventory().addItem(messageConfig.getPortalWand());
        return true;
    }

    // Some of the easter eggs require a portal recreation to work properly, this handles that
    private void setName(IPortal portal, String name) {
        boolean isEgg = false;
        for(String egg : EASTER_EGG_NAMES) {
            if (egg.equalsIgnoreCase(name) || egg.equalsIgnoreCase(portal.getName())) {
                isEgg = true;
                break;
            }
        }
        // Non-easter-egg portals can just get their name set normally
        if(!isEgg) {
            portal.setName(name);
            return;
        }

        portalManager.removePortal(portal);
        IPortal replacement = portalFactory.create(
                portal.getOriginPos(),
                portal.getDestPos(),
                portal.getSize(),
                portal.isCustom(),
                portal.getId(),
                portal.getOwnerId(),
                name
        );

        portalManager.registerPortal(replacement);
    }

    @Command
    @Path("betterportals/setPortalName")
    @RequiresPermissions("betterportals.setname")
    @Argument(name = "newName")
    @Aliases("setname")
    @RequiresPlayer
    @Description("Sets the name of the nearest portal within 20 blocks")
    public boolean setName(Player player, String newName) throws CommandException   {
        IPortal portal = getClosestPortal(player);

        // If the player doesn't own the portal, and doesn't have permission to remove portals that aren't theirs, don't remove
        if(!player.hasPermission("betterportals.setname.others") && !player.getUniqueId().equals(portal.getOwnerId())) {
            throw new CommandException(messageConfig.getErrorMessage("nameNotOwnedbyPlayer"));
        }

        // Nether portals cannot be named!
        if(portal.isNetherPortal()) {
            throw new CommandException(messageConfig.getErrorMessage("nameNetherPortal"));
        }

        setName(portal, newName);
        player.sendMessage(messageConfig.getChatMessage("changedName"));
        return true;
    }

    @Command
    @Path("betterportals/getportalname")
    @RequiresPermissions("betterportals.getname")
    @RequiresPlayer
    @Aliases("getname")
    @Description("Tells you the name of the nearest portal within 20 blocks")
    public boolean getName(Player player) throws CommandException {
        IPortal portal = getClosestPortal(player);

        String name = portal.getName();
        if(name == null) {
            throw new CommandException(messageConfig.getErrorMessage("noName"));
        }

        String nameFormat = messageConfig.getChatMessage("currentName");
        nameFormat = nameFormat.replace("{name}", portal.getName());
        player.sendMessage(nameFormat);
        return true;
    }
}
