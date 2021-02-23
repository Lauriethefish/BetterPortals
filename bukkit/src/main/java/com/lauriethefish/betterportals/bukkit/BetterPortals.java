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
    private Logger logger;
    private ConfigManager configManager;

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

    private boolean firstEnable = true;
    private boolean didEnableFail = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Unfortunately, guice doesn't really have support for conveniently catching errors during eager bindings, or binding some classes first then stopping if they fail.
        // To get round this, we first create an initial injector for loading the config, then create another with the eager bindings actually used for startup.
        // This works out pretty well.
        Injector preInitInjector = null;
        if(firstEnable) {
            preInitInjector = Guice.createInjector(new PreInitModule(this));
            this.logger = preInitInjector.getInstance(Logger.class);
            this.configManager = preInitInjector.getInstance(ConfigManager.class);
        }

        try {
            configManager.loadValues(getConfig(), this);
        }   catch(RuntimeException ex) {
            logger.severe("Failed to load the config file. Is it definitely valid YAML?");
            logger.warning("%s: %s", ex.getClass().getName(), ex.getMessage());
            didEnableFail = true;
            return;
        }

        if(firstEnable) {
            try {
                Injector injector = preInitInjector.createChildInjector(new MainModule(this));
                injector.injectMembers(this);
            } catch (RuntimeException ex) {
                logger.severe("A critical error occurred during plugin startup");
                ex.printStackTrace();
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
        }   else    {
            eventRegistrar.onPluginReload();
        }

        if(proxyConfig.isEnabled()) {
            logger.fine("Proxy is enabled! Initialising connection . . .");
            portalClient.connect();
        }

        blockUpdateFinisher.start();
        mainUpdate.start();
        portalStorage.start();

        firstEnable = false;
    }

    public void softReload() {
        logger.fine("Performing plugin soft-reload . . .");
        if(proxyConfig.isEnabled()) {
            portalClient.shutDown();
        }

        reloadConfig();
        try {
            configManager.loadValues(getConfig(), this);
        }   catch(RuntimeException ex) {
            logger.warning("Failed to reload the config file. Please check your YAML syntax!: %s: %s", ex.getClass().getName(), ex.getMessage());
            return;
        }

        playerDataManager.onPluginDisable();
        portalManager.onReload();

        if(proxyConfig.isEnabled()) {
            portalClient.connect();
        }
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
