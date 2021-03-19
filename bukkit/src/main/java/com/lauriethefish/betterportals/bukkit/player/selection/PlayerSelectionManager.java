package com.lauriethefish.betterportals.bukkit.player.selection;


import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandException;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.net.requests.GetSelectionRequest;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class PlayerSelectionManager implements IPlayerSelectionManager  {
    private final MessageConfig messageConfig;
    @Getter private final IPortalSelection currentlySelecting;
    private final IPortal.Factory portalFactory;
    private final IPortalManager portalManager;

    @Getter private IPortalSelection originSelection;
    @Getter private IPortalSelection destSelection;

    @Getter @Setter private GetSelectionRequest.ExternalSelectionInfo externalSelection;

    @Inject
    public PlayerSelectionManager(MessageConfig messageConfig, IPortalSelection currentlySelecting, IPortal.Factory portalFactory, IPortalManager portalManager) {
        this.messageConfig = messageConfig;
        this.currentlySelecting = currentlySelecting;
        this.portalFactory = portalFactory;
        this.portalManager = portalManager;

    }

    @Override
    public void trySelectOrigin() throws CommandException   {
        verifyCurrentSelection();
        originSelection = currentlySelecting.clone();
    }

    @Override
    public void trySelectDestination() throws CommandException  {
        verifyCurrentSelection();
        destSelection = currentlySelecting.clone();
    }

    private void verifyCurrentSelection() throws CommandException {
        if(!currentlySelecting.isValid()) {
            throw new CommandException(messageConfig.getErrorMessage("invalidSelection"));
        }
    }

    @Override
    public void tryCreateFromSelection(Player player, boolean twoWay, boolean invert) throws CommandException {
        if(originSelection == null || destSelection == null) {
            throw new CommandException(messageConfig.getErrorMessage("mustSelectBothSides"));
        }

        // Portals must have the same origin and destination size
        if(!originSelection.getPortalSize().equals(destSelection.getPortalSize())) {
            throw new CommandException(messageConfig.getErrorMessage("differentSizes"));
        }

        // This could invert either the destination or origin selection - there's no difference
        if(invert) {
            destSelection.invertDirection();
        }

        IPortal portal = portalFactory.create(originSelection.getPortalPosition(), destSelection.getPortalPosition(),
                                                            originSelection.getPortalSize(), true, UUID.randomUUID(), player.getUniqueId(), null);
        portalManager.registerPortal(portal);

        // Add another portal going back if we're in two way mode
        if(twoWay) {
            IPortal reversePortal = portalFactory.create(destSelection.getPortalPosition(), originSelection.getPortalPosition(),
                    originSelection.getPortalSize(), true, UUID.randomUUID(), player.getUniqueId(), null);
            portalManager.registerPortal(reversePortal);
        }
        originSelection = null;
        destSelection = null;
    }

    @Override
    public void tryCreateFromExternalSelection(Player player, boolean invert) throws CommandException {
        if(originSelection == null || externalSelection == null) {
            throw new CommandException(messageConfig.getErrorMessage("mustSelectBothSides"));
        }

        Vector externalSize = new Vector(externalSelection.getSizeX(), externalSelection.getSizeY(), 0.0);

        if(invert) {
            originSelection.invertDirection();
        }

        if(!originSelection.getPortalSize().equals(externalSize)) {
            throw new CommandException(messageConfig.getErrorMessage("differentSizes"));
        }

        // Currently you have to manually create a portal in the other direction
        IPortal portal = portalFactory.create(originSelection.getPortalPosition(), externalSelection.getPosition(),
                    originSelection.getPortalSize(), true, UUID.randomUUID(), player.getUniqueId(), null);
        portalManager.registerPortal(portal);
    }
}
