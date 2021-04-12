package com.lauriethefish.betterportals.bukkit.net.requests;

import com.lauriethefish.betterportals.bukkit.player.selection.IPortalSelection;
import com.lauriethefish.betterportals.api.PortalPosition;
import com.lauriethefish.betterportals.shared.net.requests.Request;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Sent by servers whenever the player joins so that they can find the destination position of a cross-server portal.
 *
 * The response to this is a single {@link ExternalSelectionInfo}, or null if there was no selection.
 */
@Getter
@Setter
public class GetSelectionRequest extends Request {
    private static final long serialVersionUID = 1L;

    @Getter
    public static class ExternalSelectionInfo implements Serializable {
        private final PortalPosition position;
        private final int sizeX;
        private final int sizeY;

        public ExternalSelectionInfo(IPortalSelection portalSelection) {
            assert portalSelection.isValid();

            this.position = portalSelection.getPortalPosition();
            this.sizeX = portalSelection.getPortalSize().getBlockX();
            this.sizeY = portalSelection.getPortalSize().getBlockY();
        }
    }

    private UUID playerId;
}
