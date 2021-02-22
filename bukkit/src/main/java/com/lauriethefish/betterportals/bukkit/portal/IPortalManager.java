package com.lauriethefish.betterportals.bukkit.portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Handles the storage, registering, unregistering and finding of portals.
 * All loaded portals are stored here
 */
public interface IPortalManager {
    /**
     * @return Every registered custom or nether portal
     */
    Collection<IPortal> getAllPortals();

    /**
     * Finds every portal at <code>originLoc</code>.
     * Multiple portals can have the same origin position!
     * @param originLoc Location to find portals at
     * @return The portals at <code>originLoc</code>.
     */
    Collection<IPortal> getPortalsAt(Location originLoc);

    /**
     * Finds one of the portals at <code>originLoc</code>.
     * Note that there may be multiple portals at this position - this just returns one of them or null if there isn't one.
     * Returns null if no portals are at <code>originLoc</code>.
     * @param originLoc The position to get the portal at/
     * @return One of the portals
     */
    @Nullable default IPortal getPortalAt(@NotNull Location originLoc) {
        Collection<IPortal> portals = getPortalsAt(originLoc);
        if(portals.size() == 0) {
            return null; // Avoid throwing NullPointerException if there aren't any
        }   else    {
            return portals.iterator().next();
        }
    }

    /**
     * Gets the portal with the specified unique ID
     * @param id The ID of the portal
     * @return The fetched portal, or null if there isn't one with this ID
     */
    @Nullable IPortal getPortalById(@NotNull UUID id);

    /**
     * Finds the closest portal to <code>position</code> that is under the maximum distance and matches <code>predicate</code>.
     * @param position The position to look for portals around
     * @param maximumDistance The maximum distance from this position before this returns null
     * @param predicate If this fails, the portal won't be checked to see if it's the closest.
     * @return The closest portal to <code>position</code>, or null if there is none.
     */
    @Nullable IPortal findClosestPortal(@NotNull Location position, double maximumDistance, Predicate<IPortal> predicate);

    /**
     * @see IPortalManager#findClosestPortal(Location, double, Predicate)
     */
    @Nullable default IPortal findClosestPortal(@NotNull Location position, double maximumDistance) {
        return findClosestPortal(position, maximumDistance, portal -> true);
    }

    /**
     * @see IPortalManager#findClosestPortal(Location, double, Predicate)
     */
    @Nullable default IPortal findClosestPortal(@NotNull Location position, @NotNull Predicate<IPortal> predicate) {
        return findClosestPortal(position, Double.POSITIVE_INFINITY, predicate);
    }

    /**
     * @see IPortalManager#findClosestPortal(Location, double, Predicate)
     */
    @Nullable default IPortal findClosestPortal(@NotNull Location position) {
        return findClosestPortal(position, Double.POSITIVE_INFINITY);
    }

    /**
     * Tests the portals against the {@link com.lauriethefish.betterportals.bukkit.portal.predicate.PortalPredicateManager} to find which ones are activatable by this player.
     * @param player The player to test
     * @return The activatable portals, may be empty.
     */
    @NotNull Collection<IPortal> findActivatablePortals(@NotNull Player player);

    /**
     * Registers this portal, allowing players to see through it, entities/players to teleport through it, etc.
     * @param portal The portal to be registered
     */
    void registerPortal(@NotNull IPortal portal);

    /**
     * Removes all portals at <code>originLoc</code>.
     * @param originLoc The location to remove portals at
     * @return The number of portals removed
     */
    int removePortalsAt(@NotNull Location originLoc);

    /**
     * Removes this specific portal, meaning that it can no longer be seen through or used.
     * @param portal Portal to remove
     * @return Whether it was actually registered in the first place
     */
    boolean removePortal(@NotNull IPortal portal);

    /**
     * Removes a portal by its unique ID
     * @param id The unique ID of the portal to be removed
     * @return Whether a portal with this ID was registered.
     */
    boolean removePortalById(@NotNull UUID id);
}
