package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class EventRegistrar implements IEventRegistrar {
    private final JavaPlugin pl;
    private final Set<Listener> allRegisteredListeners = new HashSet<>();
    private final Logger logger;

    @Inject
    public EventRegistrar(JavaPlugin pl, Logger logger) {
        this.pl = pl;
        this.logger = logger;
    }

    @Override
    public void register(@NotNull Listener listener) {
        pl.getServer().getPluginManager().registerEvents(listener, pl);
        allRegisteredListeners.add(listener);
    }

    @Override
    public void onPluginReload() {
        logger.fine("Re-registering events . . .");
        for(Listener listener : allRegisteredListeners) {
            pl.getServer().getPluginManager().registerEvents(listener, pl);
        }
    }
}
