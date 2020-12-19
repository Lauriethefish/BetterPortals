package com.lauriethefish.betterportals.bukkit.config;

import java.net.InetSocketAddress;
import java.util.UUID;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

// Section of the config that specifies the proxy for cross-server portals
public class ProxyConfig {
    @Getter private boolean isEnabled; // Whether or not bungeecord support will be enabled
    @Getter private InetSocketAddress address;
    @Getter private UUID encryptionKey; // Used so that portal data can't be intercepted on the network
    @Getter private int reconnectionDelay; // How long after being disconnected before attempting a reconnection (in ticks)

    public ProxyConfig(BetterPortals pl, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("proxy");

        isEnabled = section.getBoolean("enableProxy");
        if(!isEnabled) {return;} // No point loading everything else if the proxy is disabled

        // Load the IP address from the proxy address and port
        String rawAddress = section.getString("proxyAddress");
        int port = section.getInt("proxyPort");
        address = new InetSocketAddress(rawAddress, port);

        reconnectionDelay = section.getInt("reconnectionDelay");
        
        try {
            encryptionKey = UUID.fromString(section.getString("key"));
        }   catch(IllegalArgumentException ex) {
            // Print a warning message if it fails instead of a spammy error message
            pl.getLogger().warning("Failed to load encryption key from config file! Please make sure you set this to the key in the bungeecord config.");
            isEnabled = false; // Disable proxy connection - there's no valid encryption key so connection will just fail
        }
    }
}
