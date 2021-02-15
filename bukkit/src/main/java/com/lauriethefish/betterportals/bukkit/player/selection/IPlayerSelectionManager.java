package com.lauriethefish.betterportals.bukkit.player.selection;

import com.lauriethefish.betterportals.bukkit.command.framework.CommandException;
import com.lauriethefish.betterportals.bukkit.net.requests.GetSelectionRequest;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Stores the selection of both sides of a portal by a player
public interface IPlayerSelectionManager {
    @NotNull IPortalSelection getCurrentlySelecting();
    @Nullable IPortalSelection getOriginSelection();
    @Nullable IPortalSelection getDestSelection();

    @Nullable GetSelectionRequest.ExternalSelectionInfo getExternalSelection();
    void setExternalSelection(@Nullable GetSelectionRequest.ExternalSelectionInfo selection);

    // All of these should throw CommandException with the error message if they fail
    void trySelectOrigin() throws CommandException;
    void trySelectDestination() throws CommandException;
    void tryCreateFromSelection(Player player, boolean twoWay, boolean invert) throws CommandException;
    void tryCreateFromExternalSelection(Player player, boolean invert) throws CommandException;
}
