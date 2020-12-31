package com.lauriethefish.betterportals.bukkit;

import org.bstats.bukkit.Metrics;

public class MetricsManager {
    private static final int BSTATS_ID = 8669; // ID for the plugins posting on bstats

    private BetterPortals pl;
    private Metrics metrics;

    public MetricsManager(BetterPortals pl) {
        this.pl = pl;

        pl.logDebug("Initialising metrics...");
        metrics = new Metrics(pl, BSTATS_ID);
        addCharts();
    }

    private void addCharts() {
        pl.logDebug("Adding charts...");
        // Show me the total portals active
        metrics.addCustomChart(new Metrics.SingleLineChart("portals_active", () -> {
            return pl.getPortals().size() / 2; // Divide by 2, since each portal is 2 list items
        }));
        // Various charts for config options
        metrics.addCustomChart(new Metrics.SimplePie("render_distance_xz", () -> String.valueOf(pl.getLoadedConfig().getRendering().getMaxXZ())));
        metrics.addCustomChart(new Metrics.SimplePie("render_distance_y", () -> String.valueOf(pl.getLoadedConfig().getRendering().getMaxY())));
        metrics.addCustomChart(new Metrics.SimplePie("entities_enabled", () -> {
            // Format it nicely instead of true or false
            if(pl.getLoadedConfig().isEntitySupportEnabled())  {
                return "Entities";
            }   else    {
                return "No Entities";
            }
        }));
    }
}
