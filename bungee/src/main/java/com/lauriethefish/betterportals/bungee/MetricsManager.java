package com.lauriethefish.betterportals.bungee;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.shared.logging.Logger;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

public class MetricsManager {
    private static final int BSTATS_ID = 9948; // ID for the plugins posting on bstats

    @Inject
    public MetricsManager(Plugin pl, Logger logger) {
        logger.fine("Initialising metrics . . .");
        new Metrics(pl, BSTATS_ID);
        logger.fine("Metrics initialised");
    }
}