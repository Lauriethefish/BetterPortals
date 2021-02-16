package com.lauriethefish.betterportals.bukkit.util;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

/**
 * The materials/data of certain blocks that we need change based on version.
 * This small wrapper allows us to avoid the issues.
 */
public class MaterialUtil {
    public static final Material PORTAL_MATERIAL;
    public static final WrappedBlockData PORTAL_EDGE_DATA;
    private static final Set<Material> AIR_MATERIALS = new HashSet<>();

    static {
        boolean isLegacy = !VersionUtil.isMcVersionAtLeast("1.13.0");
        PORTAL_MATERIAL = isLegacy ? Material.valueOf("PORTAL") : Material.valueOf("NETHER_PORTAL");

        if(isLegacy) {
            PORTAL_EDGE_DATA = WrappedBlockData.createData(Material.valueOf("CONCRETE"), (byte) 15);
        }   else    {
            PORTAL_EDGE_DATA = WrappedBlockData.createData(Material.valueOf("BLACK_CONCRETE"));
        }

        AIR_MATERIALS.add(Material.AIR);
        if(!isLegacy) {
            AIR_MATERIALS.add(Material.valueOf("CAVE_AIR"));
            AIR_MATERIALS.add(Material.valueOf("VOID_AIR"));
        }
    }

    /**
     * Checks if <code>material</code> is an air material.
     * E.g. AIR, CAVE_AIR or VOID_AIR.
     * This is here because {@link Material#isAir()} isn't available on 1.12.
     * @param material The material to check to see if it's air
     * @return Whether or not the material is air
     */
    public static boolean isAir(Material material) {
        return AIR_MATERIALS.contains(material);
    }
}
