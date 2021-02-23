package com.lauriethefish.betterportals.bukkit.events;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Used to easily re-register listeners when the plugin reloads.
 */
public interface IEventRegistrar {
    /**
     * Registers <code>listener</code> with Bukkit.
     * @param listener The listener to register
     */
    void register(@NotNull Listener listener);

    /**
     * Re-registers all previously registered listeners
     */
    void onPluginReload();
}
