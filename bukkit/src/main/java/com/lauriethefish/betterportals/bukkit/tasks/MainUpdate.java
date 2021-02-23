package com.lauriethefish.betterportals.bukkit.tasks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.ICrashHandler;
import com.lauriethefish.betterportals.bukkit.block.external.IExternalBlockWatcherManager;
import com.lauriethefish.betterportals.bukkit.entity.faking.IEntityTrackingManager;
import com.lauriethefish.betterportals.bukkit.net.ClientRequestHandler;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.PlayerDataManager;
import com.lauriethefish.betterportals.bukkit.portal.IPortalActivityManager;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Invokes the players to update their portal views every tick.
 * The entry point for most plugin processing each tick.
 */
@Singleton
public class MainUpdate implements Runnable {
    private final JavaPlugin pl;
    private final PlayerDataManager playerDataManager;
    private final IPortalActivityManager activityManager;
    private final IPerformanceWatcher performanceWatcher;
    private final IEntityTrackingManager entityTrackingManager;
    private final ICrashHandler errorHandler;
    private final ClientRequestHandler requestHandler;
    private final IExternalBlockWatcherManager blockWatcherManager;

    @Inject
    public MainUpdate(JavaPlugin pl, PlayerDataManager playerDataManager, IPortalActivityManager activityManager, IPerformanceWatcher performanceWatcher, IEntityTrackingManager entityTrackingManager, ICrashHandler errorHandler, ClientRequestHandler requestHandler, IExternalBlockWatcherManager blockWatcherManager) {
        this.pl = pl;
        this.playerDataManager = playerDataManager;
        this.activityManager = activityManager;
        this.performanceWatcher = performanceWatcher;
        this.entityTrackingManager = entityTrackingManager;
        this.errorHandler = errorHandler;
        this.requestHandler = requestHandler;
        this.blockWatcherManager = blockWatcherManager;
    }

    public void start() {
        pl.getServer().getScheduler().runTaskTimer(pl, this, 0L, 1L);
    }

    @Override
    public void run() {
        try {
            OperationTimer timer = new OperationTimer();
            OperationTimer playerDataTimer = new OperationTimer();
            playerDataManager.getPlayers().forEach(IPlayerData::onUpdate);
            performanceWatcher.putTimeTaken("All player data update", playerDataTimer);

            OperationTimer trackingTimer = new OperationTimer();
            // Update replicated entities
            entityTrackingManager.update();
            performanceWatcher.putTimeTaken("Entity tracking manager update", trackingTimer);

            // Deactivates and view-deactivates any unused portals that were active last tick
            activityManager.postUpdate();

            OperationTimer handlingTimer = new OperationTimer();
            requestHandler.handlePendingRequests();
            performanceWatcher.putTimeTaken("Cross-server request handling", handlingTimer);

            performanceWatcher.putTimeTaken("Main update", timer);

            blockWatcherManager.update();

        }   catch(RuntimeException ex) {
            // An error during main update is bad news.
            // Things are probably now in an invalid state, so we exit the plugin now.
            errorHandler.processCriticalError(ex);
        }
    }
}
