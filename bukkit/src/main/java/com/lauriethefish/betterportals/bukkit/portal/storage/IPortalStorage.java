package com.lauriethefish.betterportals.bukkit.portal.storage;

import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;

import java.io.IOException;

/**
 * Saves/loads portals from disk
 * TODO: Eventually add support for databases, or something else instead of just YAML
 */
public interface IPortalStorage {
    /**
     * Loads all stored portals and registers them in the {@link IPortalManager}
     */
    void loadPortals() throws IOException;

    /**
     * Saves all currently registered portals in {@link IPortalManager}
     */
    void savePortals() throws IOException;
}
