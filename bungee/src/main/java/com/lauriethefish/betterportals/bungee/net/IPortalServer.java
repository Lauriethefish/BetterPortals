package com.lauriethefish.betterportals.bungee.net;

import net.md_5.bungee.api.config.ServerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Plugin messaging doesn't work for servers that are empty, since it relies on player packets.
 * This is unfortunate, and forces us to open our own socket to send the data.
 * This could probably be done some other way, e.g. redis
 * TODO: Add support for other methods in the future.
 */
public interface IPortalServer {
    /**
     * Starts listening for connections.
     * @throws IllegalStateException If the server has already started
     */
    void startUp();

    /**
     * Stops the running server, and closes all connections.
     * If the server is already shut down, this does nothing.
     */
    void shutDown();

    /**
     * Registers the given server when the handshake has succeeded.
     * @param serverHandler The server to register
     * @param serverInfo The {@link ServerInfo} that this server's address and port correspond to.
     */
    void registerServer(@NotNull IClientHandler serverHandler, @NotNull ServerInfo serverInfo);

    /**
     * Unregisters the given client if it is registered.
     * @param handler The client to unregister
     */
    void onServerDisconnect(@NotNull IClientHandler handler);

    /**
     * @param name The name of the client server target
     * @return The client's handler, or null if there is no server with name <code>name</code>.
     */
    @Nullable IClientHandler getServer(@NotNull String name);
}
