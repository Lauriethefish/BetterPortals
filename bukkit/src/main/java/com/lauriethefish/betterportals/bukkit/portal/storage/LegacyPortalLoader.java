package com.lauriethefish.betterportals.bukkit.portal.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.api.PortalDirection;
import com.lauriethefish.betterportals.api.PortalPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Singleton
public class LegacyPortalLoader {
    private final IPortal.Factory portalFactory;

    @Inject
    public LegacyPortalLoader(IPortal.Factory portalFactory) {
        this.portalFactory = portalFactory;
    }

    private @NotNull Vector loadPortalSize(@NotNull ConfigurationSection section) {
        return new Vector(
                section.getInt("x"),
                section.getInt("y"),
                0.0
        );
    }

    private @NotNull Location loadLocation(@NotNull ConfigurationSection section)  {
        return new Location(
                Bukkit.getWorld(
                        Objects.requireNonNull(section.getString("world"), "Missing world section")),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")
        );
    }

    public IPortal loadLegacyPortal(ConfigurationSection section) {
        PortalPosition originPos = new PortalPosition(
                loadLocation(Objects.requireNonNull(section.getConfigurationSection("portalPosition"), "Missing origin position")),
                PortalDirection.fromStorage(Objects.requireNonNull(section.getString("portalDirection"), "Missing origin direction"))
        );

        PortalPosition destPos = new PortalPosition(
                loadLocation(Objects.requireNonNull(section.getConfigurationSection("destinationPosition"), "Missing destination position")),
                PortalDirection.fromStorage(Objects.requireNonNull(section.getString("destinationDirection"), "Missing destination direction"))
        );

        Vector portalSize = loadPortalSize(Objects.requireNonNull(section.getConfigurationSection("portalSize"), "Missing portal size"));
        boolean anchored = section.getBoolean("anchored");
        String owner = section.getString("owner");
        UUID ownerId = owner == null ? null : UUID.fromString(owner);

        return portalFactory.create(originPos, destPos, portalSize, anchored, UUID.randomUUID(), ownerId, null);
    }
}
