package com.lauriethefish.betterportals.shared.net;

/**
 * Thrown whenever a request that requires a server name is sent and the server doesn't exist.
 */
public class ServerNotFoundException extends RequestException   {
    private static final long serialVersionUID = 1;

    public ServerNotFoundException(String serverName) {
        super("Unable to find server with name " + serverName);
    }
}
