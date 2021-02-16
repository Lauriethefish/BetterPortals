package com.lauriethefish.betterportals.bukkit.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandException;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandTree;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Command;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Description;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Path;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.RequiresPermissions;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.config.ProxyConfig;
import com.lauriethefish.betterportals.bukkit.net.IClientReconnectHandler;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class MainCommands {
    private final JavaPlugin pl;
    private final Logger logger;
    private final MessageConfig messageConfig;
    private final IPortalClient portalClient;
    private final ProxyConfig proxyConfig;
    private final IClientReconnectHandler reconnectHandler;

    @Inject
    public MainCommands(JavaPlugin pl, Logger logger, MessageConfig messageConfig, CommandTree commandTree, IPortalClient portalClient, ProxyConfig proxyConfig, IClientReconnectHandler reconnectHandler) {
        this.pl = pl;
        this.logger = logger;
        this.messageConfig = messageConfig;
        this.portalClient = portalClient;
        this.proxyConfig = proxyConfig;
        this.reconnectHandler = reconnectHandler;

        commandTree.registerCommands(this);
        commandTree.addAlias("betterportals", "bp");
    }

    @Command
    @Path("betterportals/reload")
    @Description("Reloads the plugin and the config file")
    @RequiresPermissions("betterportals.reload")
    public boolean reload(CommandSender sender) {
        PluginManager pluginManager = pl.getServer().getPluginManager();

        // Reload the config file, then disable/enable the plugin which will re-inject everything
        OperationTimer timer = new OperationTimer();
        pl.reloadConfig();
        pluginManager.disablePlugin(pl);
        pluginManager.enablePlugin(pl);

        sender.sendMessage(String.format("%s (%.03fms)", messageConfig.getChatMessage("reload"), timer.getTimeTakenMillis()));
        return true;
    }

    @Command
    @Path("betterportals/reconnect")
    @Description("Reconnects to the proxy if disconnect")
    @RequiresPermissions("betterportals.reconnect")
    public boolean reconnect(CommandSender sender) throws CommandException  {
        if(!proxyConfig.isEnabled()) {
            throw new CommandException(messageConfig.getErrorMessage("proxyDisabled"));
        }

        if(portalClient.isConnectionOpen()) {
            throw new CommandException(messageConfig.getErrorMessage("alreadyConnected"));
        }

        sender.sendMessage(messageConfig.getChatMessage("startedReconnection"));
        reconnectHandler.prematureReconnect();
        return true;
    }
}
