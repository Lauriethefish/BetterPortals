package com.lauriethefish.betterportals.bukkit.net;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.lauriethefish.betterportals.shared.net.IRequestHandler;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStream;
import com.lauriethefish.betterportals.shared.net.encryption.EncryptedObjectStreamFactory;
import com.lauriethefish.betterportals.shared.net.encryption.IEncryptedObjectStream;

public class NetworkModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(IEncryptedObjectStream.class, EncryptedObjectStream.class)
                .build(EncryptedObjectStreamFactory.class)
        );

        bind(IPortalClient.class).to(PortalClient.class);
        bind(IRequestHandler.class).to(ClientRequestHandler.class);
        bind(IClientReconnectHandler.class).to(ClientReconnectHandler.class);
    }
}
