package com.lauriethefish.betterportals.bungee;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.lauriethefish.betterportals.network.encryption.EncryptionManager;

import net.md_5.bungee.api.config.ServerInfo;

public class PortalServer {
    private BetterPortals pl;
    private boolean isActive = true;

    private ServerSocket socket;
    private Map<ServerInfo, ServerConnection> connectedServers = new ConcurrentHashMap<>();

    private EncryptionManager encryptionManager;

    // Creates a new portal server, and starts the server thread
    public PortalServer(BetterPortals pl) {
        this.pl = pl;

        // Create the encryption manager using the UUID key from the config file
        UUID encryptionKey = UUID.fromString(pl.getConfig().getString("key"));
        try {
            encryptionManager = new EncryptionManager(encryptionKey);
        }   catch(NoSuchAlgorithmException ex) {
            // If an error occurs, print it and return
            pl.getLogger().severe("Error occured while starting server: failed to initialise encryption");
            ex.printStackTrace();
            return;
        }

        // Start a new thread to run the server on
        new Thread(() -> {
            try {
                startServer();
            } catch (IOException ex) {
                // There will always be a SocketException thrown when shutdown() closes the socket to end the server, so don't print this error if the server was closed
                if(isActive) {ex.printStackTrace();}
            }
        }).start();
    }

    // Functions for managing the connectedServers map
    public void registerConnection(ServerConnection connection, ServerInfo server) {
        connectedServers.put(server, connection);
    }

    public void unregisterConnection(ServerInfo server) {
        connectedServers.remove(server);
    }

    public ServerConnection getConnection(ServerInfo server) {
        return connectedServers.get(server);
    }

    public ServerConnection getConnection(String serverName) {
        ServerInfo serverInfo = pl.getProxy().getServerInfo(serverName);
        // Return null instead of throwing a NullPointerException
        if(serverInfo == null) {return null;}

        return getConnection(serverInfo);
    }

    public void startServer() throws IOException {
        // Bind the socket to the address specified in the config
        pl.getLogger().info("Starting portal server . . .");
        socket = new ServerSocket();
        socket.bind(new InetSocketAddress(pl.getConfig().getString("bindAddress"), pl.getConfig().getInt("serverPort")));

        // Loop forever accepting new clients
        while(isActive) {
            Socket client = socket.accept();
            // Start a new ServerConnection to handle this client
            new ServerConnection(pl, client, encryptionManager);
        }
    }

    // Closes all running parts of the server
    public void shutdown() {
        pl.getLogger().info("Closing server . . .");

        // Stop listening for new requests
        isActive = false;
        try {
            if(socket != null)  {
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Disconnect all connected servers
        for(ServerConnection server : connectedServers.values()) {
            server.shutdown();
        }
    }
}
