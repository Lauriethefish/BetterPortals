package com.lauriethefish.betterportals.bukkit.block;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import lombok.Getter;
import org.bukkit.block.data.BlockData;

// Represents each block that is not fully obscured and will be rendered through the portal
// A collection of these is built by IViewableBlockArray
// NOTE: This may be read on other threads
public class ViewableBlockInfo {
    @Getter private BlockData baseOriginData;
    @Getter private BlockData baseDestData;
    @Getter private WrappedBlockData originData;
    @Getter private WrappedBlockData destData;

    public ViewableBlockInfo(BlockData originData, BlockData destData) {
        this.baseOriginData = originData;
        this.baseDestData = destData;
        this.originData = WrappedBlockData.createData(originData);
    }

    // Test constructor
    public ViewableBlockInfo() { }

    public void setOriginData(BlockData originData) {
        this.baseOriginData = originData;
        this.originData = WrappedBlockData.createData(originData);
    }

    public void setRenderedDestData(WrappedBlockData destData) {
        this.destData = destData;
    }
}
