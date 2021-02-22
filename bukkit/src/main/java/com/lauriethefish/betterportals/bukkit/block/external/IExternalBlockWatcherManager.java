package com.lauriethefish.betterportals.bukkit.block.external;

import com.lauriethefish.betterportals.bukkit.net.requests.GetBlockDataChangesRequest;
import com.lauriethefish.betterportals.shared.net.Response;

import java.util.function.Consumer;

public interface IExternalBlockWatcherManager {
    /**
     * The time of no requests for a particular data array before it gets cleared.
     */
    double CLEAR_TIME = 10;

    /**
     * Called whenever a request to fetch the block data changes is received (on the main thread) from an external server.
     * @param request The change request
     * @param onFinish Given the response when responding is complete
     */
    void onRequestReceived(GetBlockDataChangesRequest request, Consumer<Response> onFinish);

    /**
     * Removes any external change watchers that are unused.
     */
    void update();
}
