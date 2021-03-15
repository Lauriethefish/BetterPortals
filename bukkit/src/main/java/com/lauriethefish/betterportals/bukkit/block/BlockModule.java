package com.lauriethefish.betterportals.bukkit.block;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.block.external.*;
import com.lauriethefish.betterportals.bukkit.player.view.ViewFactory;
import com.lauriethefish.betterportals.bukkit.player.view.block.IPlayerBlockView;
import com.lauriethefish.betterportals.bukkit.player.view.block.PlayerBlockView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.IPlayerEntityView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.PlayerEntityView;

public class BlockModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IViewableBlockArray.class, FloodFillViewableBlockArray.class)
                .build(IViewableBlockArray.Factory.class)
        );

        install(new FactoryModuleBuilder()
                .implement(IBlockChangeWatcher.class, BlockChangeWatcher.class)
                .build(IBlockChangeWatcher.Factory.class)
        );

        install(new FactoryModuleBuilder()
                .implement(IPlayerBlockView.class, PlayerBlockView.class)
                .implement(IPlayerEntityView.class, PlayerEntityView.class)
                .build(ViewFactory.class)
        );


        bind(IExternalBlockWatcherManager.class).to(ExternalBlockWatcherManager.class);
    }
}
