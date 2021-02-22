package com.lauriethefish.betterportals.bungee.net;

import java.net.Socket;

public interface ServerHandlerFactory {
    IClientHandler create(Socket socket);
}
