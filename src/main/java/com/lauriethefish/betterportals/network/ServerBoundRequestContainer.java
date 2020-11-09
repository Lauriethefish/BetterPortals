package com.lauriethefish.betterportals.network;

import com.lauriethefish.betterportals.network.Response.RequestException;

import lombok.Getter;

// Request for the proxy to forward the contained request to another server
public class ServerBoundRequestContainer implements Request {
    private static final long serialVersionUID = 6541785442214174677L;
    
    @Getter private String destinationServer; // The name of the server this request should be forwarded to
    @Getter private Request request; // The bytes of the request, which are deserialized on the bukkit/spigot server

    public ServerBoundRequestContainer(String destinationServer, Request request)    {
        this.destinationServer = destinationServer;
        this.request = request;
    }

    // Thrown if the requested server does not exist
    public static class ServerNotFoundException extends RequestException {
        private static final long serialVersionUID = 5794656098884849821L;

        public ServerNotFoundException(String serverName) {
            super(String.format("No server with name %s exists.", serverName));
        }
    }
}
