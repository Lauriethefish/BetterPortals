package com.lauriethefish.betterportals.bukkit.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.CommandTree;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Command;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Description;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Path;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.RequiresPermissions;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.util.performance.OperationTimer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class MainCommands {
    private final JavaPlugin pl;
    private final MessageConfig messageConfig;

    @Inject
    public MainCommands(JavaPlugin pl, MessageConfig messageConfig, CommandTree commandTree) {
        this.pl = pl;
        this.messageConfig = messageConfig;

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
}
