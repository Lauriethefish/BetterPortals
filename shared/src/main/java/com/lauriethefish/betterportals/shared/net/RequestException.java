package com.lauriethefish.betterportals.shared.net;

/**
 * Wraps any errors that occur while sending or processing a request.
 */
public class RequestException extends Exception {
    private static final long serialVersionUID = 1;

    public RequestException(String message) {
        super(message);
    }

    public RequestException(Throwable cause) {
        super(cause);
    }

    public RequestException(Throwable cause, String message) {
        super(message, cause);
    }
}
