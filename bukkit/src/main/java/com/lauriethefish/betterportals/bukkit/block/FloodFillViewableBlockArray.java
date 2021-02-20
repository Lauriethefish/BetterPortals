package com.lauriethefish.betterportals.bukkit.block;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import com.lauriethefish.betterportals.bukkit.block.fetch.BlockDataFetcherFactory;
import com.lauriethefish.betterportals.bukkit.block.fetch.IBlockDataFetcher;
import com.lauriethefish.betterportals.bukkit.block.rotation.IBlockRotator;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.math.IntVector;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * After lots of testing, a flood-fill appears to be the most efficient way to find the blocks around the destination that aren't obscured.
 * <br>If you want, you can try to optimise more, but I'm not sure how to make this much better with the requirements it has.
 */
public class FloodFillViewableBlockArray implements IViewableBlockArray    {
    private final Logger logger;
    private final RenderConfig renderConfig;
    private final IPerformanceWatcher performanceWatcher;
    private final IBlockRotator blockRotator;
    private final BlockDataFetcherFactory dataFetcherFactory;
    private IBlockDataFetcher dataFetcher;

    private ConcurrentMap<IntVector, ViewableBlockInfo> nonObscuredStates;
    @Getter private ConcurrentMap<IntVector, ViewableBlockInfo> viewableStates;

    private final IPortal portal;
    private final Matrix destToOrigin;
    private final Matrix rotateDestToOrigin;
    private final Matrix originToDest;

    private final IntVector centerPos;
    private final World originWorld;
    private final PortalDirection destDirection;
    private boolean firstUpdate;

    @Inject
    public FloodFillViewableBlockArray(@Assisted IPortal portal, Logger logger, RenderConfig renderConfig, IPerformanceWatcher performanceWatcher, IBlockRotator blockRotator, BlockDataFetcherFactory dataFetcherFactory) {
        this.portal = portal;
        this.logger = logger;
        this.renderConfig = renderConfig;
        this.performanceWatcher = performanceWatcher;
        this.blockRotator = blockRotator;
        this.centerPos = new IntVector(portal.getDestPos().getVector());
        this.destToOrigin = portal.getTransformations().getDestinationToOrigin();
        this.rotateDestToOrigin = portal.getTransformations().getRotateToOrigin();
        this.originToDest = portal.getTransformations().getOriginToDestination();
        this.originWorld = portal.getOriginPos().getWorld();
        this.destDirection = portal.getDestPos().getDirection();
        this.dataFetcherFactory = dataFetcherFactory;

        reset();
    }

    private boolean isInLine(IntVector relPos) {
        return destDirection.swapVector(relPos).getZ() == 0;
    }

    /**
     * Starts a flood fill from <code>start</code> out to the edges of the viewed portal area.
     * The fill stops when it reaches occluding blocks, as we don't need to render other blocks behind these.
     * The origin data is also fetched, and this is placed in {@link FloodFillViewableBlockArray#viewableStates}.
     * <br>Some notes:
     * - There used to be a check to see if the origin and destination states are the same, but this added too much complexity when checking for changes, so I decided to remove it.
     * - That unfortunately reduces the performance of the threaded bit slightly, but I think it's worth it for the gains here.
     * @param start Start position of the flood fill
     */
    private void searchFromBlock(IntVector start) {
        List<IntVector> stack = new ArrayList<>(renderConfig.getTotalArrayLength());
        stack.add(start);

        while(stack.size() > 0) {
            IntVector destPos = stack.remove(stack.size() - 1);
            IntVector relPos = destPos.subtract(centerPos);

            BlockData destData = dataFetcher.getData(destPos);
            boolean isOccluding = destData.getType().isOccluding();

            IntVector originPos = destToOrigin.transform(destPos);
            BlockData originData = originPos.getData(originWorld);

            ViewableBlockInfo blockInfo = new ViewableBlockInfo(originData, destData);
            boolean isEdge = renderConfig.isEdge(relPos);
            if(isEdge && !isOccluding) {
                blockInfo.setRenderedDestData(MaterialUtil.PORTAL_EDGE_DATA);
            }   else    {
                blockInfo.setRenderedDestData(blockRotator.rotateByMatrix(rotateDestToOrigin, destData).toProtocolLib());
            }
            nonObscuredStates.put(originPos, blockInfo);

            boolean canSkip = destData.equals(originData) && firstUpdate && !isEdge;
            boolean isInLine = isInLine(relPos);
            if (!isInLine && !canSkip) {
                viewableStates.put(originPos, blockInfo);
            }

            // Stop when we reach the edge or an occluding block, since we don't want to show blocks outside the view area
            if ((isOccluding && !isInLine) || isEdge) {continue;}

            // Continue for any surrounding blocks that haven't been checked yet
            for(IntVector offset : renderConfig.getSurroundingOffsets()) {
                IntVector offsetPos = originPos.add(offset);
                if (!nonObscuredStates.containsKey(offsetPos)) {
                    stack.add(originToDest.transform(offsetPos));
                }
            }
        }
    }

    /**
     * Checks the origin and destination blocks for changes.
     * At the origin, we only need to check the actually viewable blocks, since there is no need to re-flood-fill.
     * At the destination, we must check all blocks that were reached by the flood-fill, then do a re-flood-fill for any that have changed to add blocks in a newly revealed cavern, for instance.
     */
    private void checkForChanges() {
        for(Map.Entry<IntVector, ViewableBlockInfo> entry : nonObscuredStates.entrySet()) {
            ViewableBlockInfo blockInfo = entry.getValue();

            IntVector destPos = originToDest.transform(entry.getKey());
            BlockData newDestData = dataFetcher.getData(destPos);
            if(!newDestData.equals(blockInfo.getBaseDestData())) {
                logger.finer("Destination block change");
                searchFromBlock(destPos);
            }

            BlockData newOriginData = entry.getKey().getData(originWorld);

            if(!newOriginData.equals(blockInfo.getBaseOriginData())) {
                logger.finer("Origin block change");
                blockInfo.setOriginData(newOriginData);
                if(!newOriginData.equals(newDestData) && !portal.getOriginPos().isInLine(entry.getKey())) {
                    viewableStates.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public void update(int ticksSinceActivated) {
        if(ticksSinceActivated % renderConfig.getBlockUpdateInterval() != 0) {return;}

        if(dataFetcher == null) {
            dataFetcher = dataFetcherFactory.create(portal);
        }
        dataFetcher.update();

        // If fetching external blocks has not yet finished, we can't do the flood-fill.
        if(!dataFetcher.isReady()) {
            logger.fine("Not updating portal, data was not yet been fetched");
            return;
        }

        OperationTimer timer = new OperationTimer();
        if(firstUpdate) {
            searchFromBlock(centerPos);
        }   else    {
            checkForChanges();
        }
        performanceWatcher.putTimeTaken(firstUpdate ? "Initial viewable block update" : "Incremental viewable block update", timer);
        performanceWatcher.putTimeTaken("Viewable block update", timer);
        firstUpdate = false;
        logger.finer("Viewable block array update took: %.3f ms. Block count: %d. Viewable count: %d", timer.getTimeTakenMillis(), nonObscuredStates.size(), viewableStates.size());
    }

    @Override
    public void reset() {
        logger.finer("Clearing block array to save memory");
        nonObscuredStates = new ConcurrentHashMap<>();
        viewableStates = new ConcurrentHashMap<>();
        firstUpdate = true;
        dataFetcher = null;
    }
}
