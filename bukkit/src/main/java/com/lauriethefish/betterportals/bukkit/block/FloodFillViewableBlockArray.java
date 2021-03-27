package com.lauriethefish.betterportals.bukkit.block;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import com.lauriethefish.betterportals.bukkit.block.fetch.BlockDataFetcherFactory;
import com.lauriethefish.betterportals.bukkit.block.fetch.IBlockDataFetcher;
import com.lauriethefish.betterportals.bukkit.block.rotation.IBlockRotator;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.api.PortalDirection;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.bukkit.util.nms.BlockDataUtil;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private final ConcurrentMap<IntVector, PacketContainer> originTileStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<IntVector, PacketContainer> destTileStates = new ConcurrentHashMap<>();

    private final IPortal portal;
    private final Matrix destToOrigin;
    private final Matrix rotateDestToOrigin;
    private final Matrix rotateOriginToDest;
    private final IntVector portalOriginPos;
    private final IntVector portalDestPos;

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
        this.rotateOriginToDest = portal.getTransformations().getRotateToDestination();
        this.originWorld = portal.getOriginPos().getWorld();
        this.destDirection = portal.getDestPos().getDirection();
        this.dataFetcherFactory = dataFetcherFactory;
        this.portalDestPos = new IntVector(portal.getDestPos().getVector());
        this.portalOriginPos = new IntVector(portal.getOriginPos().getVector());

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
        WrappedBlockData backgroundData = renderConfig.getBackgroundBlockData();
        if(backgroundData == null) {
            backgroundData = MaterialUtil.PORTAL_EDGE_DATA; // Use the default if not overridden in the config
        }

        List<IntVector> stack = new ArrayList<>(renderConfig.getTotalArrayLength());

        stack.add(destToOrigin.transform(start).subtract(portalOriginPos));
        while(stack.size() > 0) {
            IntVector originRelPos = stack.remove(stack.size() - 1);
            IntVector originPos = originRelPos.add(portalOriginPos);
            IntVector destRelPos = rotateOriginToDest.transform(originRelPos);
            IntVector destPos = destRelPos.add(portalDestPos);

            BlockData destData = dataFetcher.getData(destPos);
            boolean isOccluding = destData.getType().isOccluding();

            Block originBlock = originPos.getBlock(originWorld);
            BlockData originData = BlockData.create(originBlock);

            if(!portal.isCrossServer() && MaterialUtil.isTileEntity(destData.getType())) {
                logger.finer("Adding tile state to map . . .");
                Block destBlock = destPos.getBlock(portal.getDestPos().getWorld());

                PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(destBlock.getState());
                if(updatePacket != null) {
                    BlockDataUtil.setTileEntityPosition(updatePacket, originPos);

                    destTileStates.put(originPos, updatePacket);
                }
            }

            if(MaterialUtil.isTileEntity(originBlock.getType()))  {
                logger.finer("Adding tile state to map . . .");
                PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(originBlock.getState());
                if(updatePacket != null) {
                    originTileStates.put(originPos, updatePacket);
                }
            }

            ViewableBlockInfo blockInfo = new ViewableBlockInfo(originData, destData);
            boolean isEdge = renderConfig.isOutsideBounds(originRelPos);
            if(isEdge && !isOccluding) {
                blockInfo.setRenderedDestData(backgroundData);
            }   else    {
                blockInfo.setRenderedDestData(blockRotator.rotateByMatrix(rotateDestToOrigin, destData).toProtocolLib());
            }
            nonObscuredStates.put(originPos, blockInfo);

            boolean canSkip = destData.equals(originData) && firstUpdate && !isEdge;
            boolean isInLine = isInLine(destRelPos);
            if (!isInLine && !canSkip) {
                viewableStates.put(originPos, blockInfo);
            }

            // Stop when we reach the edge or an occluding block, since we don't want to show blocks outside the view area
            if ((isOccluding && !isInLine) || isEdge) {continue;}

            // Continue for any surrounding blocks that haven't been checked yet
            for(IntVector offset : renderConfig.getSurroundingOffsets()) {
                IntVector offsetPos = originRelPos.add(offset);
                if (!nonObscuredStates.containsKey(offsetPos.add(portalOriginPos))) {
                    stack.add(offsetPos);
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

            IntVector destPos = rotateOriginToDest.transform(entry.getKey().subtract(portalOriginPos)).add(portalDestPos); // Avoid directly using the matrix to fix floating point precision issues
            BlockData newDestData = dataFetcher.getData(destPos);

            if(!newDestData.equals(blockInfo.getBaseDestData())) {
                logger.finer("Destination block change");
                searchFromBlock(destPos);
            }

            if(!portal.isCrossServer()) {
                if (MaterialUtil.isTileEntity(newDestData.getType())) {
                    logger.finer("Adding tile state to map . . .");
                    Block destBlock = destPos.getBlock(portal.getDestPos().getWorld());

                    PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(destBlock.getState());
                    if(updatePacket != null) {
                        BlockDataUtil.setTileEntityPosition(updatePacket, entry.getKey());

                        destTileStates.put(entry.getKey(), updatePacket);
                    }
                }
            }

            Block originBlock = entry.getKey().getBlock(originWorld);
            BlockData newOriginData = BlockData.create(originBlock);
            if(MaterialUtil.isTileEntity(originBlock.getType()))  {
                logger.finer("Adding tile state to map . . .");
                PacketContainer updatePacket = BlockDataUtil.getUpdatePacket(originBlock.getState());
                if(updatePacket != null) {
                    originTileStates.put(entry.getKey(), updatePacket);
                }
            }

            if(!newOriginData.equals(blockInfo.getBaseOriginData())) {
                logger.finer("Origin block change");
                blockInfo.setOriginData(newOriginData);
                if(!newOriginData.equals(newDestData) && !portal.getOriginPos().isInLine(entry.getKey())) {
                    viewableStates.put(entry.getKey(), entry.getValue());
                }
            }
        }

        updateTileStateMap(originTileStates, originWorld, false);
        if(!portal.isCrossServer()) {
            updateTileStateMap(destTileStates, portal.getDestPos().getWorld(), true);
        }
    }

    private void updateTileStateMap(ConcurrentMap<IntVector, PacketContainer> map, World world, boolean isDestination) {
        for(Map.Entry<IntVector, PacketContainer> entry : map.entrySet()) {
            IntVector position;
            if(isDestination) {
                IntVector portalRelativePos = entry.getKey().subtract(portalOriginPos);
                position = rotateOriginToDest.transform(portalRelativePos).add(portalDestPos);
            }   else    {
                position = entry.getKey();
            }

            Block block = position.getBlock(world);
            BlockState state = block.getState();
            if(!MaterialUtil.isTileEntity(state.getType())) {
                logger.finer("Removing tile state from map . . . %b", isDestination);
                map.remove(entry.getKey());
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
    public @Nullable PacketContainer getOriginTileEntityPacket(@NotNull IntVector position) {
        return originTileStates.get(position);
    }

    @Override
    public @Nullable PacketContainer getDestinationTileEntityPacket(@NotNull IntVector position) {
        return destTileStates.get(position);
    }

    @Override
    public void reset() {
        logger.finer("Clearing block array to save memory");
        nonObscuredStates = new ConcurrentHashMap<>();
        viewableStates = new ConcurrentHashMap<>();
        originTileStates.clear();
        destTileStates.clear();
        firstUpdate = true;
        dataFetcher = null;
    }
}
