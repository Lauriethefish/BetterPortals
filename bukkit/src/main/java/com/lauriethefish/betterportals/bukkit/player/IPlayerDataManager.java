package com.lauriethefish.betterportals.bukkit.player;

import com.lauriethefish.betterportals.bukkit.net.requests.GetSelectionRequest;
import com.lauriethefish.betterportals.bukkit.player.selection.IPortalSelection;
import com.lauriethefish.betterportals.shared.net.requests.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Handles adding and removing player data whenever players join and leave the game, and processing teleport on join
 */
public interface IPlayerDataManager {
    /**
     * @return All registered players. Should be the same as all online players
     */
    @NotNull Collection<IPlayerData> getPlayers();

    /**
     * @param player Player to find the data of
     * @return Data of this player, or null if there isn't any (shouldn't happen)
     */
    @Nullable IPlayerData getPlayerData(@NotNull Player player);

    /**
     * Gets the data for the player with ID <code>uniqueId</code>.
     * @param uniqueId The player's unique ID
     * @return The player's data, or null if there is one
     */
    default @Nullable IPlayerData getPlayerData(@NotNull UUID uniqueId) {
        Player player = Bukkit.getPlayer(uniqueId);
        if(player == null) {return null;}

        return getPlayerData(player);
    }

    /**
     * Resets players' views when the plugin is disabled.
     */
    void onPluginDisable();

    /**
     * Makes it so that when the player with the ID in <code>request</code> joins the game, they will be teleported to the destination position.
     * @param request The request used by another server to set this
     */
    void setTeleportOnJoin(TeleportRequest request);

    /**
     * Sets the external selection of the player with ID <code>uniqueId</code> to <code>selection</code> once they log in, or now if they are already logged in.
     * @param uniqueId The unique ID of the player to set the external selection of
     * @param selection The selection to set
     */
    void setExternalSelectionOnLogin(UUID uniqueId, GetSelectionRequest.ExternalSelectionInfo selection);

    /**
     * Gets the destination selection of the player that is cached upon logout for syncing purposes.
     * @param uniqueId The ID of the player
     * @return The cached selection
     */
    @Nullable IPortalSelection getDestinationSelectionWhenLoggedOut(UUID uniqueId);
}
