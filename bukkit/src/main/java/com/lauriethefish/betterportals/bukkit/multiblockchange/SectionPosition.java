package com.lauriethefish.betterportals.bukkit.multiblockchange;

import java.util.Objects;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionPosition {
    public int x;
    public int y;
    public int z;
    public SectionPosition(Vector location)    {
        x = location.getBlockX() >> 4;
        y = location.getBlockY() >> 4;
        z = location.getBlockZ() >> 4;
    }

    // Make an NMS SectionPosition from this object
    public Object toNMS()   {
        return ReflectUtils.newInstance("SectionPosition", new Class[]{int.class, int.class, int.class}, new Object[]{x, y, z});
    }

    // Automatically generated
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SectionPosition)) {
            return false;
        }
        SectionPosition sectionPosition = (SectionPosition) o;
        return x == sectionPosition.x && y == sectionPosition.y && z == sectionPosition.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}