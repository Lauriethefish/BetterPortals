package com.lauriethefish.betterportals.bungee.net;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.*;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStreamFactory;
import com.lauriethefish.betterportals.shared.net.encryption.IEncryptedObjectStream;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.AEADBadTagException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ClientHandler implements IClientHandler {
    private final IPortalServer portalServer;
    private final Logger logger;
    private final Plugin pl;
    private final EncryptedObjectStreamFactory encryptedObjectStreamFactory;
    private final IRequestHandler requestHandler;

    private final Socket socket;
    private IEncryptedObjectStream objectStream;

    @Getter private ServerInfo serverInfo = null;
    @Getter private String gameVersion;

    private volatile boolean isRunning = true;

    private final AtomicInteger currentRequestId = new AtomicInteger();
    private final ConcurrentMap<Integer, Consumer<Response>> waitingRequests = new ConcurrentHashMap<>();

    @Inject
    public ClientHandler(@Assisted Socket socket, IPortalServer portalServer, Logger logger, Plugin pl, EncryptedObjectStreamFactory encryptedObjectStreamFactory, IRequestHandler requestHandler) {
        this.socket = socket;
        this.portalServer = portalServer;
        this.logger = logger;
        this.pl = pl;
        this.encryptedObjectStreamFactory = encryptedObjectStreamFactory;
        this.requestHandler = requestHandler;

        new Thread(() -> {
            try {
                run();
            }   catch(IOException ex) {
                if (!isRunning) {
                    return;
                } // An IOException gets thrown if another thread shuts down this connection

                logger.warning("An IO error occurred while connected to %s", socket.getRemoteSocketAddress());
                ex.printStackTrace();
            }   catch(AEADBadTagException ex) {
                logger.warning("Failed to initialise encryption with %s", socket.getRemoteSocketAddress());
                logger.warning("Please make sure that your encryption key is valid!");
                ex.printStackTrace();
            }   catch(Exception ex) {
                logger.warning("An error occurred while connected to %s", socket.getRemoteSocketAddress());
                ex.printStackTrace();
            }   finally     {
                disconnect();
            }
        }).start();
    }

    /**
     * Reads a {@link Handshake} to get info about the server that is connecting, then sends a {@link HandshakeResponse} to tell the connecting server if the connection was successful.
     * @return If the handshake was successful
     */
    private boolean performHandshake() throws IOException, ClassNotFoundException, GeneralSecurityException {
        logger.fine("Reading handshake . . .");
        Handshake handshake = (Handshake) objectStream.readObject();
        logger.fine("Handshake plugin version: %s. Handshake game version: %s", handshake.getPluginVersion(), handshake.getGameVersion());

        // The plugin version needs to be the same, since the protocol may have changed
        HandshakeResponse.Result result = HandshakeResponse.Result.SUCCESS;
        if(!pl.getDescription().getVersion().equals(handshake.getPluginVersion())) {
            logger.warning("A server tried to register with a different plugin version (%s)", handshake.getPluginVersion());
            result = HandshakeResponse.Result.PLUGIN_VERSION_MISMATCH;
        }

        InetSocketAddress statedServerAddress = new InetSocketAddress(socket.getInetAddress(), handshake.getServerPort());

        // Find the bungeecord server that the connector is
        ServerInfo serverInfo = findServer(statedServerAddress);
        if(serverInfo == null) {
            logger.warning("A server tried to register that didn't exist in bungeecord");
            result = HandshakeResponse.Result.SERVER_NOT_REGISTERED;
        }

        HandshakeResponse response = new HandshakeResponse();
        response.setStatus(result);
        send(response);

        if(result == HandshakeResponse.Result.SUCCESS) {
            logger.fine("Successfully registered with server %s", serverInfo);
            logger.fine("Plugin version: %s. Game version: %s.", handshake.getPluginVersion(), handshake.getGameVersion());
            portalServer.registerServer(this, serverInfo);
            this.serverInfo = serverInfo;
            this.gameVersion = handshake.getGameVersion();
            return true;
        }   else    {
            return false;
        }
    }

    /**
     * Finds the bungeecord server with the address <code>clientAddress</code>.
     * @param clientAddress The address to search for
     * @return The server with this address, or null if there is none
     */
    @SuppressWarnings("deprecation")
    private @Nullable ServerInfo findServer(InetSocketAddress clientAddress) {
        for(ServerInfo server : pl.getProxy().getServers().values()) {
            InetSocketAddress serverAddress = server.getAddress();
            if(serverAddress.equals(clientAddress)) {
                return server;
            }
        }

        return null;
    }

    private void run() throws IOException, ClassNotFoundException, GeneralSecurityException    {
        objectStream = encryptedObjectStreamFactory.create(socket.getInputStream(), socket.getOutputStream());

        if(!performHandshake()) {
            return;
        }

        while(true) {
            Object next = objectStream.readObject();
            if (next instanceof DisconnectNotice) {
                logger.fine("Received disconnection notice, shutting down!");
                return;
            } else if (next instanceof Response) {
                processResponse((Response) next);
            } else if (next instanceof Request) {
                processRequest((Request) next);
            }
        }
    }

    /**
     * Sends <code>request</code> to the request handler and then sends the response with the correct ID.
     * @param request The request to process
     */
    private void processRequest(Request request) {
        // We don't just send the response directly, since it may take some time to process the request, and we need to be ready for more requests.
        int requestId = request.getId();
        requestHandler.handleRequest(request, (response) -> {
            response.setId(requestId); // Assign the correct request ID so that the client knows which request this response is for
            try {
                send(response);
            } catch (IOException | GeneralSecurityException ex) {
                logger.warning("IO Error occurred while sending a response to a request");
                ex.printStackTrace();
                disconnect();
            }
        });
    }

    /**
     * Sends <code>response</code> to the correct request in queue.
     * @param response The response to consume
     */
    private void processResponse(Response response) {
        Consumer<Response> waiter = waitingRequests.remove(response.getId());
        if(waiter == null) {
            throw new IllegalStateException("Received response for request that didn't exist");
        }

        waiter.accept(response);
    }

    @Override
    public void shutDown() {
        if(!isRunning) {return;}

        try {
            send(new DisconnectNotice());
        }   catch(IOException | GeneralSecurityException ex)   {
            logger.warning("Error occurred while sending disconnection notice to %s", socket.getRemoteSocketAddress());
        }
        disconnect();
    }

    /**
     * Closes the socket and unregisters this handler in this {@link IPortalServer}
     * Any waiting requests will receive a response with an error.
     */
    private void disconnect() {
        if(!isRunning) {return;}
        isRunning = false;

        portalServer.onServerDisconnect(this);
        try {
            socket.close();
        }   catch (IOException ex) {
            logger.warning("Error occurred while disconnecting from %s", socket.getRemoteSocketAddress());
            ex.printStackTrace();
        }

        // Send an error to all waiting requests
        Response disconnectResponse = new Response();
        disconnectResponse.setError(new RequestException("Client server connection disconnected while sending the request"));
        for(Consumer<Response> responseConsumer : waitingRequests.values()) {
            responseConsumer.accept(disconnectResponse);
        }
    }

    private synchronized void send(Object obj) throws IOException, GeneralSecurityException {
        objectStream.writeObject(obj);
    }

    private void verifyCanSendRequests() {
        if(serverInfo == null) {
            throw new IllegalStateException("Attempted to send request before handshake was finished");
        }
    }

    @Override
    public void sendRequest(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
        verifyCanSendRequests();

        int requestId = currentRequestId.getAndIncrement();
        request.setId(requestId);
        waitingRequests.put(requestId, onFinish);

        try {
            send(request);
        }   catch(IOException | GeneralSecurityException ex)     {
            logger.warning("Client server connection disconnected while sending the request");

            disconnect();
        }
    }
}
