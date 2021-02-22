package com.lauriethefish.betterportals.bukkit.player.view.block;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.IViewableBlockArray;
import com.lauriethefish.betterportals.bukkit.block.ViewableBlockInfo;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.IMultiBlockChangeManager;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.MultiBlockChangeManagerFactory;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.math.IntVector;
import com.lauriethefish.betterportals.bukkit.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.tasks.BlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerBlockView implements IPlayerBlockView   {
    private final Player player;
    private final IPortal portal;

    private final MultiBlockChangeManagerFactory multiBlockChangeManagerFactory;
    private final IPlayerBlockStates blockStates;
    // Avoid resetting block states while they're being updated asynchronously
    private final ReentrantLock statesLock = new ReentrantLock(true);
    private final Logger logger;
    private final BlockUpdateFinisher updateFinisher;
    private final IPerformanceWatcher performanceWatcher;
    private final boolean shouldHidePortalBlocks;

    // Stored here since we can't access the Bukkit API from another thread
    private volatile Vector playerPosition;

    // Used to avoid a situation where the portal is no longer viewable and the blocks were reset, then an async update comes in and resends them
    private volatile boolean didDeactivate = false;

    @Inject
    public PlayerBlockView(@Assisted Player player, @Assisted IPortal portal,
                           MultiBlockChangeManagerFactory multiBlockChangeManagerFactory, PlayerBlockStatesFactory blockStatesFactory,
                           Logger logger, BlockUpdateFinisher updateFinisher, IPerformanceWatcher performanceWatcher, RenderConfig renderConfig) {
        this.player = player;
        this.portal = portal;
        this.multiBlockChangeManagerFactory = multiBlockChangeManagerFactory;
        this.blockStates = blockStatesFactory.create(player);
        this.logger = logger;
        this.updateFinisher = updateFinisher;
        this.performanceWatcher = performanceWatcher;
        this.shouldHidePortalBlocks = portal.isNetherPortal() && renderConfig.isPortalBlocksHidden();
    }

    // Called whenever the player moves
    @Override
    public void update(boolean refresh) {
        playerPosition = player.getEyeLocation().toVector();
        updateFinisher.scheduleUpdate(this, refresh);

        if(refresh && shouldHidePortalBlocks) {
            setPortalBlocks(WrappedBlockData.createData(Material.AIR));
        }
    }

    // Called whenever the player is no longer activating the portal
    @Override
    public void onDeactivate(boolean shouldResetStates) {
        didDeactivate = true;
        logger.finer("Player block view deactivating. Should reset states: %b", shouldResetStates);

        if(shouldResetStates) {
            // Reset the portal blocks back to the portal material. Avoid reshowing them if the portal is no longer registered, since then breaking a portal will create ghost portal blocks.
            if(shouldHidePortalBlocks && portal.isRegistered()) {
                setPortalBlocks(getPortalBlockData());
            }

            statesLock.lock();
            blockStates.resetAndUpdate();
            statesLock.unlock();
        }
    }

    public void finishUpdate(boolean refresh) {
        if(didDeactivate) {return;} // Avoid resetting block states while they're being updated asynchronously
        if(refresh) {
            logger.finer("Refreshing already sent blocks!");
        }
        statesLock.lock();

        IMultiBlockChangeManager multiBlockChangeManager = multiBlockChangeManagerFactory.create(player);
        PlaneIntersectionChecker intersectionChecker = portal.getTransformations().createIntersectionChecker(playerPosition);

        OperationTimer timer = new OperationTimer();

        IViewableBlockArray viewableBlockArray = portal.getViewableBlocks();
        for(Map.Entry<IntVector, ViewableBlockInfo> entry : viewableBlockArray.getViewableStates().entrySet()) {
            Vector position = entry.getKey().getCenterPos();

            ViewableBlockInfo block = entry.getValue();
            boolean visible = intersectionChecker.checkIfIntersects(position);

            // If visible/non-visible, change to the new state
            // However, don't bother resending the packet again if the block has already been changed
            // (unless we're refreshing the sent blocks)
            if(visible) {
                if(blockStates.setViewable(position, block) || refresh) {
                    multiBlockChangeManager.addChange(position, block.getDestData());
                }
            }   else    {
                if(blockStates.setNonViewable(position, block)) {
                    multiBlockChangeManager.addChange(position, block.getOriginData());
                }
            }
        }

        // Show the player the changed states
        multiBlockChangeManager.sendChanges();

        performanceWatcher.putTimeTaken("Player block update (threaded)", timer);
        logger.finest("Performed viewable block process. Time taken: %fms", timer.getTimeTakenMillis());

        statesLock.unlock();
    }

    // Gets the right rotation of portal block depending on the portal's direction
    private WrappedBlockData getPortalBlockData() {
        PortalDirection portalDirection = portal.getOriginPos().getDirection();
        if(portalDirection == PortalDirection.EAST || portalDirection == PortalDirection.WEST) {
            return WrappedBlockData.createData(MaterialUtil.PORTAL_MATERIAL, 2); // EAST/WEST portal blocks must be rotated
        }   else if(portalDirection == PortalDirection.NORTH || portalDirection == PortalDirection.SOUTH) {
            return WrappedBlockData.createData(MaterialUtil.PORTAL_MATERIAL, 0);
        }   else {
            throw new IllegalStateException("Tried to get portal block data of a horizontal portal");
        }
    }

    // Sets each block inside the portal window to the specified WrappedBlockData
    private void setPortalBlocks(WrappedBlockData data) {
        // Find the position at the bottom-left of the portal by subtracting half of the portal size
        Vector portalPos = portal.getOriginPos().getVector();
        Vector portalSize = portal.getSize();

        PortalDirection portalDirection = portal.getOriginPos().getDirection();
        portalPos.subtract(portalDirection.swapVector(portalSize).multiply(0.5));

        IMultiBlockChangeManager multiBlockChangeManager = multiBlockChangeManagerFactory.create(player);
        for(int x = 0; x < portalSize.getX(); x++) {
            for(int y = 0; y < portalSize.getY(); y++) {
                // Swap the coordinates if necessary to get the relative position
                Vector relativePos = portalDirection.swapVector(new Vector(x, y, 0.0));
                Vector blockPos = portalPos.clone().add(relativePos);

                multiBlockChangeManager.addChange(blockPos, data);
            }
        }
        multiBlockChangeManager.sendChanges();
    }
}
