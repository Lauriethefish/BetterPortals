package com.lauriethefish.betterportals.bukkit.net;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

@Singleton
public class ClientReconnectHandler implements IClientReconnectHandler, Runnable  {
    private final JavaPlugin pl;
    private final ProxyConfig proxyConfig;
    private final IPortalClient portalClient;
    private final Logger logger;

    private volatile BukkitTask reconnectWorker;
    private boolean isFirstReconnectionAttempt;

    @Inject
    public ClientReconnectHandler(JavaPlugin pl, ProxyConfig proxyConfig, IPortalClient portalClient, Logger logger) {
        this.pl = pl;
        this.proxyConfig = proxyConfig;
        this.portalClient = portalClient;
        this.logger = logger;
    }

    @Override
    public void prematureReconnect() {
        portalClient.connect(true);
    }

    @Override
    public void onClientDisconnect() {
        // If there is already a reconnection task running, don't start one again.
        if(reconnectWorker != null) {return;}
        if(!portalClient.getShouldReconnect()) {return;}

        int reconnectionDelay = proxyConfig.getReconnectionDelay();
        if(reconnectionDelay == -1) {return;}

        logger.info("Scheduling reconnection attempt in %d ticks", reconnectionDelay);
        isFirstReconnectionAttempt = true;
        reconnectWorker = Bukkit.getScheduler().runTaskTimer(pl, this, reconnectionDelay, reconnectionDelay);
    }

    @Override
    public void run() {
        if(isFirstReconnectionAttempt) {
            logger.info("Processing reconnection attempt to proxy");
        }   else    {
            logger.fine("Processing reconnection attempt to proxy");
        }

        // If there was a successful handshake
        if(portalClient.canReceiveRequests()) {
            logger.fine("Proxy is now connected! Stopping . . .");
            reconnectWorker.cancel();
            reconnectWorker = null;
            return;
        }

        if(portalClient.isConnectionOpen()) {
            logger.fine("Previous reconnection attempt still ongoing");
            return;
        }

        portalClient.connect(isFirstReconnectionAttempt); // Only print exceptions on the first reconnection attempt
        if(isFirstReconnectionAttempt) {
            isFirstReconnectionAttempt = false;
            logger.info("NOTE: Subsequent reconnection attempts will not print to console");
        }
    }
}
