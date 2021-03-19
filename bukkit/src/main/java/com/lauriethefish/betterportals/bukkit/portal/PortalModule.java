package com.lauriethefish.betterportals.bukkit.portal;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bukkit.entity.IPortalEntityList;
import com.lauriethefish.betterportals.bukkit.entity.PortalEntityList;
import com.lauriethefish.betterportals.bukkit.entity.PortalEntityListFactory;
import com.lauriethefish.betterportals.bukkit.math.PortalTransformationsFactory;
import com.lauriethefish.betterportals.bukkit.portal.blend.DimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.blend.IDimensionBlendManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.IPortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.predicate.PortalPredicateManager;
import com.lauriethefish.betterportals.bukkit.portal.spawning.IPortalSpawner;
import com.lauriethefish.betterportals.bukkit.portal.spawning.PortalSpawner;
import com.lauriethefish.betterportals.bukkit.portal.storage.IPortalStorage;
import com.lauriethefish.betterportals.bukkit.portal.storage.YamlPortalStorage;

public class PortalModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IPortal.class, Portal.class)
                .build(IPortal.Factory.class)
        );
        install(new FactoryModuleBuilder()
                .implement(IPortalEntityList.class, PortalEntityList.class)
                .build(PortalEntityListFactory.class)
        );
        install(new FactoryModuleBuilder().build(PortalTransformationsFactory.class));

        bind(IPortalPredicateManager.class).to(PortalPredicateManager.class);
        bind(IPortalStorage.class).to(YamlPortalStorage.class);

        bind(IPortalManager.class).to(PortalManager.class);
        bind(IPortalActivityManager.class).to(PortalActivityManager.class);

        bind(IPortalSpawner.class).to(PortalSpawner.class);
        bind(IDimensionBlendManager.class).to(DimensionBlendManager.class);


        // Portals need a PortalFactory for their deserialization
        // Not really another way we can really get it over there, so gotta use static injection :/
        requestStaticInjection(Portal.class);
    }
}
