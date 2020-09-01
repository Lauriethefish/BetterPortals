package com.lauriethefish.betterportals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    // Set the x and y of a portal's size. The z component is not required since portals only need a width and height
    public void setPortalSize(ConfigurationSection section, Vector size)  {
        section.set("x", (int) size.getX());
        section.set("y", (int) size.getY());
    }

    // Saves the given portals to disk in portals.yml,
    // May throw an IOException if something goes wrong reading the file
    public void savePortals(Map<Location, PortalPos> portals) throws IOException {
        // Get the default portal list
        FileConfiguration newPortals = new YamlConfiguration();
        ConfigurationSection portalsSection = newPortals.createSection("portals");

        int i = 0;
        for(PortalPos portal : portals.values())    {
            // Create a section of the list for this portal
            ConfigurationSection portalSection = portalsSection.createSection(String.valueOf(i));
            // Set the two location and two directions of the portal
            setLocation(portalSection.createSection("portalPosition"), portal.portalPosition);
            portalSection.set("portalDirection", portal.portalDirection.toString());
            setLocation(portalSection.createSection("destinationPosition"), portal.destinationPosition);
            portalSection.set("destinationDirection", portal.destinationDirection.toString());
            
            // Set the portal's size
            setPortalSize(portalSection.createSection("portalSize"), portal.portalSize);
            i++;
        }

        // Save the storage file
        newPortals.save(storageFile);
    }

    // Loads all of the portals from portals.yml and puts them in the given list
    // If no portals were saved it should return an empty list
    // This function will through exceptions if parsing the YAML failed
    public Map<Location, PortalPos> loadPortals() {
        Map<Location, PortalPos> portals = new HashMap<>();
        
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
            // Get a ConfigurationSection for the next portal
            ConfigurationSection nextPortalSection = portalsSection.getConfigurationSection(portalItems.next());

            // Get the two positions and two directions of the portal
            Location portalPosition = loadLocation(nextPortalSection.getConfigurationSection("portalPosition"));
            PortalDirection portalDirection = PortalDirection.valueOf(nextPortalSection.getString("portalDirection"));
            Location destinationPosition = loadLocation(nextPortalSection.getConfigurationSection("destinationPosition"));
            PortalDirection destinationDirection = PortalDirection.valueOf(nextPortalSection.getString("destinationDirection"));

            // Load thr portal's size
            Vector portalSize = loadPortalSize(nextPortalSection.getConfigurationSection("portalSize"));

            // Add a new portal to the map with the given values
            portals.put(portalPosition, new PortalPos(pl, portalPosition, portalDirection, destinationPosition, destinationDirection, portalSize));
        }

        // Return the map of all portals
        return portals;
    }
}