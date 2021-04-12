package com.lauriethefish.betterportals.shared.net;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Replied by the proxy or client server as the result of the request
 */
@Setter
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Used to preserve request/response ordering.
     */
    @Getter private int id;

    private Object result;
    private RequestException error;

    /**
     * Checks for errors, then returns the result of the request, if there is one.
     * @return The result of the request
     * @throws RequestException If an error occurred while processing the request
     */
    public Object getResult() throws RequestException {
        checkForErrors();

        return result;
    }

    /**
     * Throws an error that occurred while processing the request, if there was one.
     * @throws RequestException If there was an error while processing the request
     */
    public void checkForErrors() throws RequestException {
        if(error != null) {
            throw error;
        }
    }
}
