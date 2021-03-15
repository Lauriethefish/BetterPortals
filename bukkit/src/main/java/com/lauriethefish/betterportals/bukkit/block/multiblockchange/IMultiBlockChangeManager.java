package com.lauriethefish.betterportals.bukkit.block.multiblockchange;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Abstracts the differences in sending multi block change packets in different versions.
 */
public interface IMultiBlockChangeManager {
    /**
     * Adds a new change to the map
     * @param position Position relative to the world that the player is in
     * @param newData The new {@link WrappedBlockData} that the player will see.
     */
    void addChange(Vector position, WrappedBlockData newData);

    /**
     * Sends all queued changes.
     * Does <i>not</i> clear changes.
     */
    void sendChanges();

    interface Factory {
        IMultiBlockChangeManager create(Player player);
    }
}
