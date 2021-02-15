package com.lauriethefish.betterportals.bukkit.player.selection;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.bukkit.math.MathUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PortalSelection implements IPortalSelection    {
    private Logger logger;

    // Not axis aligned
    private Location enteredPosA;
    private Location enteredPosB;

    // Axis aligned after call to alignCoordinates
    private Location posA;
    private Location posB;

    private Location portalLocation;
    private PortalDirection portalDirection;
    @Getter private Vector portalSize;

    @Getter private boolean isValid;

    @Inject
    public PortalSelection(Logger logger) {
        this.logger = logger;
    }

    // Attempt to see if this selection is valid after selecting each position
    @Override
    public void setPositionA(@NotNull Location posA) {
        this.enteredPosA = posA;
        this.isValid = calculatePortalPosition();
    }

    @Override
    public void setPositionB(@NotNull Location posB) {
        this.enteredPosB = posB;
        this.isValid = calculatePortalPosition();
    }

    @Override
    public @Nullable PortalPosition getPortalPosition() {
        if(portalLocation == null || portalDirection == null) {return null;}
        return new PortalPosition(portalLocation, portalDirection);
    }

    @Override
    public void invertDirection() {
        portalDirection = portalDirection.getOpposite();
    }

    private boolean calculatePortalPosition() {
        if(enteredPosA == null || enteredPosB == null) {return false;}

        alignCoordinates();

        if(posA.getWorld() != posB.getWorld()) {
            logger.fine("Portal selection was two different worlds, aborting");
            return false;
        }

        findDirection();
        if(portalDirection == null) {
            logger.fine("Portal selection was not in line with a valid portal plane, aborting");
            return false;
        }

        // Find the size, aborting if it is too small
        portalSize = findSize();
        if(!(portalSize.getX() >= 1 && portalSize.getY() >= 1)) {
            logger.fine("Portal size (%s), was not large enough, aborting", portalSize);
            return false;
        }

        findPortalLocation();

        logger.fine("Successfully found selected portal position at location %s with direction %s and size %s", portalLocation, portalDirection, portalSize);
        return true;
    }

    // Direction is found based on which axis the coordinates are in line on
    // If they aren't in line on any axis, then the selection is invalid
    private void findDirection() {
        if(posA.getZ() == posB.getZ()) {
            portalDirection = PortalDirection.NORTH;
        }   else if(posA.getX() == posB.getX())    {
            portalDirection = PortalDirection.EAST;
        }   else if(posA.getY() == posB.getY()) {
            portalDirection = PortalDirection.UP;
        }   else {
            portalDirection = null;
        }
    }

    private Vector findSize() {
        portalSize = portalDirection.swapVector(posB.clone().subtract(posA).toVector());
        return portalSize.subtract(new Vector(1.0, 1.0, 0.0)); // Subtract it by 1 so it is the size of the gateway, not the frame
    }

    // Finds the correct PortalPosition at the exact center of the selected points
    private void findPortalLocation() {
        portalLocation = posA.clone().add(posB)
                            .add(new Vector(1.0, 1.0, 1.0))
                            .multiply(0.5); // Average the two positions to find the exact center
    }

    private void alignCoordinates() {
        posA = MathUtil.min(enteredPosA, enteredPosB); // Position A should always have the lowest coordinates
        posB = MathUtil.max(enteredPosA, enteredPosB); // Position B should always have the highest
    }

    // Java clone, why are you so dumb...
    @Override
    public PortalSelection clone() {
        try {
            PortalSelection clone = (PortalSelection) super.clone();
            clone.logger = this.logger;
            clone.posA = this.posA;
            clone.posB = this.posB;
            clone.portalLocation = portalLocation;
            clone.portalDirection = portalDirection;
            clone.portalSize = this.portalSize;
            clone.isValid = this.isValid;
            return clone;
        }   catch(CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
