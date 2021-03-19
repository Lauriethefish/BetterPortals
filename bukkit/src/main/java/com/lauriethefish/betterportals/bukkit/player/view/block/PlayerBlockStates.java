package com.lauriethefish.betterportals.bukkit.player.view.block;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.block.ViewableBlockInfo;
import com.lauriethefish.betterportals.bukkit.block.multiblockchange.IMultiBlockChangeManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

public class PlayerBlockStates implements IPlayerBlockStates {
    private final Player player;
    private final IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory;
    private final Logger logger;

    private final Map<Vector, ViewableBlockInfo> viewedStates = new HashMap<>();

    @Inject
    public PlayerBlockStates(@Assisted Player player, IMultiBlockChangeManager.Factory multiBlockChangeManagerFactory, Logger logger) {
        this.player = player;
        this.multiBlockChangeManagerFactory = multiBlockChangeManagerFactory;
        this.logger = logger;
    }

    @Override
    public void resetAndUpdate() {
        // Use a MultiBlockChangeManager to actually send the changes
        IMultiBlockChangeManager multiBlockChangeManager = multiBlockChangeManagerFactory.create(player);

        logger.finer("Resetting %d blocks", viewedStates.size());
        for(Map.Entry<Vector, ViewableBlockInfo> entry : viewedStates.entrySet()) {
            multiBlockChangeManager.addChange(entry.getKey(), entry.getValue().getOriginData());
        }
        multiBlockChangeManager.sendChanges();

        viewedStates.clear();
    }

    @Override
    public boolean setViewable(Vector position, ViewableBlockInfo block) {
        return viewedStates.put(position, block) == null;
    }

    @Override
    public boolean setNonViewable(Vector position, ViewableBlockInfo block) {
        return viewedStates.remove(position, block);
    }
}
