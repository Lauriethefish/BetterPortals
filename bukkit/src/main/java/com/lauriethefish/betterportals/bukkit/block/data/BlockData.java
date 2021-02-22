package com.lauriethefish.betterportals.bukkit.block.data;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

/**
 * Abstracts the difference between {@link org.bukkit.block.data.BlockData} (modern) and {@link org.bukkit.material.MaterialData} (legacy) block data.
 * I used to use {@link org.bukkit.block.BlockState} for this, but it's not as efficient.
 */
@SuppressWarnings("deprecation")
public abstract class BlockData implements Cloneable   {
    private static final boolean USE_MODERN = VersionUtil.isMcVersionAtLeast("1.13.0");

    /**
     * Instantiates the correct implementation depending on the version
     * @param block The block to get the data of
     * @return The correct implementation
     */
    @NotNull
    public static BlockData create(@NotNull Block block) {
        return USE_MODERN ? new ModernBlockData(block) : new LegacyBlockData(block);
    }

    /**
     * Loads the block data from the combined ID as fetched with {@link BlockData#getCombinedId()}
     * @param combinedId The ID to load from
     * @return The loaded instance.
     */
    @NotNull
    public static BlockData create(int combinedId) {
        return USE_MODERN ? new ModernBlockData(combinedId) : new LegacyBlockData(combinedId);
    }

    /**
     * Gets the type of this data
     * @return The type of the underlying data
     */
    @NotNull
    public abstract Material getType();

    /**
     * Converts this instance to the ProtocolLib data wrapper.
     * NOTE: This isn't particularly efficient
     * @return The ProtocolLib {@link WrappedBlockData}.
     */
    @NotNull public abstract WrappedBlockData toProtocolLib();

    /**
     * Gets the underlying {@link org.bukkit.block.data.BlockData} or {@link org.bukkit.material.MaterialData}
     * @return The underlying data
     */
    @NotNull public abstract Object getUnderlying();

    /**
     * Converts this instance into a combined integer, used for serialization.
     * @return The combined block ID
     */
    public abstract int getCombinedId();
}
