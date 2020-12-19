package com.lauriethefish.betterportals.bukkit.config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

// Stores any parts of the config that are related to configuring text
public class MessageConfig {
    private Map<String, String> messageMap = new HashMap<>();

    @Getter private String portalWandName;
    @Getter private String prefix;

    public MessageConfig(FileConfiguration file) {
        // Load each chat message into the map
        ConfigurationSection messagesSection = file.getConfigurationSection("chatMessages");
        for(String key : messagesSection.getKeys(false)) {
            messageMap.put(key, ChatColor.translateAlternateColorCodes('&', messagesSection.getString(key)));
        }

        // Load remaining message related stuff
        portalWandName = ChatColor.translateAlternateColorCodes('&', file.getString("portalWandName"));
        prefix = getRawMessage("prefix");
    }

    // Gets a chat message with the configured prefix
    public String getChatMessage(String name) {
        return prefix + getRawMessage(name);
    }

    // Returns a message formatted as red for sending errors
    public String getErrorMessage(String name) {
        return ChatColor.RED + getRawMessage(name);
    }

    // Returns a chat message without its prefix
    public String getRawMessage(String name) {
        return messageMap.get(name);
    }

    // Throws a RuntimeException with an error message from the message map, since this is what commands use to signify failure
    public void throwError(String name) {
        throw new RuntimeException(getRawMessage(name));
    }
}
