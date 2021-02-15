package com.lauriethefish.betterportals.bukkit.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.lauriethefish.betterportals.shared.logging.Logger;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.logging.Level;

@Singleton
public class MiscConfig {
    private final FileConfiguration config;
    private final Logger logger;

    @Getter private double portalActivationDistance;

    @Getter private boolean entitySupportEnabled;
    @Getter private int entityCheckInterval;

    @Getter private int teleportCooldown;
    @Getter private boolean updateCheckEnabled;

    @Getter private boolean testingCommandsEnabled;

    @Inject
    public MiscConfig(@Named("configFile") FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void load() {
        portalActivationDistance = config.getDouble("portalActivationDistance");
        entitySupportEnabled = config.getBoolean("enableEntitySupport");

        boolean disableEntityCheckInterval = config.getBoolean("checkForEntitiesEveryTick");
        entityCheckInterval = disableEntityCheckInterval ? 1 : config.getInt("entityCheckInterval");
        updateCheckEnabled = config.getBoolean("enableUpdateCheck");

        Level logLevel;
        try {
            logLevel = Level.parse(Objects.requireNonNull(config.getString("logLevel"), "Logging level missing"));
        }   catch(IllegalArgumentException | NullPointerException ex) {
            logger.warning("Invalid logging level found in the config");
            logger.warning("Defaulting to INFO");
            logLevel = Level.INFO;
        }
        logger.setLevel(logLevel);

        teleportCooldown = config.getInt("teleportCooldown");
        testingCommandsEnabled = config.getBoolean("enableTestingCommands");
    }
}
