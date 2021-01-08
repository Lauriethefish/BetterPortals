package com.lauriethefish.betterportals.bukkit.network;

import java.util.Objects;

import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.portal.PortalTransformations;
import com.lauriethefish.betterportals.network.Request;

import lombok.Getter;

// Used to fetch the array (or delete it) of block data from another server for cross-server portals
public class BlockDataArrayRequest implements Request    {
    private static final long serialVersionUID = 933279153991293225L;
    // Chooses whether to fetch the array, or delete it incase of deactivation
    public enum Mode {
        GET_OR_UPDATE,
        CLEAR
    }
    
    @Getter private final PortalPosition originPos;
    @Getter private final PortalPosition destPos;
    @Getter private final Mode mode;
    private transient PortalTransformations transformations = null;

    public BlockDataArrayRequest(PortalPosition originPos, PortalPosition destPos, Mode mode) {
        this.originPos = originPos;
        this.destPos = destPos;
        this.mode = mode;
    }

    // Constructs a get request more easily
    public BlockDataArrayRequest(PortalPosition originPos, PortalPosition destPos) {
        this(originPos, destPos, Mode.GET_OR_UPDATE);
    }

    // Creates the PortalTransformations if they haven't been made already, then returns them
    public PortalTransformations getTransformations() {
        if(transformations == null) {transformations = new PortalTransformations(originPos, destPos);}
        return transformations;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BlockDataArrayRequest)) {
            return false;
        }
        BlockDataArrayRequest getBlockDataArrayRequest = (BlockDataArrayRequest) o;
        return Objects.equals(originPos, getBlockDataArrayRequest.originPos) && Objects.equals(destPos, getBlockDataArrayRequest.destPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originPos, destPos);
    }
}
