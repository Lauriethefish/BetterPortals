package com.lauriethefish.betterportals.bukkit.block;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.BlockState;

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
        this.originData = originData.toProtocolLib();
    }

    // Test constructor
    public ViewableBlockInfo() { }

    public void setOriginData(BlockData originData) {
        this.baseOriginData = originData;
        this.originData = originData.toProtocolLib();
    }

    public void setRenderedDestData(WrappedBlockData destData) {
        this.destData = destData;
    }
}
