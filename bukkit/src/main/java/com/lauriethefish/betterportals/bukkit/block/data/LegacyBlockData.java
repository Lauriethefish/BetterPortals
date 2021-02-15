package com.lauriethefish.betterportals.bukkit.block.data;

import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

@SuppressWarnings("deprecation")
public class LegacyBlockData extends BlockData  {
    private static final Method GET_FROM_ID;
    static {
        GET_FROM_ID = ReflectionUtil.findMethod(Material.class, "getMaterial", new Class[]{int.class});
    }

    private final MaterialData underlying;


    public LegacyBlockData(@NotNull Block block) {
        this.underlying = new MaterialData(block.getType(), block.getData());
    }

    public LegacyBlockData(@NotNull MaterialData underlying) {
        this.underlying = underlying;
    }

    public LegacyBlockData(int combinedId) {
        byte data = (byte) (combinedId >> 12);
        int id = combinedId & 0x00FFFFFF;

        this.underlying = new MaterialData(getMaterial(id), data);
        throw new RuntimeException("unimplemented");
    }

    private static Material getMaterial(int id)  {
        try {
            return (Material) GET_FROM_ID.invoke(id);
        }   catch(ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public @NotNull Material getType() {
        return underlying.getItemType();
    }

    @Override
    public @NotNull WrappedBlockData toProtocolLib() {
        return WrappedBlockData.createData(getType(), underlying.getData());
    }

    @Override
    public @NotNull Object getUnderlying() {
        return underlying;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCombinedId() {
        return underlying.getItemType().getId() + (underlying.getData() << 12);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof LegacyBlockData)) {return false;}
        return ((LegacyBlockData) obj).underlying.equals(underlying);
    }

    @Override
    public LegacyBlockData clone() {
        try {
            return (LegacyBlockData) super.clone();
        }   catch(CloneNotSupportedException ex) {
            throw new Error(ex);
        }
    }
}
