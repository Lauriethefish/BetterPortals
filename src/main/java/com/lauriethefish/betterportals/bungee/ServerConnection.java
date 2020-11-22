package com.lauriethefish.betterportals.bungee;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.lauriethefish.betterportals.network.RegisterRequest;
import com.lauriethefish.betterportals.network.Request;
import com.lauriethefish.betterportals.network.Response;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer;
import com.lauriethefish.betterportals.network.SyncronizedObjectStream;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer.ServerNotFoundException;
import com.lauriethefish.betterportals.network.RegisterRequest.UnknownRegisterServerException;
import com.lauriethefish.betterportals.network.Response.RequestException;

import net.md_5.bungee.api.config.ServerInfo;

public class ServerConnection {
    private BetterPortals pl;
    private Socket socket;

    private ServerInfo server = null;

    private SyncronizedObjectStream objectStream;
    private volatile boolean isConnected = true;

    // Starts a new thread to handle connections to this server
    public ServerConnection(BetterPortals pl, Socket socket) {
        this.pl = pl;
        this.socket = socket;
        new Thread(() -> {
            // Print out any exceptions that occur
            try {
                handleClient();
            } catch (EOFException ex) {
                // An EOFException is thrown whenever the other side closes the connection. This shouldn't be printed.
            } catch (IOException | ClassNotFoundException | RuntimeException ex) {
                if(isConnected) { // An IOException is thrown whenever this side closes the connection from another thread, so don't print it if we've disconnected
                    pl.getLogger().warning("An error occurred while processing the server " + socket.getInetAddress().toString());
                    ex.printStackTrace();
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            isConnected = false;
            pl.getPortalServer().unregisterConnection(server);
            pl.getLogger().info("Server " + socket.getInetAddress() + " disconnected");
        }).start();
    }

    private void handleClient() throws IOException, ClassNotFoundException {
        pl.logDebug(String.format("Client connected with address %s.", socket.getInetAddress()));

        // All of the serialization logic is just done with Java's built-in
        // serialization, so we create an Object input/output stream
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        objectStream = new SyncronizedObjectStream(inputStream, outputStream);

        // Keep checking for requests while the socket is still open
        while (isConnected) {
            // Read the next request from the server
            Request request = (Request) objectStream.readNextOfType(Request.class);
            pl.logDebug("Received request from client %s of type %s", socket.getInetAddress(), request.getClass().getName());
            
            handleRequest(request);
        }

        socket.close();
    }

    private void handleRequest(Request request) throws IOException, ClassNotFoundException {
        Object result = null;
        try {
            // Send this request to the correct handler function
            if (request instanceof RegisterRequest) {
                handleRegisterRequest((RegisterRequest) request);
            } else if (request instanceof ServerBoundRequestContainer) {
                result = handleServerBoundRequestContainer((ServerBoundRequestContainer) request);
            }

        } catch (RequestException ex) {
            // If an error was caught, send it to the handler on the server
            objectStream.writeObject(Response.error(ex));
            return;
        }

        // Throw an error if the server wasn't found in the first request
        if (server == null) {
            throw new RuntimeException("Client did not provide a valid RegisterRequest as its first request!");
        }

        objectStream.writeObject(Response.success(result));
    }

    // Finds the correct ServerInfo that matches this RegisterRequest
    @SuppressWarnings("deprecation")
    private void handleRegisterRequest(RegisterRequest registerRequest) throws UnknownRegisterServerException {
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
    public Object sendRequest(Request request) throws IOException, RequestException, ClassNotFoundException  {
        objectStream.writeObject(request);

        return ((Response) objectStream.readNextOfType(Response.class)).getResult();
    }

    private Object handleServerBoundRequestContainer(ServerBoundRequestContainer request) throws ServerNotFoundException, IOException, ClassNotFoundException, RequestException {
        // Find the server this request should be forwarded to
        ServerConnection dServerConnection = pl.getPortalServer().getConnection(request.getDestinationServer());
        if(dServerConnection == null) {
            throw new ServerNotFoundException(request.getDestinationServer());
        }
        
        // Send the bytes of the request, and get the response
        return dServerConnection.sendRequest(request);
    }
    
    // Closes the connection to the client
    public void shutdown() {
        if(!isConnected) {return;} // Return if the socket is already disconnected

        isConnected = false;
        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
