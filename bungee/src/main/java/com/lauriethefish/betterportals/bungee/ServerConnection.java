package com.lauriethefish.betterportals.bungee;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import javax.crypto.AEADBadTagException;

import com.lauriethefish.betterportals.network.RegisterRequest;
import com.lauriethefish.betterportals.network.Request;
import com.lauriethefish.betterportals.network.Response;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer;
import com.lauriethefish.betterportals.network.RequestStream;
import com.lauriethefish.betterportals.network.TeleportPlayerRequest;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer.ServerNotFoundException;
import com.lauriethefish.betterportals.network.encryption.EncryptionManager;
import com.lauriethefish.betterportals.network.RegisterRequest.UnknownRegisterServerException;
import com.lauriethefish.betterportals.network.RegisterRequest.PluginVersionMismatchException;
import com.lauriethefish.betterportals.network.Response.RequestException;

import net.md_5.bungee.api.config.ServerInfo;

public class ServerConnection {
    private BetterPortals pl;
    private Socket socket;

    private ServerInfo server = null;

    private RequestStream objectStream;
    private volatile boolean isConnected = true;

    private EncryptionManager encryptionManager;

    // Starts a new thread to handle connections to this server
    public ServerConnection(BetterPortals pl, Socket socket, EncryptionManager encryptionManager) {
        this.pl = pl;
        this.socket = socket;
        this.encryptionManager = encryptionManager;
        new Thread(() -> {
            // Print out any exceptions that occur
            try {
                handleClient();
            }   catch(Throwable ex) {
                handleException(ex);
            }
        }).start();
    }

    private void handleClient() throws IOException, ClassNotFoundException, GeneralSecurityException {
        pl.getLogger().info(String.format("Client connected with address %s.", socket.getInetAddress()));

        // Create the synchronized request stream with our encryption keys
        objectStream = new RequestStream(socket.getInputStream(), socket.getOutputStream(), encryptionManager);

        // Keep checking for requests while the socket is still open
        while (isConnected) {
            // Read the next request from the server
            Request request = (Request) objectStream.readNextOfType(Request.class);
            pl.logDebug("Received request from client %s of type %s", socket.getInetAddress(), request.getClass().getName());
            
            objectStream.writeObject(handleRequest(request)); // Send the request to the handler, and write the Response back to the requester
        }

        socket.close();
    }

    private Response handleRequest(Request request) {
        Object result = null;
        try {
            // Send this request to the correct handler function
            if (request instanceof RegisterRequest) {
                handleRegisterRequest((RegisterRequest) request);
            } else if (request instanceof ServerBoundRequestContainer) {
                result = handleServerBoundRequestContainer((ServerBoundRequestContainer) request);
            } else if (request instanceof TeleportPlayerRequest) {
                handleTeleportPlayerRequest((TeleportPlayerRequest) request);
            }

        } catch (RequestException ex) {
            // If an error was caught, send it to the handler on the server
            return Response.error(ex);
        }   catch (Throwable ex) {
            // If any other error was caught, box it in RequestException
            return Response.error(new RequestException(ex));
        }

        // Throw an error if the server wasn't found in the first request
        if (server == null) {
            throw new RuntimeException("Client did not provide a valid RegisterRequest as its first request!");
        }

        return Response.success(result);
    }

