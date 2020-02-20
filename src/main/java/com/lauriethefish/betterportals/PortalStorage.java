package com.lauriethefish.betterportals;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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

    // Saves the given portals to disk in portals.yml,
    // May throw an IOException if something goes wrong reading the file
    public void savePortals(List<PortalPos> portals) throws IOException {
        // Get the default portal list
        FileConfiguration newPortals = YamlConfiguration.loadConfiguration(new InputStreamReader(pl.getResource("portals.yml")));
        
        // Loop through every portal
        for(int i = 0; i < portals.size(); i++) {
            PortalPos portal = portals.get(i);
            
            // Create a section of the list for this portal
            ConfigurationSection portalSection = newPortals.createSection(String.format("portals.%s", i));
            // Set the two location and two directions of the portal
            setLocation(portalSection.createSection("portalPosition"), portal.portalPosition);
            portalSection.set("portalDirection", portal.portalDirection.toString());
            setLocation(portalSection.createSection("destinationPosition"), portal.destinationPosition);
            portalSection.set("destinationDirection", portal.destinationDirection.toString());
        }

        // Save the storage file
        newPortals.save(storageFile);
    }

    // Loads all of the portals from portals.yml and puts them in the given list
    // If no portals were saved it should return an empty list
    // This function will through exceptions if parsing the YAML failed
    public List<PortalPos> loadPortals() {
        List<PortalPos> portals = new ArrayList<>();
        
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

            // Add a new portal to the list with the given values
            portals.add(new PortalPos(portalPosition, portalDirection, destinationPosition, destinationDirection));
        }

        // Return the list of all portals
        return portals;
    }
}