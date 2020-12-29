package com.lauriethefish.betterportals.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.lauriethefish.betterportals.network.Response.RequestException;

import lombok.Getter;

// Request for the proxy to forward the contained request to another server
public class ServerBoundRequestContainer implements Request {
    private static final long serialVersionUID = 6541785442214174677L;
    
    @Getter private String destinationServer; // The name of the server this request should be forwarded to
    private byte[] request; // The bytes of the request, which are deserialized on the bukkit/spigot server

    public ServerBoundRequestContainer(String destinationServer, Request request)    {
        this.destinationServer = destinationServer;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(request);
        }   catch(IOException ex) {
            ex.printStackTrace(); // This should never happen!
        }
        this.request = byteArrayOutputStream.toByteArray();
    }

    public Request getRequest() throws IOException, ClassNotFoundException {
        // Deserialize the byte array into the Request object
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(request);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return (Request) objectInputStream.readObject();
    }

    // Thrown if the requested server does not exist
    public static class ServerNotFoundException extends RequestException {
        private static final long serialVersionUID = 5794656098884849821L;

        public ServerNotFoundException(String serverName) {
            super(String.format("No server with name %s exists.", serverName));
        }
    }
}
