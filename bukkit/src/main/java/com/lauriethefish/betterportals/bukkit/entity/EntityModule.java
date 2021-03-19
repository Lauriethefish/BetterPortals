package com.lauriethefish.betterportals.bukkit.entity;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.entity.faking.*;

public class EntityModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IEntityTracker.class, EntityTracker.class)
                .build(IEntityTracker.Factory.class)
        );

        bind(IEntityPacketManipulator.class).to(EntityPacketManipulator.class);
        bind(IEntityTrackingManager.class).to(EntityTrackingManager.class);
    }
}
