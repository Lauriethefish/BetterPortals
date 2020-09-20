package com.lauriethefish.betterportals.runnables;

import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.math.PlaneIntersectionChecker;
import com.lauriethefish.betterportals.portal.PortalPos;

// Stores the data added to a queue to be processed in the Asyncronous BlockProcessor task
public class UpdateData {
    public PlayerData playerData;
    public PlaneIntersectionChecker checker;
    public PortalPos portal;
    public UpdateData(PlayerData playerData, PlaneIntersectionChecker checker, PortalPos portal)   {
        this.playerData = playerData;
        this.portal = portal;
        this.checker = checker;
    }
}
