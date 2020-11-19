package com.lauriethefish.betterportals.bukkit.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.network.RegisterRequest;
import com.lauriethefish.betterportals.network.Request;
import com.lauriethefish.betterportals.network.Response;
import com.lauriethefish.betterportals.network.ServerBoundRequestContainer;
import com.lauriethefish.betterportals.network.SyncronizedObjectStream;
import com.lauriethefish.betterportals.network.Response.RequestException;

// Handles connecting to the bungeecord/velocity server to allow cross-server portals
public class PortalClient {
    private BetterPortals pl;

    private Socket socket;
    private volatile boolean isConnected = true;

    private SyncronizedObjectStream objectStream;

    public PortalClient(BetterPortals pl) {
        this.pl = pl;

        new Thread(() -> {
            try {
                connectToServer();
            } catch (EOFException ex) {
                // An EOFException is thrown whenever the other side closes the connection. This shouldn't be printed.
            } catch (IOException | RequestException | ClassNotFoundException ex)    {
                if(isConnected) { // An IOException is thrown whenever this side closes the connection from another thread, so don't print it if we've disconnected
                    pl.getLogger().severe("An error occured while connected to the proxy!");
                    ex.printStackTrace();
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            pl.getLogger().info("Disconnected from bungeecord.");
            isConnected = false;
        }).start();
    }

    // Connects a socket to the PortalServer
    private void connectToServer() throws IOException, RequestException, ClassNotFoundException {
        pl.getLogger().info("Connecting to bungeecord . . .");
        socket = new Socket();
        socket.connect(new InetSocketAddress(pl.config.proxyAddress, pl.config.proxyPort));

        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
        objectStream = new SyncronizedObjectStream(inputStream, outputStream);

        // Send a RegisterRequest to inform the proxy of our existence
        sendRequest(new RegisterRequest(pl.getDescription().getVersion(), pl.getServer().getPort()));

        while(isConnected) {
            // Read the next request from the server
            Request request = (Request) objectStream.readNextOfType(Request.class);
            pl.logDebug("Received request from client %s of type %s", socket.getInetAddress(), request.getClass().getName());
            
            handleRequest(request);
        }

        socket.close();
        pl.getLogger().warning("This probably shouldn't happen!");
    }

    private void handleRequest(Request request) throws IOException {
        Object result = null;

        objectStream.writeObject(Response.success(result));
    }

    // Sends a request to the bukkit server, and returns the result. Throws an exception if the request failed
    public Object sendRequest(Request request) throws IOException, RequestException, ClassNotFoundException {
        pl.logDebug("Sending request of type %s", request.getClass().getName());
        objectStream.writeObject(request);

        Response response = ((Response) objectStream.readNextOfType(Response.class)); // Read the response
        pl.logDebug("Received response");
        return response.getResult(); // Get the result, throwing an error if there is one
    }

    // Sends a forward request to the proxy that sends the request to another server in the network
    public Object sendRequestToServer(Request request, String serverName) throws IOException, RequestException, ClassNotFoundException {
        // What is sent back by the proxy is a response within a response
        // Calling the sendRequest method unwraps the first response, throwing any errors that occured *while forwarding the request*, like a server not existing
        // What we have now is the actual response sent by the destination server
        Response response = (Response) sendRequest(new ServerBoundRequestContainer(serverName, request));

        return response.getResult(); // Throw an error if the request failed
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
