package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import java.io.Serializable;
import java.lang.reflect.Method;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

// Class for storing a material and data byte pair for easy serialization and passing.
public class SerializableBlockData implements Serializable {
    private static final long serialVersionUID = 8226225419982812641L;
    
    private final Material material;
    private final byte data;
    private transient Object nmsDataCache = null;
    
    @SuppressWarnings("deprecation")
    public SerializableBlockData(BlockState state) {
        this.material = state.getType();
        this.data = state.getRawData();
    }

    @SuppressWarnings("deprecation")
    public SerializableBlockData(Block block) {
        this.material = block.getType();
        this.data = block.getData();
    }

    public SerializableBlockData(Material material, byte data) {
        this.material = material;
        this.data = data;
    }

    public SerializableBlockData(Material material) {
        this(material, (byte) 0);
    }

    public Object getNMSData() {
        if(nmsDataCache == null) {nmsDataCache = getNMSData(material, data);} // Find the data if it hasn't been found already

        return nmsDataCache;
    }

    private static final Method getNMSDataMethod = ReflectUtils.isLegacy ? 
    // In legacy versions we use the combined id for getting the IBlockData Object
    ReflectUtils.findMethod(ReflectUtils.getMcClass("Block"), "getByCombinedId", new Class[] { int.class } )
    // Otherwise there's a way to get it from the type and data value
    : ReflectUtils.findMethod(ReflectUtils.getBukkitClass("util.CraftMagicNumbers"), "getBlock", new Class[] { Material.class, byte.class });

    // Finds the NMS IBlockData object from a combined block ID and data
    @SuppressWarnings("deprecation")
    private static Object getNMSData(Material material, byte data) {
        try {
            if(ReflectUtils.isLegacy) {
                // Calculate the combined block ID for legacy versions
                int combinedID = material.getId() + (data << 12);
                return getNMSDataMethod.invoke(null, combinedID);
            }   else    {
                // Otherwise, just pass the material and data
                return getNMSDataMethod.invoke(null, material, data);
            }
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
