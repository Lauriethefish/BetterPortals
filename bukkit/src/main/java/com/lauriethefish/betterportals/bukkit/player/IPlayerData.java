package com.lauriethefish.betterportals.bukkit.player;

import com.lauriethefish.betterportals.bukkit.player.selection.IPlayerSelectionManager;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface IPlayerData {
    /**
     * @return All portals that the player is currently close enough/has permission to view through
     */
    @NotNull Collection<IPortal> getViewedPortals();

    /**
     * @return The player wrapped by this instance
     */
    @NotNull Player getPlayer();

    /**
     * @return The player's current portal selection
     */
    @NotNull IPlayerSelectionManager getSelection();

    /**
     * Sets the player's current selection manager.
     * @param selection The selection manager to set
     */
    void setSelection(@NotNull IPlayerSelectionManager selection);

    /**
     * Called every tick, updates the portal view.
     */
    void onUpdate();

    /**
     * Resets portal views when the plugin is disabled.
     */
    void onPluginDisable();

    interface Factory {
        IPlayerData create(Player player);
    }
}