    // Finds the correct ServerInfo that matches this RegisterRequest
    @SuppressWarnings("deprecation")
    private void handleRegisterRequest(RegisterRequest registerRequest) throws RequestException {
        // Check that the plugin versions are the same for safety
        if(!registerRequest.getPluginVersion().equals(pl.getDescription().getVersion())) {
            throw new PluginVersionMismatchException(registerRequest.getPluginVersion(), pl.getDescription().getVersion());
        }

        InetSocketAddress claimedAddress = new InetSocketAddress(socket.getInetAddress(),
                registerRequest.getServerPort());

        // Loop through each server, and find one that matches the claimed port and IP
        for (ServerInfo server : pl.getProxy().getServers().values()) {
            // Find the address of this server, and sees if the IP and port match the
            // claimed ones
            InetSocketAddress serverAddress = server.getAddress();
            pl.logDebug("Checking server " + server.toString());
            if (serverAddress.equals(claimedAddress)) {
                this.server = server;
                pl.getPortalServer().registerConnection(this, server);
                pl.logDebug("Client with address " + socket.getInetAddress() + " registered");
                return;
            }
        }
        pl.getLogger().warning("Invalid RegisterRequest received from " + claimedAddress.toString());

        // Throw an exception if the server wasn't found
        throw new UnknownRegisterServerException(claimedAddress);
    }

    // Sends a request to the bukkit server, and returns the result, or throws RequestException if the result was an error
    public Object sendRequest(Request request) throws RequestException  {
        pl.logDebug("Sending request of type %s", request.getClass().getName());
        try {
            objectStream.writeObject(request);

            pl.logDebug("Reading response . . .");
            return ((Response) objectStream.readNextOfType(Response.class)).getResult();
        }   catch(IOException | ClassNotFoundException | GeneralSecurityException ex) {
            // Send any IO/other reading exceptions to handleException, since these are fatal.
            // Then box the error as a RequestException
            handleException(ex);
            throw new RequestException("Error while sending request", ex);
        }
    }

    private Object handleServerBoundRequestContainer(ServerBoundRequestContainer request) throws RequestException {
        // Find the server this request should be forwarded to
        ServerConnection dServerConnection = pl.getPortalServer().getConnection(request.getDestinationServer());
        if(dServerConnection == null) {
            throw new ServerNotFoundException(request.getDestinationServer());
        }
        
        // Send the bytes of the request, and get the response
        return dServerConnection.sendRequest(request);
    }

    private void handleTeleportPlayerRequest(TeleportPlayerRequest request) throws RequestException {
        pl.logDebug("Teleporting player %s to server %s", request.getPlayerId(), request.getDestServer());
        // Find the destination server
        ServerConnection dServerConnection = pl.getPortalServer().getConnection(request.getDestServer());
        if(dServerConnection == null) {
            pl.logDebug("Server was not found!");
            throw new ServerNotFoundException(request.getDestServer());
        }
        
        pl.logDebug("Sending teleport request");
        // Send the teleport request to the destination server so that the player gets teleported when they join
        dServerConnection.sendRequest(request);
        
        pl.logDebug("Teleporting to remote server");
        // Move the player to the destination server
        pl.getProxy().getPlayer(request.getPlayerId()).connect(pl.getProxy().getServerInfo(request.getDestServer()));
    }

    // Should be called whenever there is any kind of exception while reading from/writing to the socket.
    // This shuts down the connection, so that later attempts to read or write will fail.
    // Methods that can throw exceptions other than RequestException should direct them here and then package them as RequestException to be sent back to the caller
    private void handleException(Throwable ex) {
        if(!isConnected) {return;}

        shutdown(); // Shut down the connection

        if(ex instanceof AEADBadTagException) { // This exception is thrown if the encryption key is wrong
            pl.getLogger().severe(String.format("Disconnected from server %s due to a tag mismatch (is your encryption key correct on both sides?)", socket.getInetAddress()));
        }   else if(!(ex instanceof EOFException))  { // An EOFException is thrown whenever the other side closes the connection. This shouldn't be printed.
            pl.getLogger().severe(String.format("An error occured while connected to the server %s", socket.getInetAddress()));
            ex.printStackTrace();
        }

        pl.getLogger().info("Server " + socket.getInetAddress() + " disconnected");
    }
    
    // Closes the connection to the client
    public void shutdown() {
        if(!isConnected) {return;} // Return if the socket is already disconnected

        isConnected = false;
        pl.getPortalServer().onServerDisconnect(server); // Unregister us
        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
