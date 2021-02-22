package com.lauriethefish.betterportals.bukkit.portal.blend;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface IDimensionBlendManager {
    /**
     * Performs the random swapping of blocks from <code>destination</code> to <code>origin</code>.
     * Certain blocks aren't copied to avoid breaking through the nether roof, for example.
     * @param origin The place where blocks will be copied to, this should ideally be in the center of the portal
     * @param destination Where the blocks will be copied from
     */
    void performBlend(@NotNull Location origin, @NotNull Location destination);
}
