package com.lauriethefish.betterportals.multiblockchange;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Interface to represent both implementations of MultiblockChangeManager
public interface MultiBlockChangeManager {
    // Instatiates the correct implementation of MultiBlockChangeManager depending on the underlying server
    public static MultiBlockChangeManager createInstance(Player player)  {
        return (MultiBlockChangeManager) ReflectUtils.newInstance(ReflectUtils.multiBlockChangeImpl,
                                                new Class[]{Player.class}, new Object[]{player});
    }

    public void addChange(Vector location, Object newType);
    public default void addChange(Location location, Object newType) {
        addChange(location.toVector(), newType);
    }

    public void sendChanges();
}