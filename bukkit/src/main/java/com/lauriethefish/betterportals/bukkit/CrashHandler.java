package com.lauriethefish.betterportals.bukkit;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class CrashHandler implements ICrashHandler {
    private final JavaPlugin pl;
    private final Logger logger;

    @Inject
    public CrashHandler(JavaPlugin pl, Logger logger) {
        this.pl = pl;
        this.logger = logger;
    }

    @Override
    public void processCriticalError(Throwable t) {
        logger.severe("A critical error occurred during plugin execution.");
        logger.severe("Please create an issue at %s to get this fixed.", ISSUES_URL);
        t.printStackTrace();

        shutdownPlugin();
    }

    private void shutdownPlugin() {
        logger.info("Attempting to shut down the plugin . . .");
        pl.getServer().getPluginManager().disablePlugin(pl);
    }
}
