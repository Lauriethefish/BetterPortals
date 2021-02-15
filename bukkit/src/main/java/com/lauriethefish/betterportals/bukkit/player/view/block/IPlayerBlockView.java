package com.lauriethefish.betterportals.bukkit.player.view.block;

public interface IPlayerBlockView {
    void update(boolean refresh); // If refresh is true, all blocks will be resent to the player regardless of if they've already seen the changes
    void onDeactivate(boolean shouldResetStates);
}
