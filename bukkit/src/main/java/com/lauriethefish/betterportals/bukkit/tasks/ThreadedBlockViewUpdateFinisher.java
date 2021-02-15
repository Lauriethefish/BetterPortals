package com.lauriethefish.betterportals.bukkit.tasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Uses a thread instead of a Bukkit task to finish block view updates
 * Probably less idiomatic, but does provide lower latency
 */
@Singleton
public class ThreadedBlockViewUpdateFinisher extends BlockViewUpdateFinisher implements Runnable    {
    private final JavaPlugin pl;

    @Inject
    public ThreadedBlockViewUpdateFinisher(JavaPlugin pl, Logger logger) {
        super(logger);
        this.pl = pl;

        new Thread(this).start();
    }

    @Override
    public void run() {
        logger.fine("Hello from block view update thread!");
        while (pl.isEnabled()) {
            // Make sure to stop if the plugin is disabled

            super.finishPendingUpdates();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        logger.fine("Goodbye from block view update thread!");
    }
}
