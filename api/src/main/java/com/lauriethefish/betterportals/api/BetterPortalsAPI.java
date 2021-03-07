package com.lauriethefish.betterportals.api;

import lombok.Setter;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class BetterPortalsAPI {
    @Setter private static BetterPortalsAPI instance = null;

    /**
     * Gets the current API instance.
     * @return The current API instance.
     */
    public static BetterPortalsAPI get() {
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
     */
    public abstract BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name, boolean isCustom);

    /**
     * Creates a custom portal and registers it with the plugin so that it can be seen and teleported through.
     * @param originPosition Where the portal comes from
     * @param destinationPosition Where the portal goes to
     * @param size The size of this portal, on the X and Y. The Z coordinate is unused.
     * @param owner The unique ID of the player who owns the portal, or null if there is none
     * @param name The name of the portal. Must not contain spaces
     * @return The newly created portal.
     */
    public BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size, @Nullable UUID owner, @Nullable String name) {
        return createPortal(originPosition, destinationPosition, size, owner, name, true);
    }

    /**
     * Creates a custom portal with no owner and no name and registers it with the plugin so that it can be seen and teleported through.
     * @param originPosition Where the portal comes from
     * @param destinationPosition Where the portal goes to
     * @param size The size of this portal, on the X and Y. The Z coordinate is unused.
     * @return The newly created portal.
     */
    public BetterPortal createPortal(@NotNull PortalPosition originPosition, @NotNull PortalPosition destinationPosition, @NotNull Vector size) {
        return createPortal(originPosition, destinationPosition, size, null, null);
    }

    /**
     * Adds a predicate for activating a portal.
     * Each portal that is activated is checked against this every tick for each player, so try not to make it too complex!
     * If this returns false, the portal will not be activated, so it cannot be viewed through, or teleported through by any player.
     * @param predicate The predicate to add
     */
    public abstract void addPortalActivationPredicate(@NotNull PortalPredicate predicate);

    /**
     * Adds a predicate for viewing through a portal. If this fails, the player will still be able to teleport but will not be able to view through.
     * @param predicate The predicate to add
     */
    public abstract void addPortalViewPredicate(@NotNull PortalPredicate predicate);

    /**
     * Adds a predicate for teleporting through a portal. If this fails, the player will not be able to teleport.
     * @param predicate The predicate to add
     */
    public abstract void addPortalTeleportPredicate(@NotNull PortalPredicate predicate);
}
