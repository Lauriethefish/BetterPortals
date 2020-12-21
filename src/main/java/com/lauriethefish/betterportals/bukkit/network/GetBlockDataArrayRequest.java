package com.lauriethefish.betterportals.bukkit.network;

import java.util.Objects;

import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.portal.PortalTransformations;
import com.lauriethefish.betterportals.network.Request;

import lombok.Getter;

// Used to fetch the array of block data from another server for cross-server portals
public class GetBlockDataArrayRequest implements Request    {
    private static final long serialVersionUID = 933279153991293225L;
    
    @Getter private PortalPosition originPos;
    @Getter private PortalPosition destPos;
    private transient PortalTransformations transformations = null;

    public GetBlockDataArrayRequest(PortalPosition originPos, PortalPosition destPos) {
        this.originPos = originPos;
        this.destPos = destPos;
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
        if (!(o instanceof GetBlockDataArrayRequest)) {
            return false;
        }
        GetBlockDataArrayRequest getBlockDataArrayRequest = (GetBlockDataArrayRequest) o;
        return Objects.equals(originPos, getBlockDataArrayRequest.originPos) && Objects.equals(destPos, getBlockDataArrayRequest.destPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originPos, destPos);
    }
}
