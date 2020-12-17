package com.lauriethefish.betterportals.bukkit;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.lauriethefish.betterportals.bukkit.portal.blockarray.SerializableBlockData;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import lombok.Getter;

// Stores data about a block surrounding the portal that is in the HashMap of viewable blocks
public class BlockRaycastData implements Serializable   {
    private static final long serialVersionUID = -198098468444437819L;

    private SerializableBlockData edgeData = ReflectUtils.isLegacy ? new SerializableBlockData(Material.valueOf("CONCRETE"), (byte) 15)
                                                                   : new SerializableBlockData(Material.valueOf("BLACK_CONCRETE"), (byte) 0);

    @Getter private Vector originVec;

    // Store the material and legacy data of the origin/destination material, since these can be easily serialized
    @Getter private SerializableBlockData originData;
    @Getter private SerializableBlockData destData;

    public BlockRaycastData(Vector originPos, SerializableBlockData originData, SerializableBlockData destData, boolean edge) {
        this.originVec = originPos;
        this.originData = originData;
        // Use either the edge or actual data
        if(edge) {
            this.destData = edgeData;
        }   else    {
            this.destData = destData;
        }
    }

    // Used when a block state only changes at the origin and not the destination
    public void changeOriginData(SerializableBlockData newData) {
        originData = newData;
    }

    // Manually implement readObject and writeObject for efficiency and because Vector isn't serializable
    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
        originVec = new Vector(inputStream.readDouble(), inputStream.readDouble(), inputStream.readDouble());
        destData = (SerializableBlockData) inputStream.readObject();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.writeDouble(originVec.getX());
        outputStream.writeDouble(originVec.getY());
        outputStream.writeDouble(originVec.getZ());
        outputStream.writeObject(destData);
    }
}