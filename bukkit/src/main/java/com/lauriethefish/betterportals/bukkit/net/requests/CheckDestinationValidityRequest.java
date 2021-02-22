package com.lauriethefish.betterportals.bukkit.net.requests;

import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Before we active a cross-server portal, we need to check that the destination server and world exist and is on the correct game version.
 * Cross-version portals are not allowed at the moment, since implementing block conversion would be too difficult.
 *
 * This request has no result, but will return a response with a request exception if the destination is not valid.
 */
@Getter
@Setter
public class CheckDestinationValidityRequest extends Request {
    private String destinationWorldName;
    private UUID destinationWorldId; // Used if a world with ID worldId cannot be found
    private String originGameVersion;
}
