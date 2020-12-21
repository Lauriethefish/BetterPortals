package com.lauriethefish.betterportals.network;

import java.net.InetSocketAddress;

import com.lauriethefish.betterportals.network.Response.RequestException;

import lombok.Getter;

// Sent by a server when the plugin first initialises, used to ensure that the plugin version is correct, amongst other info.
public class RegisterRequest implements Request {
    private static final long serialVersionUID = -6671138109964944654L;

    @Getter private String pluginVersion;
    @Getter private int serverPort; // Used to find which server this request is actually from
    public RegisterRequest(String pluginVersion, int serverPort) {
        this.pluginVersion = pluginVersion;
        this.serverPort = serverPort;
    }

    // Exception for if there was no valid server registered
    public static class UnknownRegisterServerException extends RequestException    {
        private static final long serialVersionUID = 830319387336912275L;

        public UnknownRegisterServerException(InetSocketAddress address)   {
            super("No server was found with address " + address.toString());
        }
    }

    // Called if the connecting server has the wrong plugin version
    public static class PluginVersionMismatchException extends RequestException     {
        private static final long serialVersionUID = 3473736369128055628L;

        public PluginVersionMismatchException(String invalidVersion, String expectedVersion) {
            super("BungeeCord plugin version (" + expectedVersion + ") did not match server plugin version of " + invalidVersion);
        }
    }
}
