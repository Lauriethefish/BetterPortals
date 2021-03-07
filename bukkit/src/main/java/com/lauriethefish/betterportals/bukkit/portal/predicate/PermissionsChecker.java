package com.lauriethefish.betterportals.bukkit.portal.predicate;

import com.lauriethefish.betterportals.api.BetterPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

/**
 * Checks that players have permission before teleporting them.
 * The permissions work as follows:
 * <code>betterportals.use.nether.originWorldName</code> for nether portals.
 * <code>betterportals.use.custom.originPortalName</code> for custom portals.
 *
 * And the same for <code>betterportals.see</code>, just with viewing.
 * This class does both, since you can pass a different path into the constructor
 */
public class PermissionsChecker implements PortalPredicate {
    private final String basePath;

    public PermissionsChecker(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public boolean test(@NotNull BetterPortal portal, @NotNull Player player) {
        PluginManager pm = Bukkit.getPluginManager();
        String permName = basePath + ((IPortal) portal).getPermissionPath();

        // Get the permission, setting it to a default of true if necessary
        Permission permission = pm.getPermission(permName);
        if (permission == null) {
            permission = new Permission(permName, PermissionDefault.TRUE);
            pm.addPermission(permission);
        }

        return player.hasPermission(basePath) && player.hasPermission(permission);
    }
}
