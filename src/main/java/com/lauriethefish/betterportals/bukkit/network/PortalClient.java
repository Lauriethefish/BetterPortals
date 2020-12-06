package com.lauriethefish.betterportals.bukkit.network;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.network.RegisterRequest;
import com.lauriethefish.betterportals.network.Request;
import com.lauriethefish.betterportals.network.Response;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer;
import com.lauriethefish.betterportals.network.RequestStream;
import com.lauriethefish.betterportals.network.TeleportPlayerRequest;
import com.lauriethefish.betterportals.network.Response.RequestException;
import com.lauriethefish.betterportals.network.encryption.EncryptionManager;

import org.bukkit.Location;

import lombok.Getter;

// Handles connecting to the bungeecord/velocity server to allow cross-server portals
public class PortalClient {
    private BetterPortals pl;

    private Socket socket;
    @Getter private volatile boolean isConnected = true;

    private RequestStream objectStream;

    public PortalClient(BetterPortals pl) {
        this.pl = pl;

        new Thread(() -> {
            try {
                connectToServer();
            } catch (Throwable ex) {
                handleException(ex);
            }
            pl.getLogger().info("Disconnected from bungeecord.");
            isConnected = false;
        }).start();
    }

    // Connects a socket to the PortalServer
    private void connectToServer() throws IOException, RequestException, ClassNotFoundException, GeneralSecurityException {
        pl.getLogger().info("Connecting to bungeecord . . .");
        socket = new Socket();
        socket.connect(new InetSocketAddress(pl.config.proxyAddress, pl.config.proxyPort));

        EncryptionManager manager = new EncryptionManager(pl.config.encryptionKey);
        objectStream = new RequestStream(socket.getInputStream(), socket.getOutputStream(), manager);

        // Send a RegisterRequest to inform the proxy of our existence
        sendRequest(new RegisterRequest(pl.getDescription().getVersion(), pl.getServer().getPort()));

        while(isConnected) {
            // Read the next request from the server
            Request request = (Request) objectStream.readNextOfType(Request.class);
            pl.logDebug("Received request from server of type %s", request.getClass().getName());
            
            // Send it to the handler, then write the result back to the server
            Response response = handleRequest(request);
            pl.logDebug("Sending response");
            objectStream.writeObject(response);
        }

        socket.close();
        pl.getLogger().warning("This probably shouldn't happen!");
    }

    private Response handleRequest(Request request) {
        pl.logDebug("Procesing request of type %s", request.getClass().getName());
        Object result = null;

        try {
            // Send the request to the correct handler
            if(request instanceof ServerBoundRequestContainer) {
                result = handleServerBoundRequestContainer((ServerBoundRequestContainer) request);
            } else if(request instanceof GetBlockDataArrayRequest) {
                result = handleGetBlockDataArrayRequest((GetBlockDataArrayRequest) request);
            } else if(request instanceof TeleportPlayerRequest) {
                handleTeleportPlayerRequest((TeleportPlayerRequest) request);
            }
        }   catch(RequestException ex) { // Send back RequestExceptions to the requester
            pl.logDebug("Returning request exception!");
            return Response.error(ex);
        }   catch(Throwable ex) { // If any other exception was thrown, box it in a RequestException
            pl.logDebug("Returning request exception!");
            return Response.error(new RequestException(ex));
        }

        return Response.success(result);
    }

    private Object handleGetBlockDataArrayRequest(GetBlockDataArrayRequest request) {
        return pl.getBlockArrayProcessor().handleGetBlockDataArrayRequest(request);
    }

    private Object handleServerBoundRequestContainer(ServerBoundRequestContainer request) throws RequestException, IOException, ClassNotFoundException {
        return handleRequest(request.getRequest());
    }

    private void handleTeleportPlayerRequest(TeleportPlayerRequest request) {
        // Find the position specified in the TeleportPlayerRequest
        Location destPos = new Location(pl.getServer().getWorld(request.getDestWorldName()),
                request.getDestX(), request.getDestY(), request.getDestZ(),
                request.getDestYaw(), request.getDestPitch());
        
        pl.logDebug("Setting to teleport on join for player %s", request.getPlayerId());
        pl.setToTeleportOnJoin(request.getPlayerId(), destPos); // Make sure the player teleports to the destination when they join
    }

    // Sends a request to the bukkit server, and returns the result. Throws an exception if the request failed
    public Object sendRequest(Request request) throws RequestException {
        pl.logDebug("Sending request of type %s", request.getClass().getName());
        try {
            objectStream.writeObject(request);

            Response response = ((Response) objectStream.readNextOfType(Response.class)); // Read the response
            pl.logDebug("Received response");
            return response.getResult(); // Get the result, throwing an error if there is one
        }   catch(IOException | ClassNotFoundException | GeneralSecurityException ex) {
            handleException(ex); // Shut down the connection if necessary
            throw new RequestException("Error occured while reading the response!", ex);
        }
    }

    // Sends a forward request to the proxy that sends the request to another server in the network
    public Object sendRequestToServer(Request request, String serverName) throws RequestException {
        // What is sent back by the proxy is a response within a response
        // Calling the sendRequest method unwraps the first response, throwing any errors that occured *while forwarding the request*, like a server not existing
        // What we have now is the actual response sent by the destination server
        Response response = (Response) sendRequest(new ServerBoundRequestContainer(serverName, request));

        try {
			return response.getResult();
		} catch (ClassNotFoundException | IOException ex) {
            handleException(ex);
			return new RequestException("Error occured while reading the result!", ex);
		}
    }

    // Should be called whenever there is any kind of exception while reading from/writing to the socket.
    // This shuts down the connection, so that later attempts to read or write will fail.
    // Methods that can throw exceptions other than RequestException should direct them here and then package them as RequestException to be sent back to the caller
    private void handleException(Throwable ex) {
        if(!isConnected) {return;}

        shutdown(); // Shut down the connection

        // An EOFException is thrown whenever the other side closes the connection. This shouldn't be printed.
        if(!(ex instanceof EOFException)) {
            pl.getLogger().severe("An error occured while connected to the proxy!");
            ex.printStackTrace();
        }
    }

    public void shutdown() {
        if(!isConnected) {return;} // Return if the socket is already disconnected
        
        // Close the socket, and set isConnected to false so that the error handling code knows that the SocketException is intentional
        isConnected = false;
        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
