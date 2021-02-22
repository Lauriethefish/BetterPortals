package com.lauriethefish.betterportals.bukkit.util.nms;

import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public class BlockDataUtil {
    private static final Method GET_HANDLE;
    private static final Method GET_COMBINED_ID;
    private static final Method GET_FROM_COMBINED_ID;
    private static final Method FROM_HANDLE;

    static {
        Class<?> nmsBlock = MinecraftReflectionUtil.findNMSClass("Block");
        Class<?> craftBlockData = MinecraftReflectionUtil.findCraftBukkitClass("block.data.CraftBlockData");
        Class<?> nmsBlockData = MinecraftReflectionUtil.findNMSClass("IBlockData");

        GET_HANDLE = ReflectionUtil.findMethod(craftBlockData, "getState");
        GET_COMBINED_ID = ReflectionUtil.findMethod(nmsBlock, "getCombinedId", new Class[]{nmsBlockData});

        GET_FROM_COMBINED_ID = ReflectionUtil.findMethod(nmsBlock, "getByCombinedId", new Class[]{int.class});
        FROM_HANDLE = ReflectionUtil.findMethod(craftBlockData, "fromData", new Class[]{nmsBlockData});
    }

    /**
     * Converts <code>blockData</code> into a combined ID that stores all info about the block.
     * @param blockData The data to convert
     * @return The combined ID of the data
     */
    public static int getCombinedId(@NotNull BlockData blockData) {
        try {
            Object nmsData = GET_HANDLE.invoke(blockData);
            return (int) GET_COMBINED_ID.invoke(null, nmsData);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Converts <code>combinedId</code> as created in {@link BlockDataUtil#getCombinedId(BlockData)} back into a {@link BlockData}.
     * @param combinedId The ID to convert
     * @return The bukkit block data
     */
    public static BlockData getByCombinedId(int combinedId) {
        try {
            Object nmsData = GET_FROM_COMBINED_ID.invoke(null, combinedId);
            return (BlockData) FROM_HANDLE.invoke(null, nmsData);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
