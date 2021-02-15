package com.lauriethefish.betterportals.bukkit.player.view.entity;

/**
 * Handles adding the player to fake entity trackers to show entities through the portal.
 * Also handles the hiding of entities when they go behind a portal
 */
public interface IPlayerEntityView {
    /**
     * Needs to be called every tick, regardless of if the player moved.
     * This is because entities can move, even if the player is stationary.
     */
    void update();

    /**
     * Removes all fake entities, and re-shows all hidden ones.
     * @param shouldResetEntities This should be true if still within view distance of the portal/not changing worlds.
     */
    void onDeactivate(boolean shouldResetEntities);
}
