package com.lauriethefish.betterportals.bukkit.math;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.lauriethefish.betterportals.bukkit.config.RenderConfig;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.PortalDirection;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Handles the matrices used for transforming coordinates around the origin and destination of portals.
 */
public class PortalTransformations {
    private final PortalPosition originPos;
    private final PortalPosition destPos;
    private final RenderConfig renderConfig;
    private final Vector portalSize;

    @Getter private final Matrix originToDestination; // Translates a vector from the origin of the portal to its destination
    @Getter private Matrix rotateToDestination; // Rotates a unit vector to the destination

    @Getter private final Matrix destinationToOrigin;
    @Getter private Matrix rotateToOrigin;

    private final World originWorld;
    private final World destinationWorld;

    @Inject
    public PortalTransformations(@Assisted IPortal portal, RenderConfig renderConfig) {
        this.originPos = portal.getOriginPos();
        this.destPos = portal.getDestPos();
        this.portalSize = portal.getSize();
        this.renderConfig = renderConfig;

        rotateToDestination = Matrix.makeRotation(originPos.getDirection(), destPos.getDirection());
        rotateToOrigin = Matrix.makeRotation(destPos.getDirection(), originPos.getDirection());

        // Apply the dinnerbone easter egg
        if("dinnerbone".equalsIgnoreCase(portal.getName())) {
            applyDinnerbone();
        }

        originToDestination = Matrix.makeTranslation(destPos.getVector())
                .multiply(rotateToDestination)
                .multiply(Matrix.makeTranslation(originPos.getVector().multiply(-1.0)));

        destinationToOrigin = Matrix.makeTranslation(originPos.getVector())
                .multiply(rotateToOrigin)
                .multiply(Matrix.makeTranslation(destPos.getVector().multiply(-1.0)));

        originWorld = originPos.getWorld();
        destinationWorld = destPos.getWorld();
    }

    /**
     * Flips the portal view 180 degrees. Does nothing for horizontal portals
     */
    private void applyDinnerbone() {
        PortalDirection originDirection = originPos.getDirection();
        PortalDirection destinationDirection = destPos.getDirection();

        if(originDirection.isHorizontal() || destinationDirection.isHorizontal()) {return;}
        Vector axis = destinationDirection.toVector();

        rotateToDestination = rotateToDestination.multiply(Matrix.makeRotation(axis, Math.PI));
        rotateToOrigin = rotateToOrigin.multiply(Matrix.makeRotation(axis, -Math.PI));
    }

    /**
     * Moves the location from the origin coordinate space to the destination coordinate space.
     * @param loc The location to be moved
     * @return The location in the destination coordinate space
     */
    public Location moveToDestination(Location loc) {
        Location result = originToDestination.transform(loc.toVector()).toLocation(destinationWorld);
        result.setDirection(rotateToDestination.transform(loc.getDirection()));

        return result;
    }

    /**
     * Transforms <code>vec</code> from the destination coordinate space to the origin coordinate space.
     * @param vec The vector to move
     * @return The vector in the origin coordinate space
     */
    public IntVector moveToOrigin(IntVector vec) {
        return destinationToOrigin.transform(vec);
    }

    /**
     * Transforms <code>vec</code> from the origin coordinate space to the destination coordinate space.
     * @param vec The vector to move
     * @return The vector in the destination coordinate space
     */
    public IntVector moveToDestination(IntVector vec) {
        return originToDestination.transform(vec);
    }

    /**
     * Moves the location from the destination coordinate space to the origin coordinate space.
     * @param loc The location to be moved
     * @return A new location in the origin coordinate space
     */
    public Location moveToOrigin(Location loc) {
        Location result = destinationToOrigin.transform(loc.toVector()).toLocation(originWorld);
        result.setDirection(rotateToOrigin.transform(loc.getDirection()));

        return result;
    }

    /**
     * Rotates the vector from the origin coordinate space to the destination coordinate space - no translation is performed.
     * @param vec The vector to be rotated
     * @return A new vector in the destination coordinate space
     */
    public Vector rotateToDestination(Vector vec) {
        return rotateToDestination.transform(vec);
    }

    /**
     * Rotates the vector from the destination coordinate space to the origin coordinate space - no translation is performed.
     * @param vec The vector to be rotated
     * @return A new vector in the origin coordinate space
     */
    public Vector rotateToOrigin(Vector vec) {
        return rotateToOrigin.transform(vec);
    }

    /**
     * Finds the correct {@link PlaneIntersectionChecker} for checking if blocks/entities are visible through the parent portal.
     * @param rayOrigin The origin of rays to check, usually the player's eye position
     * @return The intersection checker
     */
    public PlaneIntersectionChecker createIntersectionChecker(Vector rayOrigin) {
        Vector planeSize = portalSize.clone().multiply(0.5); // The size for the intersection checker is a radius, so we half this since it's the size of the full portal window
        planeSize = originPos.getDirection().swapVector(planeSize); // Must be on the Z and Y, or Z and X if horizontal (portal's sizes always use the XZ, but we don't want this here)
        planeSize.add(renderConfig.getCollisionBox()); // Expand the size slightly by the values in the config

        return new PlaneIntersectionChecker(
                originPos.getVector(),
                originPos.getDirection().toVector(),
                rayOrigin, // The origin pos of a portal is always exactly in the center of the plane
                planeSize // The max deviation acts as a radius, so we half this
        );
    }
}