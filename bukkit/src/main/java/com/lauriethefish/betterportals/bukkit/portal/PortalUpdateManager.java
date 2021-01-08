package com.lauriethefish.betterportals.bukkit.portal;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.network.BlockDataArrayRequest;

public class PortalUpdateManager {
    private BetterPortals pl;
    private Portal portal;

    private int ticksSinceActivated = -1;

    public PortalUpdateManager(BetterPortals pl, Portal portal) {
        this.pl = pl;
        this.portal = portal;
    }

    public void playerUpdate() {
        // Entities aren't processed for cross-server portals
        if(!portal.isCrossServer()) {// Update the entity list if need need to
            if (ticksSinceActivated % pl.getLoadedConfig().getEntityCheckInterval() == 0) {
                portal.updateNearbyEntities();
            }
            portal.checkEntityTeleportation(); // Teleport entities through the portal if they have crossed in the last tick
        }

        // Update blocks if we need to
        if(ticksSinceActivated % pl.getLoadedConfig().getRendering().getBlockUpdateInterval() == 0)   {
            pl.logDebug("Updating current blocks");
            portal.updateCurrentBlocks();
        }

        ticksSinceActivated++;
    }

    public void onActivate() {
        pl.logDebug("Portal activated by player");
        ticksSinceActivated = 0;
        portal.forceloadDestinationChunks();
    }

    public void onDeactivate() {
        pl.logDebug("Portal no longer activated by player");
        ticksSinceActivated = -1;

        portal.unforceloadDestinationChunks();

        // Clear the cached array when the player no longer activates the portal to avoid leaking memory
        pl.getBlockArrayProcessor().clearCachedArray(portal.createBlockDataRequest(BlockDataArrayRequest.Mode.CLEAR));
    }

    public boolean isActivatedByPlayer() {
        return ticksSinceActivated != -1;
    }
}
