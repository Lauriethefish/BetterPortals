package com.lauriethefish.betterportals.bukkit.runnables;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

public class ClientReconnect implements Runnable  {
    private BetterPortals pl;

    public ClientReconnect(BetterPortals pl) {
        this.pl = pl;
        pl.getLogger().info("Attempting reconnection in " + pl.config.reconnectionDelay + " ticks.");
        pl.getServer().getScheduler().runTaskLater(pl, this, pl.config.reconnectionDelay);
    }

    @Override
    public void run() {
        pl.connectToProxy(); // Attempt a reconnection
    }
}
