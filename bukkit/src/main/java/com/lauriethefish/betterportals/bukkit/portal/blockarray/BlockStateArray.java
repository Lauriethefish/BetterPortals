package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.ReflectUtils;
import org.bukkit.Location;

public interface BlockStateArray {
    static BlockStateArray createInstance(BetterPortals pl) {
        if(ReflectUtils.isLegacy) {
            return new LegacyBlockStateArray(pl);
        }   else    {
            return new ModernBlockStateArray(pl);
        }
    }

    boolean initialise();
    boolean update(Location loc, int index);
    boolean isOccluding(int index);
    SerializableBlockData getBlockData(int index);
}
