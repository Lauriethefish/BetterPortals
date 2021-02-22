package com.lauriethefish.betterportals.bukkit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class MetricsManager {
    private static final int BSTATS_ID = 8669;

    private final Logger logger;
    private final RenderConfig renderConfig;
    private final MiscConfig miscConfig;
    private final ProxyConfig proxyConfig;
    private final IPortalManager portalManager;

    private final Metrics metrics;

    @Inject
    public MetricsManager(JavaPlugin pl, Logger logger, RenderConfig renderConfig, MiscConfig miscConfig, IPortalManager portalManager, ProxyConfig proxyConfig) {
        this.logger = logger;
        this.renderConfig = renderConfig;
        this.miscConfig = miscConfig;
        this.portalManager = portalManager;
        this.proxyConfig = proxyConfig;

        logger.fine("Initialising metrics . . .");
        this.metrics = new Metrics(pl, BSTATS_ID);
        addCharts();
        logger.fine("Metrics initialised");
    }

    private void addCharts() {
        logger.fine("Adding charts . . .");
        metrics.addCustomChart(new SingleLineChart("portals_active", () -> {
            return portalManager.getAllPortals().size() / 2; // Divide by 2, since each portal is 2 list items
        }));

        metrics.addCustomChart(new SimplePie("render_distance_xz", () -> String.valueOf(renderConfig.getMaxXZ())));
        metrics.addCustomChart(new SimplePie("render_distance_y", () -> String.valueOf(renderConfig.getMaxY())));
        metrics.addCustomChart(new SimplePie("entities_enabled", () -> {
            // Format it nicely instead of true or false
            if(miscConfig.isEntitySupportEnabled())  {
                return "Entities";
            }   else    {
                return "No Entities";
            }
        }));

        metrics.addCustomChart(new SimplePie("cross-server_portals", () -> proxyConfig.isEnabled() ? "Cross server portals" : "No cross server portals"));
    }
}
