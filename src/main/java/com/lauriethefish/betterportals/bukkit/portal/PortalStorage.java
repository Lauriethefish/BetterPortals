package com.lauriethefish.betterportals.bukkit.portal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

// Deals with saving and loading all of the active portals to portals.yml
public class PortalStorage {
    private BetterPortals pl;
    // The portals.yml file used for storing the portals
    private File storageFile;

    public PortalStorage(BetterPortals pl) {
        this.pl = pl;

        // Make sure that the data folder has been created
        Path dataFolder = createDataFolder();
        try {
            // Make the portals.yml file, if it does not exit
            createStorageFile(dataFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Checks to see if portals.yml exists. If it does not, it creates it
    private void createStorageFile(Path dataFolder) throws IOException {
        // Get the portals.yml file
        storageFile = dataFolder.resolve("portals.yml").toFile();

        // If if does not exist create it
        storageFile.createNewFile();
    }

    private Path createDataFolder() {
        // Get the plugins data folder (server/plugins/betterportals)
        Path dataFolder = pl.getDataFolder().toPath();
        // Make it if it does not exist
        dataFolder.toFile().mkdir();
        // Navigate to server/plugins/betterportals/data
        Path portalsDataFolder = dataFolder.resolve("data");
        // Make it if it does not exist
        portalsDataFolder.toFile().mkdir();
        // Return the path to the data folders
        return portalsDataFolder;
    }

    // Sets all of the parameters required for a location
    public void setLocation(ConfigurationSection section, Location location)  {
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("world", location.getWorld().getName());
    }

    // Loads all of the parameters required for a location
    public Location loadLocation(ConfigurationSection section)  {
        return new Location(
            pl.getServer().getWorld(
                section.getString("world")),
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")
        );
    }

    // Loads the x and y size of a portal
    public Vector loadPortalSize(ConfigurationSection section)  {
        return new Vector(
            section.getInt("x"),
            section.getInt("y"),
            0.0
        );
    }

    // Loads a UUID, accounting for null
    public UUID loadUUID(String string) {
        return (string == null) ? null : UUID.fromString(string);
    }

    // Set the x and y of a portal's size. The z component is not required since portals only need a width and height
    public void setPortalSize(ConfigurationSection section, Vector size)  {
        section.set("x", (int) size.getX());
        section.set("y", (int) size.getY());
    }

    // Saves the given portals to disk in portals.yml,
    // May throw an IOException if something goes wrong reading the file
    public void savePortals(Map<Location, Portal> portals) throws IOException {
        // Get the default portal list
        FileConfiguration newPortals = new YamlConfiguration();
        ConfigurationSection portalsSection = newPortals.createSection("portals");

        int i = 0;
        for(Portal portal : portals.values())    {
            portalsSection.set(String.valueOf(i), portal);
            i++;
        }

        // Save the storage file
        newPortals.save(storageFile);
    }

    // Loads a portal from the legacy format
    private Portal loadLegacyPortal(ConfigurationSection section) {
        // Load the two portal positions in the legacy format
        PortalPosition originPos = new PortalPosition(
            loadLocation(section.getConfigurationSection("portalPosition")),
            PortalDirection.valueOf(section.getString("portalDirection"))
        );

        PortalPosition destPos = new PortalPosition(
            loadLocation(section.getConfigurationSection("destinationPosition")),
            PortalDirection.valueOf(section.getString("destinationDirection"))
        );

        // Load the portalSize and other info, making sure to account for null
        Vector portalSize = loadPortalSize(section.getConfigurationSection("portalSize"));
        boolean anchored = section.getBoolean("anchored");
        String owner = section.getString("owner");
        UUID ownerId = owner == null ? null : UUID.fromString(owner);

        return new Portal(pl, originPos, destPos, portalSize, anchored, ownerId); // Return a new portal
    }

    // Loads all of the portals from portals.yml and puts them in the given list
    // If no portals were saved it should return an empty list
    // This function will through exceptions if parsing the YAML failed
    public Map<Location, Portal> loadPortals() {
        Map<Location, Portal> portals = new HashMap<>();
        
        // Load the portals.yml file as YAML
        FileConfiguration currentStorage = YamlConfiguration.loadConfiguration(storageFile);
        // If the file is empty, return an empty list
        if(currentStorage.getKeys(false).size() == 0)   {
            return portals;
        }

        // Get the portals section
        ConfigurationSection portalsSection = currentStorage.getConfigurationSection("portals");

        // Iterate through all of the portals in the list
        Iterator<String> portalItems = portalsSection.getKeys(false).iterator();
        while(portalItems.hasNext())    {
            String portalNumber = portalItems.next();
            ConfigurationSection portalSection = portalsSection.getConfigurationSection(portalNumber);
            Portal newPortal;

            if(portalSection.contains("portalPosition")) {
                // Otherwise, use the method for loading legacy portals
                pl.logDebug("Loading legacy portal.");
                newPortal = loadLegacyPortal(portalSection);
            }   else    {
                // Use configuration serialization to load a portal if it has the new format
                pl.logDebug("Loading modern portal.");
                newPortal = (Portal) portalsSection.get(portalNumber);
            }

            // Check if a portal's world is no longer loaded, since this happens when a world is deleted
            if(newPortal.getOriginPos().getWorld() == null) {
                pl.getLogger().warning(String.format("Portal at position %s, was not loaded because the world it was in no longer exists!", newPortal.getOriginPos().getVector()));
                continue;
            }

            portals.put(newPortal.getOriginPos().getLocation(), newPortal);
        }

        return portals;
    }
}