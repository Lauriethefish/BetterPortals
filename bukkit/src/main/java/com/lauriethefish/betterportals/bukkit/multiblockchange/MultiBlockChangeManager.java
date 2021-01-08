package com.lauriethefish.betterportals.bukkit.multiblockchange;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Interface to represent both implementations of MultiblockChangeManager
public interface MultiBlockChangeManager {
    // Instantiates the correct implementation of MultiBlockChangeManager depending on the underlying server
    static MultiBlockChangeManager createInstance(Player player)  {
        return (MultiBlockChangeManager) ReflectUtils.newInstance(ReflectUtils.multiBlockChangeImpl,
                                                new Class[]{Player.class}, new Object[]{player});
    }

    void addChange(Vector location, Object newType);
    default void addChange(Location location, Object newType) {
        addChange(location.toVector(), newType);
    }

    void sendChanges();
}