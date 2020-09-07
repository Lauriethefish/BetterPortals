package com.lauriethefish.betterportals.multiblockchange;

import com.lauriethefish.betterportals.ReflectUtils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

// Interface to represent both implementations of MultiblockChangeManager
public interface MultiBlockChangeManager {
    // Instatiates the correct implementation depending on the version
    public static MultiBlockChangeManager createInstance(Player player)  {
        if(ReflectUtils.useNewMultiBlockChangeImpl) {
            return new MultiBlockChangeManager_1_16_2(player);
        }   else    {
            return new MultiBlockChangeManager_Old(player);
        }
    }

    public void addChange(Vector location, Object newType);
    public default void addChange(Location location, Object newType) {
        addChange(location.toVector(), newType);
    }

    public void sendChanges();
}