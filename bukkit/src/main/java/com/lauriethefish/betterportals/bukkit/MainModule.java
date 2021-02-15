package com.lauriethefish.betterportals.bukkit;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.lauriethefish.betterportals.bukkit.block.FloodFillViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.block.ViewableBlockArrayFactory;
import com.lauriethefish.betterportals.bukkit.block.external.*;
import com.lauriethefish.betterportals.bukkit.command.CommandsModule;
import com.lauriethefish.betterportals.bukkit.entity.*;
import com.lauriethefish.betterportals.bukkit.entity.faking.*;
import com.lauriethefish.betterportals.bukkit.events.EventsModule;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformationsFactory;
import com.lauriethefish.betterportals.bukkit.net.ClientRequestHandler;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.net.PortalClient;
import com.lauriethefish.betterportals.bukkit.player.*;
import com.lauriethefish.betterportals.bukkit.player.selection.*;
import com.lauriethefish.betterportals.bukkit.player.view.IPlayerPortalView;
import com.lauriethefish.betterportals.bukkit.player.view.ViewFactory;
import com.lauriethefish.betterportals.bukkit.player.view.PlayerPortalView;
import com.lauriethefish.betterportals.bukkit.player.view.PlayerPortalViewFactory;
import com.lauriethefish.betterportals.bukkit.player.view.block.*;
import com.lauriethefish.betterportals.bukkit.player.view.entity.IPlayerEntityView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.PlayerEntityView;
import com.lauriethefish.betterportals.bukkit.portal.blend.DimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.blend.IDimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.PortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.*;
import com.lauriethefish.betterportals.bukkit.portal.PortalActivityManager;
import com.lauriethefish.betterportals.bukkit.portal.spawning.*;
import com.lauriethefish.betterportals.bukkit.portal.storage.IPortalStorage;
import com.lauriethefish.betterportals.bukkit.portal.storage.YamlPortalStorage;
import com.lauriethefish.betterportals.bukkit.tasks.BlockViewUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.tasks.MainUpdate;
import com.lauriethefish.betterportals.bukkit.tasks.ThreadedBlockViewUpdateFinisher;
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

        install(new MinecraftVersionModule());

        install(new FactoryModuleBuilder().implement(IPortal.class, Portal.class).build(PortalFactory.class));
        install(new FactoryModuleBuilder().implement(IPortalEntityList.class, PortalEntityList.class).build(PortalEntityListFactory.class));
        install(new FactoryModuleBuilder().implement(IPlayerBlockStates.class, PlayerBlockStates.class).build(PlayerBlockStatesFactory.class));
        install(new FactoryModuleBuilder().implement(IViewableBlockArray.class, FloodFillViewableBlockArray.class).build(ViewableBlockArrayFactory.class));
        install(new FactoryModuleBuilder().implement(IPlayerData.class, PlayerData.class).build(PlayerDataFactory.class));
        install(new FactoryModuleBuilder().implement(IPlayerPortalView.class, PlayerPortalView.class).build(PlayerPortalViewFactory.class));
        install(new FactoryModuleBuilder().implement(IEntityTracker.class, EntityTracker.class).build(EntityTrackerFactory.class));
        install(new FactoryModuleBuilder().build(PortalTransformationsFactory.class));
        install(new FactoryModuleBuilder().implement(IEncryptedObjectStream.class, EncryptedObjectStream.class).build(EncryptedObjectStreamFactory.class));
        install(new FactoryModuleBuilder().implement(IBlockChangeWatcher.class, BlockChangeWatcher.class).build(BlockChangeWatcherFactory.class))   ;
        install(new FactoryModuleBuilder()
                .implement(IPlayerBlockView.class, PlayerBlockView.class)
                .implement(IPlayerEntityView.class, PlayerEntityView.class)
                .build(ViewFactory.class));

        bind(IPortalManager.class).to(PortalManager.class);
        bind(IPortalActivityManager.class).to(PortalActivityManager.class);
        bind(BlockViewUpdateFinisher.class).to(ThreadedBlockViewUpdateFinisher.class);
        bind(IPortalPredicateManager.class).to(PortalPredicateManager.class);
        bind(IPerformanceWatcher.class).to(PerformanceWatcher.class);
        bind(IPlayerSelectionManager.class).to(PlayerSelectionManager.class);
        bind(IPortalSelection.class).to(PortalSelection.class);
        bind(IPortalWandManager.class).to(PortalWandManager.class);
        bind(IPortalSpawner.class).to(PortalSpawner.class);
        bind(IPortalStorage.class).to(YamlPortalStorage.class);
        bind(IEntityPacketManipulator.class).to(EntityPacketManipulator.class);
        bind(IEntityTrackingManager.class).to(EntityTrackingManager.class);
        bind(ICrashHandler.class).to(CrashHandler.class);
        bind(IDimensionBlendManager.class).to(DimensionBlendManager.class);
        bind(IPortalClient.class).to(PortalClient.class);
        bind(IRequestHandler.class).to(ClientRequestHandler.class);
        bind(IExternalBlockWatcherManager.class).to(ExternalBlockWatcherManager.class);

        // Portals need a PortalFactory for their serialization
        // Not really another way we can really get it over there, so gotta use static injection :/
        requestStaticInjection(Portal.class);

        // Base the distance for a hard block reset on the server's view distance
        double blockSendUpdateDistance = Bukkit.getServer().getViewDistance() * 25;
        bind(double.class).annotatedWith(Names.named("blockSendUpdateDistance")).toInstance(blockSendUpdateDistance);

        bind(IPlayerDataManager.class).to(PlayerDataManager.class).asEagerSingleton();
        bind(MainUpdate.class).asEagerSingleton();

        install(new EventsModule());
        install(new CommandsModule());
    }
}
