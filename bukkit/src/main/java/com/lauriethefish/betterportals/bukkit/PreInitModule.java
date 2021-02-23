package com.lauriethefish.betterportals.bukkit;

import com.google.inject.AbstractModule;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.logging.OverrideLogger;

/**
 * Binds values for loading the config and logging during plugin startup.
 */
public class PreInitModule extends AbstractModule {
    private final BetterPortals pl;
    public PreInitModule(BetterPortals pl) {
        this.pl = pl;
    }

    @Override
    public void configure() {
        bind(Logger.class).toInstance(new OverrideLogger(pl.getLogger()));
    }
}
