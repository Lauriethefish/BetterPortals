package com.lauriethefish.betterportals.network;

import java.util.UUID;

import lombok.Getter;

// Sent by a server to get the proxy to teleport the player to the destination of a portal
// This is also sent to destServer in order to correctly position the player once they join
public class TeleportPlayerRequest implements Request   {
    private static final long serialVersionUID = 1113165482649027108L;
    
    @Getter private UUID playerId;

    @Getter private String destServer;
    @Getter private String destWorldName;

    @Getter private double destX;
    @Getter private double destY;
    @Getter private double destZ;

    @Getter private float destYaw;
    @Getter private float destPitch;

    public TeleportPlayerRequest(UUID playerId, String destServer, String destWorldName, double destX, double destY, double destZ, float destYaw, float destPitch) {
        this.playerId = playerId;
        this.destServer = destServer;
        this.destWorldName = destWorldName;
        this.destX = destX;
        this.destY = destY;
        this.destZ = destZ;
        this.destYaw = destYaw;
        this.destPitch = destPitch;
    }
}
