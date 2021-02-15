package com.lauriethefish.betterportals.bukkit.block.external;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.math.IntVector;
import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class ExternalBlockWatcherManager implements IExternalBlockWatcherManager    {
    private final Logger logger;
    private final BlockChangeWatcherFactory blockChangeWatcherFactory;
    private final Map<UUID, IBlockChangeWatcher> watchers = new HashMap<>();

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

        Response response = new Response();
        Map<IntVector, Integer> changes = watcher.checkForChanges();
        logger.finer("Change count: %d", changes.size());

        response.setResult(changes);
        onFinish.accept(response);
    }

    @Override
    public void update() {
        // TODO
    }
}
