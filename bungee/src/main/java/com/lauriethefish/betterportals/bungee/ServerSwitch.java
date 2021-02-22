package com.lauriethefish.betterportals.bungee;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bungee.net.IClientHandler;
import com.lauriethefish.betterportals.bungee.net.IPortalServer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.RequestException;
import com.lauriethefish.betterportals.shared.net.requests.PreviousServerPutRequest;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class ServerSwitch implements Listener {
    private final IPortalServer portalServer;
    private final Logger logger;

    @Inject
    public ServerSwitch(IPortalServer portalServer, Logger logger, Plugin pl) {
        this.portalServer = portalServer;
        this.logger = logger;
        pl.getProxy().getPluginManager().registerListener(pl, this);
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        if(event.getFrom() == null) {return;}

        IClientHandler from = portalServer.getServer(event.getFrom().getName());
        IClientHandler to = portalServer.getServer(event.getPlayer().getServer().getInfo().getName());

        if(from == null || to == null) {return;}

        PreviousServerPutRequest request = new PreviousServerPutRequest();
        request.setPlayerId(event.getPlayer().getUniqueId());
        request.setPreviousServer(event.getFrom().getName());

        to.sendRequest(request, (response) -> {
            try {
                response.checkForErrors();
            }   catch(RequestException ex) {
                logger.warning("Failed to set previous server for player %s", event.getPlayer().getUniqueId());
                ex.printStackTrace();
            }
        });
    }
}