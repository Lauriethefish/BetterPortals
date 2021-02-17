package com.lauriethefish.betterportals.bukkit.portal.storage;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.bukkit.portal.Portal;
import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Stores all portals in YAML format.
 * This makes use of {@link org.bukkit.configuration.serialization.ConfigurationSerializable} quite a bit for convenience.
 * Portals are currently stored in <code>plugins/BetterPortals/data/portals.yml</code>
 * TODO: Add legacy portal loading
 */
public class YamlPortalStorage implements IPortalStorage    {
    private final JavaPlugin pl;
    private final IPortalManager portalManager;
    private final Logger logger;

    @Inject
    public YamlPortalStorage(JavaPlugin pl, Logger logger, IPortalManager portalManager) {
        this.pl = pl;
        this.logger = logger;
        this.portalManager = portalManager;

        ConfigurationSerialization.registerClass(Portal.class);
        ConfigurationSerialization.registerClass(PortalPosition.class);
    }

    private Path getDataFolder() {
        File pluginFolder = pl.getDataFolder();
        pluginFolder.mkdir();

        File dataFolder = pluginFolder.toPath().resolve("data").toFile();
        dataFolder.mkdir();
        return dataFolder.toPath();
    }

    private File getPortalsFile() {
        return getDataFolder().resolve("portals.yml").toFile();
    }

    private FileConfiguration loadPortalsFile() {
        logger.fine("Loading from plugins/BetterPortals/data/portals.yml");
        return YamlConfiguration.loadConfiguration(getPortalsFile());
    }

    private void savePortalsFile(FileConfiguration configFile) throws IOException {
        logger.fine("Saving to plugins/BetterPortals/data/portals.yml");
        configFile.save(getPortalsFile());
    }

    @Override
    public void loadPortals()    {
        FileConfiguration file = loadPortalsFile();

        ConfigurationSection portalsSection = file.getConfigurationSection("portals");
        if(portalsSection == null) {
            logger.fine("The portals file was empty, stopping!");
            return;
        }

        Set<String> portalNumbers = portalsSection.getKeys(false);
        logger.finer("Loading %d portals from parsed YAML . . .", portalNumbers.size());
        for(String portalNumber : portalNumbers) {
            ConfigurationSection portalSection = portalsSection.getConfigurationSection(portalNumber);
            Portal newPortal;

            try {
                if (portalSection != null && portalSection.contains("portalPosition")) {
                    logger.finer("Loading legacy portal.");
                    newPortal = /*loadLegacyPortal(portalSection); // TODO*/null;
                } else {
                    logger.finer("Loading modern portal.");
                    newPortal = (Portal) portalsSection.get(portalNumber);
                }
            }   catch(RuntimeException ex) { // Avoid failing all portals when one is invalid
                logger.warning("Failed to load portal: %s", ex.getMessage());
                continue;
            }

            // Check if a portal's world is no longer loaded, since this happens when a world is deleted
            if(newPortal.getOriginPos().getWorld() == null) {
                pl.getLogger().warning(String.format("Portal at position %s, was not loaded because the world it was in no longer exists!", newPortal.getOriginPos().getVector()));
                continue;
            }

            portalManager.registerPortal(newPortal);
        }
        logger.fine("Loaded portals");
    }

    @Override
    public void savePortals() throws IOException    {
        FileConfiguration file = new YamlConfiguration();

        ConfigurationSection portalsSection = file.createSection("portals");

        logger.fine("Saving all portals . . .");
        int i = 0;
        for(IPortal portal : portalManager.getAllPortals()) {
            try {
                portalsSection.set(String.valueOf(i), portal);
            }   catch(RuntimeException ex) { // Avoid failing all portals when one is invalid
                logger.warning("Failed to save portal: %s", ex.getMessage());
            }
            i++;
        }
        logger.fine("Saved %d portals", i);

        savePortalsFile(file);
    }
}
