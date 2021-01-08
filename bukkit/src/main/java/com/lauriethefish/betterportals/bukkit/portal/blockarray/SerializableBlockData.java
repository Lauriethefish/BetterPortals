package com.lauriethefish.betterportals.bukkit.portal.blockarray;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import lombok.Getter;
import org.bukkit.block.data.BlockData;

// Class for storing a material and data byte pair for easy serialization and passing.
public class SerializableBlockData implements Serializable {
    private static final long serialVersionUID = 8226225419982812641L;
    
    @Getter private Object nmsData;
    
    public SerializableBlockData(BlockState state) {
        this.nmsData = ReflectUtils.getNMSData(state); 
    }

    public SerializableBlockData(BlockData blockData) {
        this.nmsData = ReflectUtils.getNMSData(blockData);
    }

    public SerializableBlockData(Block block) {
        this(block.getState());
    }

    public SerializableBlockData(Material material, byte data) {
        this.nmsData = ReflectUtils.getNMSData(material, data);
    }

    public SerializableBlockData(Material material) {
        this.nmsData = ReflectUtils.getNMSData(material);
    }

    // Use the combined block ID for serialization.
    private void writeObject(ObjectOutputStream stream) throws IOException  {
        stream.writeInt(ReflectUtils.NMSDataToCombinedId(nmsData));
    }

    private void readObject(ObjectInputStream stream) throws IOException {
        nmsData = ReflectUtils.combinedIdToNMSData(stream.readInt());
    }
}
