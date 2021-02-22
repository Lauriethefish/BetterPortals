package com.lauriethefish.betterportals.shared.net.requests;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Sent by the proxy so that the client server knows where to request the externally selected portal position from.
 */
@Getter
@Setter
public class PreviousServerPutRequest extends Request   {
    private UUID playerId;
    private String previousServer;
}
