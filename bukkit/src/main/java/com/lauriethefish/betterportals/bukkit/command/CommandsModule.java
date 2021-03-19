package com.lauriethefish.betterportals.bukkit.command;

import com.google.inject.AbstractModule;

public class CommandsModule extends AbstractModule {
    @Override
    public void configure() {
        bind(MainCommands.class).asEagerSingleton();
        bind(CustomPortalCommands.class).asEagerSingleton();
    }
}
