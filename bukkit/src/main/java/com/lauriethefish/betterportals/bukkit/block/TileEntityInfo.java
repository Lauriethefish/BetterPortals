package com.lauriethefish.betterportals.bukkit.block;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import org.bukkit.block.TileState;

public class TileEntityInfo {
    private NbtCompound nbtCompound;
    private PacketContainer updatePacket;

    public TileEntityInfo(TileState tileState) {

    }
}
