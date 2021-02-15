package com.lauriethefish.betterportals.bukkit.player.view.block;

import com.lauriethefish.betterportals.bukkit.block.ViewableBlockInfo;
import org.bukkit.util.Vector;

// Implementors of this class must make sure that it is thread safe
// Manages the current blocks that the player can see
public interface IPlayerBlockStates {
    // Should be called when moving a short distance in the same dimension
    // Actually changes the blocks back to what they should be
    void resetAndUpdate();

    // Note: these methods just update the array, they won't actually send any packets
    // Returns if the block was not viewable last tick, AKA whether or not an update packet is needed
    boolean setViewable(Vector position, ViewableBlockInfo block);

    // Returns if the block was viewable last tick, AKA whether or not an update packet is needed
    boolean setNonViewable(Vector position, ViewableBlockInfo block);
}
