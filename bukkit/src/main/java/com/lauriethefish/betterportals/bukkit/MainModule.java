package com.lauriethefish.betterportals.bukkit;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.lauriethefish.betterportals.bukkit.block.BlockModule;
import com.lauriethefish.betterportals.bukkit.block.FloodFillViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.block.ViewableBlockArrayFactory;
import com.lauriethefish.betterportals.bukkit.block.external.*;
import com.lauriethefish.betterportals.bukkit.command.CommandsModule;
import com.lauriethefish.betterportals.bukkit.entity.EntityModule;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.entity.PortalEntityList;
import com.lauriethefish.betterportals.bukkit.entity.PortalEntityListFactory;
import com.lauriethefish.betterportals.bukkit.entity.faking.*;
import com.lauriethefish.betterportals.bukkit.events.EventsModule;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformationsFactory;
import com.lauriethefish.betterportals.bukkit.net.*;
import com.lauriethefish.betterportals.bukkit.player.*;
import com.lauriethefish.betterportals.bukkit.player.selection.*;
import com.lauriethefish.betterportals.bukkit.player.view.IPlayerPortalView;
import com.lauriethefish.betterportals.bukkit.player.view.PlayerPortalView;
import com.lauriethefish.betterportals.bukkit.player.view.PlayerPortalViewFactory;
import com.lauriethefish.betterportals.bukkit.player.view.ViewFactory;
import com.lauriethefish.betterportals.bukkit.player.view.block.*;
import com.lauriethefish.betterportals.bukkit.player.view.entity.IPlayerEntityView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.PlayerEntityView;
import com.lauriethefish.betterportals.bukkit.portal.*;
import com.lauriethefish.betterportals.bukkit.portal.blend.DimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.blend.IDimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.PortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.spawning.IPortalSpawner;
import com.lauriethefish.betterportals.bukkit.portal.spawning.PortalSpawner;
import com.lauriethefish.betterportals.bukkit.portal.storage.IPortalStorage;
import com.lauriethefish.betterportals.bukkit.portal.storage.YamlPortalStorage;
import com.lauriethefish.betterportals.bukkit.tasks.BlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.tasks.MainUpdate;
import com.lauriethefish.betterportals.bukkit.tasks.ThreadedBlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.PerformanceWatcher;
import com.lauriethefish.betterportals.shared.net.IRequestHandler;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStream;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStreamFactory;
import com.lauriethefish.betterportals.shared.net.encryption.IEncryptedObjectStream;
import org.bukkit.Bukkit;
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
