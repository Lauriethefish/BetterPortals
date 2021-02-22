package com.lauriethefish.betterportals.bungee.net;

import com.lauriethefish.betterportals.shared.net.Response;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents each sub-server connected to the proxy that is registered with the plugin
 */
public interface IClientHandler {
    /**
     * @return The game version of the connected server, or null if the server hasn't completed the handshake.
     */
    @Nullable String getGameVersion();

    /**
     * @return The bungeecord {@link ServerInfo} that this server handler represents, or null if the server hasn't completed the handshake
     */
    @Nullable ServerInfo getServerInfo();

    /**
     * Safely shuts down the connection to the server by sending a disconnection notice. Called on portal server shutdown.
     * Does nothing if already disconnected
     */
    void shutDown();

    /**
     * Sends <code>request</code> to this server to be processed.
     * @param request The request to send.
     * @param onFinish Called with the response when it is received.
     */
    void sendRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish);
}
