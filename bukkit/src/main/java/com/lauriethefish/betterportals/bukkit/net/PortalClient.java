package com.lauriethefish.betterportals.bukkit.net;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.*;
import com.lauriethefish.betterportals.shared.net.encryption.CipherManager;
import com.lauriethefish.betterportals.shared.net.HandshakeResponse;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStreamFactory;
import com.lauriethefish.betterportals.shared.net.encryption.IEncryptedObjectStream;
import com.lauriethefish.betterportals.shared.net.requests.RelayRequest;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import javax.crypto.AEADBadTagException;

import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Singleton
public class PortalClient implements IPortalClient {
    private final JavaPlugin pl;
    private final ProxyConfig proxyConfig;
    private final Logger logger;
    private final EncryptedObjectStreamFactory encryptedObjectStreamFactory;
    private final IRequestHandler requestHandler;
    private final IClientReconnectHandler reconnectHandler;

    private Socket socket;
    private volatile boolean isRunning = false;
    private volatile boolean hasHandshakeFinished = false;

    private volatile boolean shouldReconnectIfFailed;

    private IEncryptedObjectStream objectStream;

    private final AtomicInteger currentRequestId = new AtomicInteger();
    private final ConcurrentMap<Integer, Consumer<Response>> waitingRequests = new ConcurrentHashMap<>();

