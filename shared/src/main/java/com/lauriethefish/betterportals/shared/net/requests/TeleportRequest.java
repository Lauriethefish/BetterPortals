package com.lauriethefish.betterportals.shared.net.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class TeleportRequest extends Request    {
    private static final long serialVersionUID = 1L;

    private UUID playerId;
    private String destServer;
    private UUID destWorldId;
    private String destWorldName;

    private double destX;
    private double destY;
    private double destZ;

    private float destPitch;
    private float destYaw;

    private boolean flying;
    private boolean gliding;

    private double destVelX;
    private double destVelY;
    private double destVelZ;
}
