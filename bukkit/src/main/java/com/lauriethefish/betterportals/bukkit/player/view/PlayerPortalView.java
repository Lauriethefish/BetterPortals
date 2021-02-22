package com.lauriethefish.betterportals.bukkit.player.view;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.player.view.block.IPlayerBlockView;
import com.lauriethefish.betterportals.bukkit.player.view.entity.IPlayerEntityView;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

// Represents each portal that a player is looking through
public class PlayerPortalView implements IPlayerPortalView  {
    private final Player player;
    private final Logger logger;
    private final RenderConfig renderConfig;
    private final IPerformanceWatcher performanceWatcher;

    private final IPlayerBlockView blockView;
    private final IPlayerEntityView entityView;

    private Location previousPosition = null;
    private int ticksSinceStarted = 0;

    @Inject
    public PlayerPortalView(@Assisted Player player, @Assisted IPortal viewedPortal, ViewFactory viewFactory, Logger logger, RenderConfig renderConfig, IPerformanceWatcher performanceWatcher, MiscConfig miscConfig) {
        this.player = player;
        this.logger = logger;
        this.renderConfig = renderConfig;
        this.performanceWatcher = performanceWatcher;

        this.blockView = viewFactory.createBlockView(player, viewedPortal);
        if(!miscConfig.isEntitySupportEnabled()) {
            this.entityView = null;
        }   else {
            this.entityView = viewFactory.createEntityView(player, viewedPortal);
        }
    }

    private boolean shouldSendPackets() {
        if(previousPosition == null) {return false;} // This condition shouldn't happen unless a player gets near a portal really quickly before going away, but to be on the safe side
        Location currentPosition = player.getLocation();

        logger.finer("Previous pos: %s, Current pos: %s", previousPosition, currentPosition);
        if(previousPosition.getWorld() != currentPosition.getWorld()) {return false;} // No need to bother if the player has switched worlds
        // Roughly measure whether or not the player moved so far that they're out of render distance
        // This is needed to not send the block reset packets if the player moved a long distance away
        // Changing blocks in chunks that the player can't see is unsafe!
        return currentPosition.distance(previousPosition) < Bukkit.getViewDistance() * 16 * 2;
    }

    @Override
    public void update() {
        OperationTimer timer = new OperationTimer();
        boolean moved = !player.getLocation().equals(previousPosition);

        // We refresh the block view every N ticks so that if the client doesn't change some of the blocks, they will be resent
        if(ticksSinceStarted % renderConfig.getBlockStateRefreshInterval() == 0) {
            blockView.update(true);
        }   else if(moved) { // Otherwise, an update only happens when we move to save on performance
            blockView.update(false);
        }

        // This must be called every tick, since entities can move and they might be visible now
        if(entityView != null) {
            entityView.update();
        }

        ticksSinceStarted++;
        performanceWatcher.putTimeTaken("Player portal view update", timer);
        previousPosition = player.getLocation();
    }

    @Override
    public void onDeactivate() {
        boolean shouldSendPackets = shouldSendPackets();

        blockView.onDeactivate(shouldSendPackets);
        if(entityView != null) {
            entityView.onDeactivate(shouldSendPackets);
        }
    }
}
