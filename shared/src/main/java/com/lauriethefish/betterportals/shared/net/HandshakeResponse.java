package com.lauriethefish.betterportals.shared.net;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
public class HandshakeResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Result {
        SUCCESS(),

        /**
         * The server and client plugin version must be the same.
         */
        PLUGIN_VERSION_MISMATCH(),

        /**
         * If a server attempts to connect that isn't actually registered with bungeecord.
         */
        SERVER_NOT_REGISTERED()
    }

    private Result status;
}