    @Inject
    public PortalClient(JavaPlugin pl, ProxyConfig proxyConfig, Logger logger, CipherManager cipherManager, EncryptedObjectStreamFactory encryptedObjectStreamFactory, IRequestHandler requestHandler, IClientReconnectHandler reconnectHandler) {
        this.pl = pl;
        this.proxyConfig = proxyConfig;
        this.logger = logger;
        this.encryptedObjectStreamFactory = encryptedObjectStreamFactory;
        this.requestHandler = requestHandler;
        this.reconnectHandler = reconnectHandler;

        if(proxyConfig.isEnabled()) {
            try {
                cipherManager.init(proxyConfig.getEncryptionKey());
            } catch (NoSuchAlgorithmException ex) {
                logger.severe("Unable to find algorithm to encrypt proxy connection");
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void connect(boolean printErrors) {
        if(isRunning) {throw new IllegalStateException("Attempted to start connection when was was already established");}
        isRunning = true;
        shouldReconnectIfFailed = true;

        new Thread(() -> {
            try {
                run();
            }   catch(IOException ex) {
                // An IOException gets thrown if another thread shuts down this connection
                if (!isRunning) {
                    return;
                }
                if(printErrors) {
                    logger.warning("An IO error occurred while connected to the proxy");
                    logger.warning("%s: %s", ex.getClass().getName(), ex.getMessage()); // Don't print the full stack trace - it's pretty long
                }
            }   catch(AEADBadTagException ex) {
                shouldReconnectIfFailed = false;
                if(printErrors) {
                    logger.warning("Failed to initialise encryption with the proxy");
                    logger.warning("Please make sure that your encryption key is valid!");
                    ex.printStackTrace();
                }
            }   catch(Exception ex) {
                if(printErrors) {
                    logger.warning("An error occurred while connected to the proxy");
                    ex.printStackTrace();
                }
            }   finally     {
                disconnect();
            }
        }).start();
    }

    private void run() throws IOException, GeneralSecurityException, ClassNotFoundException {
        socket = new Socket();
        socket.connect(proxyConfig.getAddress());


        logger.fine("Hello from client thread");
        objectStream = encryptedObjectStreamFactory.create(socket.getInputStream(), socket.getOutputStream());

        if(!runHandshake()) {
            shouldReconnectIfFailed = false; // The handshake will just fail again if reconnecting, so don't
            return;
        }

        logger.info("Successfully connected to the proxy");

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
     * Sends <code>request</code> to the request handler on the main thread.
     * @param request The request to process
     */
    private void processRequest(Request request) {
        // We don't just send the response directly, since it may take some time to process the request, and we need to be ready for more requests.
        requestHandler.handleRequest(request, (response) -> Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            response.setId((request).getId()); // Assign the correct request ID so that the proxy knows which request this response is for
            try {
                send(response);
            } catch (IOException | GeneralSecurityException ex) {
                logger.warning("IO Error occurred while sending a response to a request");
                ex.printStackTrace();
                disconnect();
            }
        }));
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

        // Call it on the main server thread
        Bukkit.getScheduler().runTask(pl, () -> waiter.accept(response));
    }

    /**
     * Sends a {@link Handshake} to the proxy to verify the plugin version, and to tell the proxy our game version.
     * @return Whether or not the handshake was successful.
     */
    private boolean runHandshake() throws IOException, GeneralSecurityException, ClassNotFoundException {
        logger.fine("Running handshake . . .");
        Handshake handshake = new Handshake();
        handshake.setPluginVersion(pl.getDescription().getVersion());
        handshake.setServerPort(Bukkit.getPort());
        handshake.setGameVersion(VersionUtil.getCurrentVersion());
        objectStream.writeObject(handshake);

        HandshakeResponse response = (HandshakeResponse) objectStream.readObject();

        switch(response.getStatus()) {
            case SUCCESS:
                logger.fine("Handshake was successful");
                hasHandshakeFinished = true;
                return true;
            case PLUGIN_VERSION_MISMATCH:
                logger.severe("Bukkit plugin & proxy plugin versions are different. Please update both to the latest version");
                return false;
            case SERVER_NOT_REGISTERED:
                logger.severe("Proxy reported that this server wasn't registered on their end. This happens if the server you're connecting from isn't registered with bungeecord");
                return false;
        }
        throw new IllegalStateException("This should never happen");
    }

    @Override
    public void shutDown() {
        if(!isRunning) {return;}
        isRunning = false;
        hasHandshakeFinished = false;
        shouldReconnectIfFailed = false;

        try {
            if(objectStream != null) {
                send(new DisconnectNotice());
            }
        }   catch(IOException | GeneralSecurityException ex) {
            logger.warning("Error occurred while sending disconnection notice to proxy");
            ex.printStackTrace();
        }

        disconnect(true); // Force it since we've set isRunning to false
    }

    @Override
    public boolean canReceiveRequests() {
        return hasHandshakeFinished;
    }

    @Override
    public boolean isConnectionOpen() {
        return isRunning;
    }

    @Override
    public boolean getShouldReconnect() {
        return shouldReconnectIfFailed;
    }

    private void disconnect() {
        disconnect(false);
    }

    /**
     * Closest the socket, and sets {@link PortalClient#isRunning} to false.
     * Does nothing if already disconnected.
     * All waiting requests will receive an error.
     */
    private void disconnect(boolean force) {
        if(!isRunning && !force) {return;}
        if(hasHandshakeFinished || force) {
            logger.info("Disconnecting from the proxy");
        }

        isRunning = false;
        hasHandshakeFinished = false;

        try {
            if(socket != null) {
                socket.close();
            }
        }   catch(IOException ex) {
            logger.warning("Error occurred while closing proxy connection socket");
            ex.printStackTrace();
        }

        Response disconnectResponse = new Response();
        disconnectResponse.setError(new RequestException("Disconnected from proxy while sending the request"));
        for(Consumer<Response> responseConsumer : waitingRequests.values()) {
            responseConsumer.accept(disconnectResponse);
        }

        reconnectHandler.onClientDisconnect();
    }

    @Override
    public void sendRequestToProxy(@NotNull Request request, @NotNull Consumer<Response> onFinish) {
        int requestId = currentRequestId.getAndIncrement();
        // Set a new request ID to preserve order in case the request doesn't come back straight away
        request.setId(requestId);
        waitingRequests.put(requestId, onFinish);

        if(!hasHandshakeFinished) {
            Response notConnected = new Response();
            notConnected.setError(new RequestException("Not connected to the proxy"));
            onFinish.accept(notConnected);
            return;
        }

        // Avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously(pl, () -> {
            try {
                send(request);
            } catch (IOException | GeneralSecurityException ex) {
                logger.warning("Disconnected from proxy while sending request");
                disconnect();
            }
        });
    }

    @Override
    public void sendRequestToServer(@NotNull Request request, @NotNull String destinationServer, @NotNull Consumer<Response> onFinish) {
        // Tell the proxy to send the request to the correct server
        RelayRequest relayRequest = new RelayRequest();
        relayRequest.setInnerRequest(request);
        relayRequest.setDestination(destinationServer);

        // Relayed responses are wrapped so that they can be treated as opaque on the proxy side
        sendRequestToProxy(relayRequest, (response) -> {
            try {
                byte[] responseData = (byte[]) response.getResult();
                Object deserializedResponse = new ObjectInputStream(new ByteArrayInputStream(responseData)).readObject();
                onFinish.accept((Response) deserializedResponse);

            }   catch(RequestException ex) { // This is thrown if there is an error while forwarding the request, not in its processing at the destination
                Response eResponse = new Response();
                eResponse.setError(ex);
                onFinish.accept(eResponse);
            }   catch(IOException | ClassNotFoundException ex) {
                disconnect();
            }
        });
    }

    public synchronized void send(Object obj) throws GeneralSecurityException, IOException {
        objectStream.writeObject(obj);
    }
}
