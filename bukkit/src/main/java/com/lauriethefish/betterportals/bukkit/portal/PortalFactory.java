package com.lauriethefish.betterportals.bukkit.portal;

import com.google.inject.assistedinject.Assisted;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.UUID;

public interface PortalFactory {
    IPortal create(@Assisted("originPos") @NotNull PortalPosition originPos,
                   @Assisted("destPos") @NotNull PortalPosition destPos,
                   @NotNull Vector size, boolean isCustom,
                   @NotNull @Assisted("id") UUID id,
                   @Nullable @Assisted("ownerId") UUID ownerId,
                   @Nullable @Assisted("name") String name
    );
}
