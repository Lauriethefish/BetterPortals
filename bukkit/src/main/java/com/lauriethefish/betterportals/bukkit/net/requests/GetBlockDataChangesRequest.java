package com.lauriethefish.betterportals.bukkit.net.requests;

import com.lauriethefish.betterportals.api.IntVector;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Used to request a set of block data changes from the destination of a cross-server portal.
 * This will send all of the blocks within the portal area the first time it's sent with a particular {@link GetBlockDataChangesRequest#changeSetId}.
 */
@Getter
@Setter
public class GetBlockDataChangesRequest extends Request {
    private static final long serialVersionUID = 1L;

    private UUID changeSetId;
    private IntVector position;
    private Matrix rotateOriginToDest;
    private UUID worldId;
    private String worldName; // Used if a world with ID worldId cannot be found

    /**
     * The external server might have a different render distance, so we need to send this in the request
     */
    private int xAndZRadius;
    private int yRadius;
}
