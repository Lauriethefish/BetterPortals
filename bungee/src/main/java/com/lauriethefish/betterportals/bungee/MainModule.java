package com.lauriethefish.betterportals.bungee;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.bungee.net.*;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.logging.OverrideLogger;
import com.lauriethefish.betterportals.shared.net.IRequestHandler;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStream;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStreamFactory;
import com.lauriethefish.betterportals.shared.net.encryption.IEncryptedObjectStream;
import net.md_5.bungee.api.plugin.Plugin;

public class MainModule extends AbstractModule {
    private final BetterPortals pl;
    public MainModule(BetterPortals pl) {
        this.pl = pl;
    }

    @Override
    public void configure() {
        bind(Plugin.class).toInstance(pl);

        bind(Logger.class).toInstance(new OverrideLogger(pl.getLogger()));
        bind(IPortalServer.class).to(PortalServer.class);
        bind(IRequestHandler.class).to(ProxyRequestHandler.class);
        install(new FactoryModuleBuilder().implement(IClientHandler.class, ClientHandler.class).build(ServerHandlerFactory.class));
        install(new FactoryModuleBuilder().implement(IEncryptedObjectStream.class, EncryptedObjectStream.class).build(EncryptedObjectStreamFactory.class));
    }
}
