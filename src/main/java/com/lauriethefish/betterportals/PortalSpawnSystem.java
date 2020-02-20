package com.lauriethefish.betterportals;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

// Handles finding a suitable location for spawning a portal, can also deal with the actual building of the portal
public class PortalSpawnSystem {
    private BetterPortals pl;

    public static final Vector[] portalCornerLocations = {
        new Vector(0.0, 0.0, 0.0),
        new Vector(3.0, 0.0, 0.0),
        new Vector(0.0, 4.0, 0.0),
        new Vector(3.0, 4.0, 0.0)
    };

    public PortalSpawnSystem(BetterPortals pl)  {
        this.pl = pl;
    }

    // Find a suitable location for spawning the portal
    // If a suitable location cannot be found, it just returns the location given
    // A suitable location is defined as one where the bottom of the portal is solid blocks,
    // and the three blocks above are all in air
    // The location returned is the bottom left block of the portal
    // This function will try to find and link to use for scaling the coordinates,
    // if no link is found it will return null
    public Location findSuitablePortalLocation(Location originLocation, PortalDirection direction) {
        // Loop through all of the links between worlds, and try to find a link for this portal
        WorldLink link = null;
        for(WorldLink currentLink : pl.config.worldLinks)   {
            if(currentLink.originWorld.equals(originLocation.getWorld())) {
                link = currentLink;
                break;
            }
        }
        // If no link was found, return null
        if(link == null)    {
            return null;
        }

        // Multiply by the links rescaling factor and then set it to the correct world
        Location destinationLoc = originLocation.clone();
        destinationLoc.multiply(link.coordinateRescalingFactor);
        // Reset the Y coordinate since it should not be effected by coordinate rescaling
        destinationLoc.setY(originLocation.getY());
        destinationLoc.setWorld(link.destinationWorld);
        // Limit the Y by the correct values
        destinationLoc.setY(Math.min(link.maxSpawnY, destinationLoc.getY()));
        destinationLoc.setY(Math.max(link.minSpawnY, destinationLoc.getY()));

        // Convert the location to a block and back, this should floor the location so it is all whole numbers
        Location prefferedLocation = destinationLoc.getBlock().getLocation();

        // Variables for storing the current closestSuitableLocation
        Location closestSuitableLocation = null;
        // Set the current closest distance to infinity so that all numbers are less than it
        double closestSuitableDistance = Double.POSITIVE_INFINITY;

        // Loop through a few areas around the portal
        for(double z = -35.0; z < 35.0; z += 5.0)  {
            for(double y = link.minSpawnY; y < link.maxSpawnY; y++) {
                for(double x = -35.0; x < 35.0; x += 5.0)  {
                    // Find the location of the current potential portal spawn
                    Location newLoc = new Location(prefferedLocation.getWorld(), prefferedLocation.getX() + x, y, prefferedLocation.getZ() + z);

                    // Check if the distance is less than the closest distance first, for performance
                    double distance = prefferedLocation.distance(newLoc);
                    if(distance < closestSuitableDistance)  {
                        // Check if it is suitable
                        if(checkSuitableSpawnLocation(newLoc.clone(), direction))  {
                            closestSuitableLocation = newLoc;
                            closestSuitableDistance = distance;
                        }
                    }
                }  
            }
        }

        // If a suitable location was found, return it
        if(closestSuitableLocation != null) {
            return closestSuitableLocation;
        }
        
        // Otherwise return the given location
        return prefferedLocation;
    }

    // Checks if the position is given is suitable for spawning a portal
    // See definition of a suitable location above
    public boolean checkSuitableSpawnLocation(Location location, PortalDirection direction)   {
        boolean allSuitable = true;
        // Loop through the two columbs of portal blocks
        outer:
        for(double x = 1.0; x <= 2.0; x++)  {
            // Get our current position on the x/z
            Location currentPosX = location.clone().add(VisibilityChecker.orientVector(direction, new Vector(x, 0.0, 0.0)));

            // If the ground is not solid, it is not suitable 
            if(!currentPosX.getBlock().getType().isSolid())  {
                allSuitable = false;
                break outer;
            }

            // If the air above the columns is solid, it is not suitable
            for(double y = 1.0; y <= 3.0; y++)    {
                // Get our current position including the y
                Location currentPos = currentPosX.clone().add(0.0, y, 0.0);

                if(currentPos.getBlock().getType().isSolid())  {
                    allSuitable = false;
                    break outer;
                }
            }
        }

        // Return true if both the ground and ir is suitable
        return allSuitable;
    }

    // Checks all four corners of the portal given to see if they are solid blocks
    // If they are not, they are set to stone
    public void fixPortalCorners(Location location, PortalDirection direction)  {
        // Loop through all four corners
        for(Vector offset : portalCornerLocations)  {
            // If the portal is facing north/south, invert the x and z coordinates
            offset = VisibilityChecker.orientVector(direction, offset);

            // Find the location of the block
            Location newLoc = location.clone().add(offset);
            // Check if it is solid, if it is not, set it to stone
            if(!newLoc.getBlock().getType().isSolid())  {
                newLoc.getBlock().setType(Material.STONE);
            }
        }
    }

    // Spawns a portal at the given location, with the correct orientation
    // Also spawns four blocks at the sides of the portal to stand on if they are not solid
    @SuppressWarnings("deprecation")
    public void spawnPortal(Location location, PortalDirection direction)  {
        // Loop through the x, y and z coordinates around the portal
        // Portal is only generated at z == 0,
        // The other two z coordinates are used for generating the blocks at the sides of portals
        for(double z = -1.0; z <= 1.0; z++) {
            for(double y = 0.0; y <= 4.0; y++)  {
                for(double x = 0.0; x <= 3.0; x++)  {
                    // Calculate the location next to the portal
                    Location newLoc = location.clone().add(VisibilityChecker.orientVector(direction, new Vector(x, y, z)));

                    // If the z is not 0, generate the blocks at the sides of the portal
                    if(z != 0.0)    {
                        // If the y is 0 and the x and 1 or 2 then the ground blocks need
                        // to be put down
                        if(y == 0.0)    {
                            if(x == 1.0 || x == 2.0)    {
                                // Only update the ground blocks if they are not solid
                                if(!newLoc.getBlock().getType().isSolid()) {
                                    newLoc.getBlock().setType(Material.OBSIDIAN);
                                }
                            }
                        }   else    {
                            newLoc.getBlock().setType(Material.AIR);
                        }
                        
                        continue;
                    }

                    // Otherwise, generate the portal blocks
                    // Note that we do NOT update physics when generating these blocks.
                    // This is because the physics deletes portal blocks that shouldn't be there
                    Block block = newLoc.getBlock();
                    if(x == 0 || x == 3.0 || y == 0.0 || y == 4.0)  {
                        // Set the sides of the portal to obsidian
                        block.setType(Material.OBSIDIAN, false);
                    }   else    {
                        // Set the centre to portal. NOTE: The portal must be rotated if the portal faces north/south
                        block.setType(Material.PORTAL, false);
                        if(direction == PortalDirection.NORTH_SOUTH)    {
                            block.setData((byte) 2, false);
                        }
                    }
                }
            }
        }
    }
}