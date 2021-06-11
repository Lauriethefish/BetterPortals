package com.lauriethefish.betterportals.bukkit.block.fetch;

import com.lauriethefish.betterportals.api.IntVector;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

public interface IBlockDataFetcher {
    /**
     * Updates the currently fetched data.
     */
    void update();

    /**
     * @return Whether the data has been fetched and can be read with {@link IBlockDataFetcher#getData(IntVector)}.
     */
    boolean isReady();

    /**
     * Reads the data at <code>position</code>.
     * @param position The position to get the data at. The destination world is implied
     * @return The block data at that position
     */
    @NotNull BlockData getData(@NotNull IntVector position);
}
