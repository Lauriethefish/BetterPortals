package com.lauriethefish.betterportals.bukkit.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

import javax.inject.Named;

/**
 * Handles formatting text based on what's in the messages section of the config.
 */
@Singleton
public class MessageConfig {
    private final FileConfiguration file;

    private final Map<String, String> messageMap = new HashMap<>();

    @Getter private String portalWandName;
    @Getter private String prefix;
    @Getter private String messageColor;

    @Inject
    public MessageConfig(@Named("configFile") FileConfiguration file) {
        this.file = file;
    }

    public void load() {
        ConfigurationSection messagesSection = Objects.requireNonNull(file.getConfigurationSection("chatMessages"), "Missing chat messages section");

        for(String key : messagesSection.getKeys(false)) {
            messageMap.put(key, ChatColor.translateAlternateColorCodes('&', messagesSection.getString(key)));
        }

        portalWandName = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(file.getString("portalWandName"), "Missing portalWandName"));
        prefix = getRawMessage("prefix");
        messageColor = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(messagesSection.getString("messageColor"), "Missing messageColor"));
    }

    /**
     * Finds a chat message with the plugin prefix.
     * @param name The name in the config
     * @return A chat message with the configured plugin prefix
     */
    public String getChatMessage(String name) {
        return prefix + getRawMessage(name);
    }

    /**
     * Finds a chat message without the prefix, for boxing in a {@link CommandException}
     * @param name The name in the config
     * @return A chat message without the prefix.
     */
    public String getErrorMessage(String name) {
        return getRawMessage(name);
    }

    /**
     * Returns a yellow message for warnings in chat.
     * @param name The name in the config
     * @return The yellow formatted message
     */
    public String getWarningMessage(String name) {
        return ChatColor.YELLOW + getRawMessage(name);
    }

    /**
     * Finds a chat message without the prefix.
     * @param name The name in the config
     * @return A chat message without the prefix.
     */
    public String getRawMessage(String name) {
        return messageMap.get(name);
    }
}
