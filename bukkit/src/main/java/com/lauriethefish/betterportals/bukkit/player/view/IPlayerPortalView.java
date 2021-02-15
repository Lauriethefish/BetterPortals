package com.lauriethefish.betterportals.bukkit.player.view;

import org.bukkit.Location;

public interface IPlayerPortalView {
    // Called every tick while viewing through this view
    void update();

    // Should reset the view through the portal back to the normal world
    // Previous position is needed as some things don't need to be reset when switching worlds/moving far enough away
    void onDeactivate();
}
