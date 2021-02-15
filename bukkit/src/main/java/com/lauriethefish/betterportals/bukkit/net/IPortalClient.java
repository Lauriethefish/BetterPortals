package com.lauriethefish.betterportals.bukkit.net;

import com.lauriethefish.betterportals.shared.net.Response;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface IPortalClient {

    /**
     * Starts a connection to the proxy stated in {@link ProxyConfig}
     * @throws IllegalStateException if a connection is already open
     */
    void connect();

    /**
     * Disconnects from the proxy safely, sending a disconnection notice.
     * Does nothing if already disconnected
     */
    void shutDown();

    /**
     * @return if the handshake has completed and sending requests is safe.
     */
    boolean canReceiveRequests();

    /**
     * @return Whether a connection is currently open to the proxy. The handshake hasn't necessarily finished, even if this is true
     */
    boolean isConnectionOpen();

    /**
     * Sends <code>request</code> and calls <code>onReceive</code> once a response is read.
     * This will not block.
     * @param request The request to send
     * @param onReceive Called with the response on the main thread once the client thread receives it.
     * @throws IllegalStateException If the client is not connected to the proxy
     */
    void sendRequestToProxy(@NotNull Request request, @NotNull Consumer<Response> onReceive);


    /**
     * Forwards <code>request</code> to one of the servers connected to the proxy, and calls <code>onReceive</code> once a response is read.
     * @param request The request to forward
     * @param destinationServer The server to forward to
     * @param onReceive Called with the response on the main thread once the client thread receives it.
     * @throws IllegalStateException If the client is not connected to the proxy
     */
    void sendRequestToServer(Request request, String destinationServer, Consumer<Response> onReceive);
}
