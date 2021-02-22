package com.lauriethefish.betterportals.shared.net;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Before the client is connected, it needs to send information about the plugin and game version for other servers to see.
 * This is also used to guarantee that the plugin version on the client & server is the same.
 */
@Getter
@Setter
public class Handshake implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pluginVersion;
    private String gameVersion;
    private int serverPort;
}
