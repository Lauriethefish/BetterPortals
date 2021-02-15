package com.lauriethefish.betterportals.bukkit.portal.predicate;

import com.google.inject.Singleton;
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
 */
@Singleton
public class TeleportPermissions implements PortalPredicate {
    @Override
    public boolean test(@NotNull IPortal portal, @NotNull Player player) {
        PluginManager pm = Bukkit.getPluginManager();
        String permName;

        if (portal.isNetherPortal()) {
            permName = String.format("betterportals.use.nether.%s", portal.getOriginPos().getWorld().getName());
        } else {
            // Portals with no names default to just requiring the betterportals.use permission.
            if (portal.getName() == null) {
                return player.hasPermission("betterportals.use");
            } else {
                permName = String.format("betterportals.use.custom.%s", portal.getName());
            }
        }

        // Get the permission, setting it to a default of true if necessary
        Permission permission = pm.getPermission(permName);
        if (permission == null) {
            permission = new Permission(permName, PermissionDefault.TRUE);
            pm.addPermission(permission);
        }

        return player.hasPermission("betterportals.use") && player.hasPermission(permission);
    }
}
