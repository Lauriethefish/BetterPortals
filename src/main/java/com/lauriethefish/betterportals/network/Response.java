package com.lauriethefish.betterportals.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import lombok.Getter;

// Allows you to send back an error or success from a request
public class Response implements Serializable   {
    private static final long serialVersionUID = -2447012242238557293L;

    private byte[] result;
    @Getter private RequestException error; // This is null if the result was a success

    // Private constructor so that we use the statis methods below
    private Response(Object result, RequestException error)  throws IOException {
        // Serialize the result to a byte array to avoid it being deserialized on the proxy
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(result);

        this.result = byteArrayOutputStream.toByteArray();
        this.error = error;
    }

    // Methods for easily creating an error or success result
    public static Response error(RequestException error) throws IOException {
        return new Response(null, error);
    }

    public static Response success(Object result) throws IOException  {
        return new Response(result, null);
    }

    // Throw the exception if one was found, otherwise return the result
    public Object getResult() throws RequestException, IOException, ClassNotFoundException {
        if(error != null)   {throw error;}

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(result);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return objectInputStream.readObject();
    }

    // Returns the result if there is one, or null if there isn't.
    public Object tryGetResult()   {
        return result;
    }

    // Simple marker for all exceptions that are returned in a response
    public static class RequestException extends Exception {
        private static final long serialVersionUID = -276936249697279234L;

        public RequestException(String message) {
            super(message);
        }

        public RequestException(Throwable cause) {
            super(cause);
        }

        public RequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
