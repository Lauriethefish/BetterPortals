package com.lauriethefish.betterportals.bungee;

import org.bstats.bungeecord.Metrics;

public class MetricsManager {
    private static final int BSTATS_ID = 9948; // ID for the plugins posting on bstats

    private final BetterPortals pl;
    private final Metrics metrics;

    public MetricsManager(BetterPortals pl) {
        this.pl = pl;

        pl.logDebug("Initialising metrics...");
        metrics = new Metrics(pl, BSTATS_ID);
    }
}
