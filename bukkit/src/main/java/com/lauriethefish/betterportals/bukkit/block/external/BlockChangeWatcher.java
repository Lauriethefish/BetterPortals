package com.lauriethefish.betterportals.bukkit.block.external;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;
import com.lauriethefish.betterportals.bukkit.util.nms.BlockDataUtil;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BlockChangeWatcher implements IBlockChangeWatcher  {
    private final IPerformanceWatcher performanceWatcher;
    private final IntVector center;
    private final Matrix rotationMatrix;
    private World world;

    private final int xAndZRadius;
    private final int yRadius;

    private final Map<IntVector, BlockData> previousData = new HashMap<>();

    @Inject
    public BlockChangeWatcher(@Assisted GetBlockDataChangesRequest request, IPerformanceWatcher performanceWatcher) {
        this.performanceWatcher = performanceWatcher;
        this.center = request.getPosition();
        this.rotationMatrix = request.getRotateOriginToDest();
        this.xAndZRadius = request.getXAndZRadius();
        this.yRadius = request.getYRadius();
        this.world = Bukkit.getWorld(request.getWorldId());
        if(world == null) {
            this.world = Bukkit.getWorld(request.getWorldName());
        }
    }

    public @NotNull Map<IntVector, Integer> checkForChanges() {
        Map<IntVector, Integer> result = new HashMap<>();

        OperationTimer timer = new OperationTimer();
        for(int x = -xAndZRadius; x <= xAndZRadius ; x++) {
            for(int z = -xAndZRadius; z <= xAndZRadius; z++) {
                for(int y = -yRadius; y <= yRadius; y++) {
                    IntVector blockPos = rotationMatrix.transform(new IntVector(x, y, z)).add(center);

                    BlockData data = blockPos.getBlock(world).getBlockData();
                    BlockData oldData = previousData.get(blockPos);

                    if(!data.equals(oldData)) {
                        result.put(blockPos, BlockDataUtil.getCombinedId(data));
                        previousData.put(blockPos, data);
                    }
                }
            }
        }

        performanceWatcher.putTimeTaken("Block change watcher update", timer);

        return result;
    }
}
