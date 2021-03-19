package com.lauriethefish.betterportals.bukkit;

import com.google.inject.AbstractModule;
import com.lauriethefish.betterportals.bukkit.block.BlockModule;
import com.lauriethefish.betterportals.bukkit.command.CommandsModule;
import com.lauriethefish.betterportals.bukkit.entity.EntityModule;
import com.lauriethefish.betterportals.bukkit.events.EventsModule;
import com.lauriethefish.betterportals.bukkit.net.*;
import com.lauriethefish.betterportals.bukkit.player.*;
import com.lauriethefish.betterportals.bukkit.portal.*;
import com.lauriethefish.betterportals.bukkit.tasks.BlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.tasks.ThreadedBlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.PerformanceWatcher;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.logging.OverrideLogger;
import org.bukkit.plugin.java.JavaPlugin;

public class MainModule extends AbstractModule {
    private final BetterPortals pl;

    public MainModule(BetterPortals pl) {
        this.pl = pl;
    }

    @Override
    protected void configure() {
        bind(JavaPlugin.class).toInstance(pl);
        bind(BetterPortals.class).toInstance(pl);
        bind(Logger.class).toInstance(new OverrideLogger(pl.getLogger()));

        bind(BlockUpdateFinisher.class).to(ThreadedBlockUpdateFinisher.class);
        bind(IPerformanceWatcher.class).to(PerformanceWatcher.class);
        bind(ICrashHandler.class).to(CrashHandler.class);

        bind(MetricsManager.class).asEagerSingleton();

        install(new EventsModule());
        install(new CommandsModule());
        install(new PortalModule());
        install(new BlockModule());
        install(new NetworkModule());
        install(new PlayerModule());
        install(new EntityModule());
        install(new MinecraftVersionModule());
    }
}
