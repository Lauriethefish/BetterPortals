package com.lauriethefish.betterportals.api;

import lombok.Setter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Main class of the BetterPortals API.
 * NOTE: No API calls should be made when the plugin is disabled.
 */
public abstract class BetterPortalsAPI {
    @Setter private static BetterPortalsAPI instance = null;

    /**
     * Gets the current API instance.
     * @return The current API instance.
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public static @NotNull BetterPortalsAPI get() {
        if(instance == null) {
            throw new IllegalStateException("Attempted to call API when BetterPortals was not enabled");
        }

        return instance;
    }

    /**
     * Creates a portal and registers it with the plugin so that it can be seen and teleported through.
     * @param originPosition Where the portal comes from
     * @param destinationPosition Where the portal goes to
     * @param size The size of this portal, on the X and Y. The Z coordinate is unused.
     * @param owner The unique ID of the player who owns the portal, or null if there is none
     * @param name The name of the portal. Must not contain spaces
     * @param isCustom Whether this portal is a custom portal (custom portals aren't removed if the portal blocks are missing)
     * @return The newly created portal.
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public abstract @NotNull BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name, boolean isCustom);

    /**
     * Creates a custom portal and registers it with the plugin so that it can be seen and teleported through.
     * @param originPosition Where the portal comes from
     * @param destinationPosition Where the portal goes to
     * @param size The size of this portal, on the X and Y. The Z coordinate is unused.
     * @param owner The unique ID of the player who owns the portal, or null if there is none
     * @param name The name of the portal. Must not contain spaces
     * @return The newly created portal.
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public @NotNull BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name) {
        return createPortal(originPosition, destinationPosition, size, owner, name, true);
    }

    /**
     * Creates a custom portal with no owner and no name and registers it with the plugin so that it can be seen and teleported through.
     * @param originPosition Where the portal comes from
     * @param destinationPosition Where the portal goes to
     * @param size The size of this portal, on the X and Y. The Z coordinate is unused.
     * @return The newly created portal.
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public @NotNull BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size) {
        return createPortal(originPosition, destinationPosition, size, null, null);
    }

    /**
     * Adds a predicate for activating a portal.
     * Each portal that is within activation distance is checked against this every tick for each player, so try not to make it too complex!
     * If this returns false, the portal will not be activated, so it cannot be viewed through, or teleported through by any player.
     * @param predicate The predicate to add
     * @throws IllegalStateException if BetterPortals is not enabled

     */
    public abstract void addPortalActivationPredicate(@NotNull PortalPredicate predicate);

    /**
     * Removes the added predicate <code>predicate</code> for activating a portal.
     * @param predicate The predicate to remove
     * @throws IllegalStateException if BetterPortals is not enabled
     * @throws UnknownPredicateException if the predicate wasn't added
     */
    public abstract void removePortalActivationPredicate(@NotNull PortalPredicate predicate);

    /**
     * Adds a predicate for viewing through a portal. If this fails, the player will still be able to teleport but will not be able to view through.
     * This is called every tick for each portal that a player views, so don't make it too complex!
     * @param predicate The predicate to add
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public abstract void addPortalViewPredicate(@NotNull PortalPredicate predicate);

    /**
     * Removes the added predicate <code>predicate</code> for viewing a portal.
     * @param predicate The predicate to remove
     * @throws IllegalStateException if BetterPortals is not enabled
     * @throws UnknownPredicateException if the predicate wasn't added
     */
    public abstract void removePortalViewPredicate(@NotNull PortalPredicate predicate);

    /**
     * Adds a predicate for teleporting through a portal. If this fails, the player will not be able to teleport.
     * @param predicate The predicate to add
     * @throws IllegalStateException if BetterPortals is not enabled
     */
    public abstract void addPortalTeleportPredicate(@NotNull PortalPredicate predicate);

    /**
     * Removes the added predicate <code>predicate</code> for teleporting through a portal.
     * @param predicate The predicate to remove
     * @throws IllegalStateException if BetterPortals is not enabled
     * @throws UnknownPredicateException if the predicate wasn't added
     */
    public abstract void removePortalTeleportPredicate(@NotNull PortalPredicate predicate);

    /**
     * Fetches a portal by its unique ID, as obtained by {@link BetterPortal#getId()}
     * @param id The ID of the portal
     * @return The portal, or null if there is none with this ID
     */
    public abstract @Nullable BetterPortal getPortalById(@NotNull UUID id);
}
