package com.lauriethefish.betterportals.bukkit.runnables;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

public class ClientReconnect implements Runnable  {
    private BetterPortals pl;

    public ClientReconnect(BetterPortals pl) {
        this.pl = pl;
        int reconnectionDelay = pl.getLoadedConfig().getProxy().getReconnectionDelay();;
        pl.getLogger().info("Attempting reconnection in " + reconnectionDelay + " ticks.");
        pl.getServer().getScheduler().runTaskLater(pl, this, reconnectionDelay);
    }

    @Override
    public void run() {
        if(!pl.getNetworkClient().isConnected()) {
            pl.connectToProxy(); // Attempt a reconnection
        }   else    {
            pl.logDebug("Reconnect worker did not start a reconnection since it had already happened.");
        }
    }
}
