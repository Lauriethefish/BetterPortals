package com.lauriethefish.betterportals.bukkit.config;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

@Singleton
public class ProxyConfig {
    private final FileConfiguration config;
    private final Logger logger;

    @Getter private boolean isEnabled; // Whether or not bungeecord support will be enabled
    @Getter private InetSocketAddress address;
    @Getter private UUID encryptionKey; // Used so that portal data can't be intercepted on the network
    @Getter private int reconnectionDelay; // How long after being disconnected before attempting a reconnection (in ticks)

    @Inject
    public ProxyConfig(@Named("configFile") FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void load() {
        ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("proxy"), "Proxy section missing");

        isEnabled = section.getBoolean("enableProxy");
        if(!isEnabled) {return;} // No point loading everything else if the proxy is disabled

        // Load the IP address from the proxy address and port
        String rawAddress = Objects.requireNonNull(section.getString("proxyAddress"), "Proxy address missing");
        int port = section.getInt("proxyPort");
        address = new InetSocketAddress(rawAddress, port);

        reconnectionDelay = section.getInt("reconnectionDelay");

        try {
            encryptionKey = UUID.fromString(Objects.requireNonNull(section.getString("key"), "Encryption key missing"));
        }   catch(IllegalArgumentException ex) {
            // Print a warning message if it fails instead of a spammy error message
            logger.warning("Failed to load encryption key from config file! Please make sure you set this to the key in the bungeecord config.");
            isEnabled = false; // Disable proxy connection - there's no valid encryption key so connection will just fail
        }
    }
}
