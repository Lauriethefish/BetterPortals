package com.lauriethefish.betterportals.bukkit.block.data;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.lauriethefish.betterportals.bukkit.util.nms.BlockDataUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class ModernBlockData extends BlockData  {
    @Getter private final org.bukkit.block.data.BlockData underlying;

    public ModernBlockData(Block block) {
        this.underlying = block.getBlockData();
    }

    public ModernBlockData(int combinedId) {
        this.underlying = BlockDataUtil.getByCombinedId(combinedId);
    }

    public ModernBlockData(org.bukkit.block.data.BlockData underlying) {
        this.underlying = underlying;
    }

    @Override
    public @NotNull Material getType() {
        return underlying.getMaterial();
    }

    @Override
    public @NotNull WrappedBlockData toProtocolLib() {
        return WrappedBlockData.createData(underlying);
    }

    @Override
    public int getCombinedId() {
        return BlockDataUtil.getCombinedId(underlying);
    }

    @Override
    public ModernBlockData clone() {
        try {
            return (ModernBlockData) super.clone();
        }   catch(CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ModernBlockData)) {return false;}
        return ((ModernBlockData) obj).underlying.equals(underlying);
    }
}
