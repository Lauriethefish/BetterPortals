package com.lauriethefish.betterportals.bukkit.block.external;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.math.IntVector;
import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class ExternalBlockWatcherManager implements IExternalBlockWatcherManager    {
    /**
     * Number of seconds before clearing block watchers due to inactivity
     */
    private static final int BLOCK_WATCHER_CLEAR_DELAY = 5;

    private final Logger logger;
    private final BlockChangeWatcherFactory blockChangeWatcherFactory;
    private final Map<UUID, IBlockChangeWatcher> watchers = new HashMap<>();
    private final Map<UUID, Instant> lastRequested = new HashMap<>();

    @Inject
    public ExternalBlockWatcherManager(Logger logger, BlockChangeWatcherFactory blockChangeWatcherFactory) {
        this.logger = logger;
        this.blockChangeWatcherFactory = blockChangeWatcherFactory;
    }

    @Override
    public void onRequestReceived(GetBlockDataChangesRequest request, Consumer<Response> onFinish) {
        logger.finer("Processing block changes with ID %s", request.getChangeSetId());
        UUID watcherId = request.getChangeSetId();
        IBlockChangeWatcher watcher = watchers.computeIfAbsent(watcherId, key -> blockChangeWatcherFactory.create(request));
        lastRequested.put(request.getChangeSetId(), Instant.now());

        Response response = new Response();
        Map<IntVector, Integer> changes = watcher.checkForChanges();
        logger.finer("Change count: %d", changes.size());

        response.setResult(changes);
        onFinish.accept(response);
    }

    @Override
    public void update() {
        // Clear any watchers that are inactive
        Instant now = Instant.now();
        Iterator<Map.Entry<UUID, Instant>> iterator = lastRequested.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<UUID, Instant> entry = iterator.next();
            long secondsElapsed = Duration.between(entry.getValue(), now).getSeconds();
            if(secondsElapsed > BLOCK_WATCHER_CLEAR_DELAY) {
                logger.fine("Clearing external block watcher due to inactivity");
                iterator.remove();

                watchers.remove(entry.getKey());
            }
        }
    }
}
