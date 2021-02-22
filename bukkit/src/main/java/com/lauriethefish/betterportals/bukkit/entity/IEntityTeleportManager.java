package com.lauriethefish.betterportals.bukkit.entity;

public interface IEntityTeleportManager {
    /**
     * Called every tick while the portal is active.
     * Teleports entities that walk through the portal
     */
    void update();
}
