package com.lauriethefish.betterportals.shared.net.requests;

import lombok.Getter;
import lombok.Setter;

import java.io.*;

/**
 * Used for when a client server wants to send a request to another client server.
 * The proxy will send the inner request to the client server.
 */
public class RelayRequest extends Request   {
    private static final long serialVersionUID = 1L;

    @Getter @Setter private String destination;
    /**
     * The inner request is stored as a byte array, since it contains types that the proxy might not be able to deserialize.
     */
    private byte[] innerRequest;

    /**
     * Sets and serializes the request that will be relayed.
     * @param request The request to be relayed
     */
    public void setInnerRequest(Request request) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(outputStream).writeObject(request);
        }   catch(IOException ex) {
            // This should never happen
        }

        this.innerRequest = outputStream.toByteArray();
    }

    /**
     * Deserializes the relayed request.
     * @return The relayed request
     * @throws ClassNotFoundException If a class in the relayed request doesn't exist on this server.
     * @throws IOException If the inner request was corrupt somehow and was too short.
     */
    public Request getInnerRequest() throws ClassNotFoundException, IOException {
        return (Request) new ObjectInputStream(new ByteArrayInputStream(innerRequest)).readObject();
    }
}
