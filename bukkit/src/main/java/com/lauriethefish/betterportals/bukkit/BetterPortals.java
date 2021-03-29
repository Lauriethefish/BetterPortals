package com.lauriethefish.betterportals.bukkit;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandTree;
import com.lauriethefish.betterportals.bukkit.config.ConfigManager;
import com.lauriethefish.betterportals.bukkit.config.MiscConfig;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import com.lauriethefish.betterportals.bukkit.events.IEventRegistrar;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.bukkit.portal.IPortalManager;
import com.lauriethefish.betterportals.bukkit.portal.storage.IPortalStorage;
import com.lauriethefish.betterportals.bukkit.tasks.BlockUpdateFinisher;
import com.lauriethefish.betterportals.bukkit.tasks.MainUpdate;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class BetterPortals extends JavaPlugin {
    @Inject private Logger logger;
    @Inject private ConfigManager configManager;

    @Inject private CommandTree commandTree;
    @Inject private IPortalStorage portalStorage;
    @Inject private IPlayerDataManager playerDataManager;
    @Inject private UpdateManager updateManager;
    @Inject private MiscConfig miscConfig;
    @Inject private ProxyConfig proxyConfig;
    @Inject private IPortalClient portalClient;
    @Inject private MainUpdate mainUpdate;
    @Inject private BlockUpdateFinisher blockUpdateFinisher;
    @Inject private IPortalManager portalManager;
    @Inject private IEventRegistrar eventRegistrar;
    @Inject private API apiImplementation;

    private boolean firstEnable = true;
    private boolean didEnableFail = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if(firstEnable) {
            startup();
            if(didEnableFail) {return;}

            if(miscConfig.isTestingCommandsEnabled()) {
                commandTree.registerTestCommands();
            }
        }   else    {
            reloadConfig();
            loadConfig();
        }

        if(proxyConfig.isEnabled()) {
            logger.fine("Proxy is enabled! Initialising connection . . .");
            portalClient.connect();
        }

        if(!firstEnable) {
            eventRegistrar.onPluginReload();
            portalManager.onReload();
        }

        blockUpdateFinisher.start();
        mainUpdate.start();
        portalStorage.start();

        apiImplementation.onEnable();
        firstEnable = false;
    }

    private boolean loadConfig() {
        try {
            configManager.loadValues(getConfig(), this);
        }   catch(RuntimeException ex) {
            logger.warning("Failed to reload the config file. Please check your YAML syntax!: %s: %s", ex.getClass().getName(), ex.getMessage());
            return false;
        }
        return true;
    }

    private void startup() {
        try {
            Injector injector = Guice.createInjector(new MainModule(this));
            injector.injectMembers(this);
        } catch (RuntimeException ex) {
            getLogger().severe("A critical error occurred during plugin startup");
            ex.printStackTrace();
            didEnableFail = true;
            return;
        }

        if(!loadConfig()) {
            didEnableFail = true;
            return;
        }

        try {
            portalStorage.loadPortals();
        } catch(IOException | RuntimeException ex) {
            getLogger().severe("Failed to load the portals from portals.yml. Did you modify it with an incorrect format?");
            ex.printStackTrace();
            didEnableFail = true;
            return;
        }

        if(miscConfig.isUpdateCheckEnabled()) {
            updateManager.checkForUpdates();
        }
    }

    public void softReload() {
        apiImplementation.onDisable();

        logger.fine("Performing plugin soft-reload . . .");
        if(proxyConfig.isEnabled()) {
            portalClient.shutDown();
        }

        reloadConfig();
        if(!loadConfig()) {
            return;
        }

        playerDataManager.onPluginDisable();
        portalManager.onReload();

        if(proxyConfig.isEnabled()) {
            portalClient.connect();
        }
        apiImplementation.onEnable();
    }

    @Override
    public void onDisable() {
        // We don't want to over-save the portals file if loading it failed
        if(didEnableFail) {return;}

        try {
            playerDataManager.onPluginDisable();
        }  catch(RuntimeException ex) {
            logger.severe("Error occurred while resetting player views");
            ex.printStackTrace();
        }

        try {
            portalStorage.savePortals();
        }   catch(RuntimeException | IOException ex) {
            logger.severe("Error occurred while saving the portals to portals.yml. Check your file permissions!");
            ex.printStackTrace();
        }

        if(portalClient.isConnectionOpen()) {
            portalClient.shutDown();
        }
        logger.fine("Goodbye!");
    }

    // Forward these on to the command tree to process all of our commands
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return commandTree.onGlobalCommand(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return commandTree.onGlobalTabComplete(sender, label, args);
    }
}
