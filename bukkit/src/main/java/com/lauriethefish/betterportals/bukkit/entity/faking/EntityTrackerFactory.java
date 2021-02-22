package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.entity.Entity;

public interface EntityTrackerFactory {
    IEntityTracker create(Entity entity, IPortal portal);
}
