package com.lauriethefish.betterportals.bukkit.player.selection;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

// Handles creating the correct portal wand from the config, and checking if an item is/isn't the portal wand
public interface IPortalWandManager {
    @NotNull ItemStack getPortalWand();
    boolean isPortalWand(@NotNull ItemStack item);
}
