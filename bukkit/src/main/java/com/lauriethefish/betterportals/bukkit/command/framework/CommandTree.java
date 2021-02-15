package com.lauriethefish.betterportals.bukkit.command.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Aliases;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Command;
import com.lauriethefish.betterportals.bukkit.command.framework.annotations.Path;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class CommandTree {
    private final MessageConfig messages;
    private final Logger logger;
    private final IPlayerDataManager playerDataManager;

    private final ParentCommand rootNode;

    @Inject
    public CommandTree(MessageConfig messages, Logger logger, IPlayerDataManager playerDataManager) {
        this.messages = messages;
        this.logger = logger;
        this.playerDataManager = playerDataManager;

        // We don't want to print an exception if another command like /gamemode is entered
        this.rootNode = new ParentCommand(logger, messages, true);
    }

    private void registerCommand(Object obj, Method method) {
        String[] aliases = new String[0];
        String path = "";
        for(Annotation annotation : method.getAnnotations()) {
            if(annotation instanceof Path) {
                path = ((Path) annotation).value();
            }   else if(annotation instanceof Aliases) {
                aliases = ((Aliases) annotation).value();
            }
        }
        // Commands must have a valid path annotation, otherwise we don't know when to call them
        if(path.length() == 0) {
            throw new InvalidCommandException("No valid Path annotation found");
        }
        path = path.toLowerCase();

        String[] pathElements = path.split("/");
        // Loads other annotations like arguments
        SubCommand command = new SubCommand(obj, method, messages, logger, playerDataManager);

        rootNode.recursivelyAdd(pathElements, command);
        for(String alias : aliases) {
            addAlias(path, alias);
        }
    }

    public void registerCommands(Object obj) {
        logger.fine("Registering commands on Object of type %s", obj.getClass().getName());
        for(Method method : obj.getClass().getMethods()) {
            // Commands that have a command annotation must be registered
            for(Annotation annotation : method.getAnnotations()) {
                if(annotation instanceof Command) {registerCommand(obj, method);}
            }
        }
    }

    // Adds a new alias for the specified subcommand, or even a parent of multiple subcommands
    // For instance, addAlias("betterportals/doThing", "do") would allow you to run "/bp do" instead of /bp doThing
    // addAlias("betterportals", "bp") would allow you to use "/bp" instead of "/betterportals" in front of any subcommand in it
    public void addAlias(String path, String alias) {
        rootNode.addCommandAlias(path.split("/"), alias);
    }

    // Checks if the string is purely whitespace
    private boolean isStringBlank(String str) {
        for(char c : str.toCharArray()) {
            if(c != ' ') {return false;}
        }
        return true;
    }

    // Called by the plugin when any command is executed
    public boolean onGlobalCommand(CommandSender sender, String label, String[] args) {
        // Add the label to the args list
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add(0, label);
        args = argsList.toArray(new String[0]);

        try {
            return rootNode.execute(sender, "/", args);
        }   catch(CommandException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
            return false;
        }
    }

    public List<String> onGlobalTabComplete(CommandSender sender, String label, String[] args) {
        // Add the label to the args list
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        argsList.add(0, label);
        // Remove blank strings from the end
        if(isStringBlank(argsList.get(argsList.size() - 1))) {
            argsList.remove(argsList.size() - 1);
        }
        args = argsList.toArray(new String[0]);

        return rootNode.tabComplete(sender, args);
    }
}
