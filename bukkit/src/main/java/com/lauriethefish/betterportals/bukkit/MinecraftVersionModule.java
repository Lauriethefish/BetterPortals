package com.lauriethefish.betterportals.bukkit;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.IMultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.MultiBlockChangeManagerFactory;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.MultiBlockChangeManager_1_16_2;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.MultiBlockChangeManager_Old;
import com.lauriethefish.betterportals.bukkit.block.rotation.IBlockRotator;
import com.lauriethefish.betterportals.bukkit.block.rotation.LegacyBlockRotator;
import com.lauriethefish.betterportals.bukkit.block.rotation.ModernBlockRotator;
import com.lauriethefish.betterportals.bukkit.chunk.chunkloading.IChunkLoader;
import com.lauriethefish.betterportals.bukkit.chunk.chunkloading.LegacyChunkLoader;
import com.lauriethefish.betterportals.bukkit.chunk.chunkloading.ModernChunkLoader;
import com.lauriethefish.betterportals.bukkit.chunk.generation.IChunkGenerationChecker;
import com.lauriethefish.betterportals.bukkit.chunk.generation.LegacyChunkGenerationChecker;
import com.lauriethefish.betterportals.bukkit.chunk.generation.ModernChunkGenerationChecker;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;

/**
 * Binds the correct implementation of some classes depending on the game version
 */
public class MinecraftVersionModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(IChunkLoader.class).to(VersionUtil.isMcVersionAtLeast("1.13.0") ? ModernChunkLoader.class : LegacyChunkLoader.class);
        bind(IBlockRotator.class).to(VersionUtil.isMcVersionAtLeast("1.13.0") ? ModernBlockRotator.class : LegacyBlockRotator.class);
        bind(IChunkGenerationChecker.class).to(VersionUtil.isMcVersionAtLeast("1.14.0") ? ModernChunkGenerationChecker.class : LegacyChunkGenerationChecker.class);
        install(new FactoryModuleBuilder()
            .implement(IMultiBlockChangeManager.class, VersionUtil.isMcVersionAtLeast("1.16.2") ? MultiBlockChangeManager_1_16_2.class : MultiBlockChangeManager_Old.class)
            .build(MultiBlockChangeManagerFactory.class)
        );
    }
}
