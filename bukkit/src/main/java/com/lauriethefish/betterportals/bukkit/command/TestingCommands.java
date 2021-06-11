package com.lauriethefish.betterportals.bukkit.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.*;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.entity.faking.EntityInfo;
import com.lauriethefish.betterportals.bukkit.entity.faking.IEntityPacketManipulator;
import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.net.requests.TestForwardedRequest;
import com.lauriethefish.betterportals.api.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.spawning.NewPortalChecker;
import com.lauriethefish.betterportals.bukkit.util.MaterialUtil;
import com.lauriethefish.betterportals.bukkit.util.nms.BlockDataUtil;
import com.lauriethefish.betterportals.bukkit.util.performance.IPerformanceWatcher;
import com.lauriethefish.betterportals.bukkit.util.performance.Operation;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.net.RequestException;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class TestingCommands {
    private final IPerformanceWatcher performanceWatcher;
    private final NewPortalChecker spawnChecker;
    private final IEntityPacketManipulator entityPacketManipulator;
    private final IPortalClient portalClient;
    private List<Integer> storedData;

    @Inject
    public TestingCommands(IPerformanceWatcher performanceWatcher, NewPortalChecker spawnChecker, IEntityPacketManipulator entityPacketManipulator, IPortalClient portalClient) {
        this.performanceWatcher = performanceWatcher;
        this.spawnChecker = spawnChecker;
        this.entityPacketManipulator = entityPacketManipulator;
        this.portalClient = portalClient;
    }

    @Command
    @Path("betterportals/test/performance")
    @Aliases("speed")
    public boolean printPerformanceInfo(CommandSender sender) {
        sender.sendMessage("Performance Metrics");
        sender.sendMessage("-------------------------------------------------");
        for(Map.Entry<String, Operation> entry : performanceWatcher.getTimedOperations().entrySet()) {
            Operation operation = entry.getValue();

            sender.sendMessage(String.format("%s: Average time: %.03fms, Highest time: %.03fms, Lowest time: %.03fms",
                    entry.getKey(),
                    operation.getAverageTime().getNano() / 1_000_000d,
                    operation.getHighestTime().getNano() / 1_000_000d,
                    operation.getLowestTime().getNano() / 1_000_000d
            ));
        }
        return true;
    }

    @Command
    @Path("betterportals/test/portalBlock")
    @Argument(name = "dataValue")
    @RequiresPlayer
    public boolean createTestPortalBlock(Player sender, byte dataValue) {
        BlockState state = sender.getLocation().getBlock().getState();

        state.setType(MaterialUtil.PORTAL_MATERIAL);
        state.setRawData(dataValue);

        state.update(true, false);
        return true;
    }

    @Command
    @Path("betterportals/test/isValid")
    @Argument(name = "direction")
    @Argument(name = "sizeX")
    @Argument(name = "sizeY")
    @RequiresPlayer
    public boolean testIsValidSpawnPos(Player player, PortalDirection direction, int sizeX, int sizeY) {
        Vector size = new Vector(sizeX, sizeY, 0.0);
        player.sendMessage(String.valueOf(spawnChecker.isValidPortalSpawnPosition(player.getLocation().subtract(0.0, 1.0, 0.0), direction, size)));

        return true;
    }

    public EntityInfo getEntityInfo(Player player) {
        Entity nearestEntity = null;
        for(Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if(entity instanceof Player) {continue;}
            nearestEntity = entity;
            break;
        }

        assert nearestEntity != null;
        return new EntityInfo(nearestEntity);
    }

    @Command
    @Path("betterportals/test/hideEntity")
    @RequiresPlayer
    public boolean hideEntity(Player player) {
        entityPacketManipulator.hideEntity(getEntityInfo(player), player);
        return true;
    }

    @Command
    @Path("betterportals/test/showEntity")
    @RequiresPlayer
    public boolean showEntity(Player player) {
        entityPacketManipulator.showEntity(getEntityInfo(player), player);
        return true;
    }

    @Command
    @Path("betterportals/test/smallTeleport")
    @RequiresPlayer
    public boolean testTeleport(Player player) {
        player.teleport(player.getLocation().add(0.1, 0.0, 0.0));
        return true;
    }

    @Command
    @Path("betterportals/test/serializeBlocks")
    @RequiresPlayer
    public boolean doTestSerialization(Player player) {
        List<Integer> result = new ArrayList<>();

        OperationTimer timer = new OperationTimer();
        for(int x = -10; x < 10; x++) {
            for(int y = -10; y < 10; y++) {
                for(int z = -10; z < 10; z++) {
                    BlockData blockData = player.getLocation().add(x, y, z).getBlock().getBlockData();
                    result.add(BlockDataUtil.getCombinedId(blockData));
                }
            }
        }
        this.storedData = result;
        player.sendMessage(String.format("Serialized block data of %d blocks. Time taken: %.03f", result.size(), timer.getTimeTakenMillis()));

        return true;
    }

    @Command
    @Path("betterportals/test/restoreSerializedBlocks")
    @RequiresPlayer
    public boolean doTestDeserialization(Player player) {
        OperationTimer timer = new OperationTimer();
        int i = 0;
        for(int x = -10; x < 10; x++) {
            for(int y = -10; y < 10; y++) {
                for(int z = -10; z < 10; z++) {
                    int storedCombinedId = storedData.get(i);
                    BlockData blockData = BlockDataUtil.getByCombinedId(storedCombinedId);

                    player.getLocation().add(x, y, z).getBlock().setBlockData(blockData);

                    i++;
                }
            }
        }

        player.sendMessage(String.format("Successfully restored %d blocks. Time taken: %.03f", storedData.size(), timer.getTimeTakenMillis()));
        return true;
    }


    @Command
    @Path("betterportals/test/forwardRequest")
    @Argument(name = "serverName")
    public boolean sendForwardedRequest(CommandSender sender, String serverName) {
        TestForwardedRequest request = new TestForwardedRequest();
        request.setTestField(new IntVector(5, 10, 5));

        portalClient.sendRequestToServer(request, serverName, (response -> {
            try {
                IntVector result = (IntVector) response.getResult();
                sender.sendMessage(result.toString());
            }   catch(RequestException ex) {
                ex.printStackTrace();
            }
        }));

        return true;
    }

    @Command
    @Path("betterportals/test/refresh")
    @RequiresPlayer
    public boolean refreshChunk(Player sender) {
        Chunk senderChunk = sender.getLocation().getChunk();
        sender.getWorld().refreshChunk(senderChunk.getX(), senderChunk.getZ());
        return true;
    }
}
