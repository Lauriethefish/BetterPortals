package com.lauriethefish.betterportals.bukkit.events;

import com.google.inject.AbstractModule;

public class EventsModule extends AbstractModule {
    @Override
    public void configure() {
        bind(IEventRegistrar.class).to(EventRegistrar.class);

        bind(PortalTeleportationEvents.class).asEagerSingleton();
        bind(SelectionEvents.class).asEagerSingleton();
        bind(SpawningEvents.class).asEagerSingleton();
    }
}
