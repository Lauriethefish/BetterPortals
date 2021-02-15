package com.lauriethefish.betterportals.bukkit.util;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;

/**
 * The materials/data of certain blocks that we need change based on version.
 * This small wrapper allows us to avoid the issues.
 */
public class MaterialUtil {
    public static final Material PORTAL_MATERIAL;
    public static final WrappedBlockData PORTAL_EDGE_DATA;

    static {
        boolean isLegacy = !VersionUtil.isMcVersionAtLeast("1.13.0");
        PORTAL_MATERIAL = isLegacy ? Material.valueOf("PORTAL") : Material.valueOf("NETHER_PORTAL");

        if(isLegacy) {
            PORTAL_EDGE_DATA = WrappedBlockData.createData(Material.valueOf("CONCRETE"), (byte) 15);
        }   else    {
            PORTAL_EDGE_DATA = WrappedBlockData.createData(Material.valueOf("BLACK_CONCRETE"));
        }
    }
}
