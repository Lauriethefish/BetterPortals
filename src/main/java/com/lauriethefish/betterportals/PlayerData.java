package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// This class stores information about the player, required by the plugin
// In this instance it only stores one value, the position of the last portal they used

public class PlayerData {
    // Reference to the plugin
    private BetterPortals pl;

    // Reference to the player
    public Player player;
    // The destination of the last portal that they used
    // This is used as a form of cooldown for portals
    // If it is null then they were not in a portal during the last tick
    public Location lastUsedPortal = null;

    // The last portal that had the portal effect active.
    // If this changes, then the ghost blocks sent to the player are reset to avoid phantom blocks breaking the illusion
    public PortalPos lastActivePortal = null;
    // Store the surrouding ghost blocks that have been sent to the player so that they can be reverted later.
    public BlockConfig[] surroundingPortalBlockStates;

    // Player's velocity as measured by the PlayerMoveEvent, this gives us a more accurate velocity
    public Vector velocity = new Vector(0.0, 0.0, 0.0);

    public PlayerData(BetterPortals pl, Player player) {
        this.player = player;
        this.pl = pl;

        surroundingPortalBlockStates = new BlockConfig[pl.config.totalArrayLength];
    }

    // Resets all of the ghost block updates that have been set to the player
    // This also has the effect of changing surroundingPortalBlockStates to be all null
    @SuppressWarnings("deprecation")
    public void resetSurroundingBlockStates()   {
        // Loop through all of the potential ghost blocks
        for(BlockConfig state : surroundingPortalBlockStates)   {
            // If a block has not been changed, skip it
            if(state == null)   {continue;}
            // Check if the block is in the right world
            if(state.location.getWorld() != player.getWorld())  {break;}

            // Check if the block is in an incorrect state,
            // if it is, set it back to what it should be
            Block block = state.location.getBlock();
            if(block.getType() != state.material)   {
                player.sendBlockChange(state.location, block.getType(), block.getData());
            }
        }

        // Reset surroundingPortalBlockStates to be all null
        surroundingPortalBlockStates = new BlockConfig[pl.config.totalArrayLength];
    }
}