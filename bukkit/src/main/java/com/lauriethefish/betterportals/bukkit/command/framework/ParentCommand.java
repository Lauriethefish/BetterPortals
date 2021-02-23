package com.lauriethefish.betterportals.bukkit.command.framework;

import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.util.ArrayUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.command.CommandSender;

import java.util.*;

public class ParentCommand implements ICommand  {
    private final Logger logger;
    private final MessageConfig messageConfig;
    private final Map<String, ICommand> subCommands = new HashMap<>();
    // Aliases are also stored in the command array, but they're put here so we can identify them as aliases
    private final Set<String> aliases = new HashSet<>();
    // If this is true, the command will just exit silently if the subcommand does not exist instead of printing a help screen
    private final boolean isRoot;

    ParentCommand(Logger logger, MessageConfig messageConfig, boolean isRoot) {
        this.logger = logger;
        this.messageConfig = messageConfig;
        this.isRoot = isRoot;
    }

    ParentCommand(Logger logger, MessageConfig messageConfig) {
        this(logger, messageConfig, false);
    }

    @Override
    public boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException {
        // If the user didn't specify a subcommand, or they asked for help, or the subcommand was invalid, display the help screen
        if(args.length == 0 || args[0].equalsIgnoreCase("help") ||!subCommands.containsKey(args[0].toLowerCase())) {
            if(!isRoot) {
                displayHelp(sender, pathToCall);
            }
            return false;
        }   else    {
            String subCommandName = args[0].toLowerCase();
            // Add the sub command on to the path to call
            String newPathToCall = String.format("%s%s ", pathToCall, subCommandName);
            // Execute the sub command, making sure to
            ICommand subCommand = subCommands.get(subCommandName);
            return subCommand.execute(sender, newPathToCall, ArrayUtil.removeFirstElement(args));
        }
    }

    List<String> tabComplete(CommandSender sender, String[] args) {
        // If we're at the end of the chain, return a list of our sub commands
        if(args.length == 0) {
            return new ArrayList<>(subCommands.keySet());
        }

        ICommand validEnteredCommand = subCommands.get(args[0]);
        if(validEnteredCommand instanceof ParentCommand) {
            return ((ParentCommand) validEnteredCommand).tabComplete(sender, ArrayUtil.removeFirstElement(args));
        }   else    {
            // Find the commands that start with the currently entered word
            List<String> result = new ArrayList<>();
            for(String command : subCommands.keySet()) {
                if(command.startsWith(args[0])) {
                    result.add(command);
                }
            }

            return result;
        }
    }

    private void displayHelp(CommandSender sender, String pathToCall) {
        sender.sendMessage(messageConfig.getChatMessage("help"));
        subCommands.forEach((name, subCommand) -> {
            // Since this command is an alias, we don't display it in the help menu directly.
            if(aliases.contains(name)) {return;}

            String helpMessage = String.format("%s- %s%s", messageConfig.getMessageColor(), pathToCall, name);
            if(subCommand instanceof SubCommand) {
                helpMessage += ((SubCommand) subCommand).getUsage();
            }
            sender.sendMessage(helpMessage);
        });
    }

    void addCommandAlias(String[] remainingElements, String aliasName) {
        String originalName = remainingElements[0];
        if(remainingElements.length > 1) {
            // Go down to the next command if there is more left in the path
            ICommand nextCommand = subCommands.get(originalName);
            if(!(nextCommand instanceof ParentCommand)) {
                throw new IllegalArgumentException("Invalid original name for alias");
            }
            ((ParentCommand) nextCommand).addCommandAlias(ArrayUtil.removeFirstElement(remainingElements), aliasName);
        }   else    {
            ICommand toBeAliased = subCommands.get(originalName);
            if(toBeAliased == null) {throw new IllegalArgumentException("Invalid original name for alias");}

            if(subCommands.containsKey(aliasName)) {
                logger.warning("Override existing command with alias");
            }
            subCommands.put(aliasName, toBeAliased);
            aliases.add(aliasName); // Also add it to this set so that we know it's an alias
        }
    }

    // Recursively adds more subcommands until reaching the specified command
    void recursivelyAdd(String[] remainingElements, SubCommand command) {
        String currentName = remainingElements[0];
        if(remainingElements.length == 1) {
            if(subCommands.containsKey(currentName)) {
                logger.warning("Overriding previously existing command");
            }
            subCommands.put(currentName, command);
        }   else    {
            // Add a new subparent command
            if(!subCommands.containsKey(currentName)) {
                subCommands.put(currentName, new ParentCommand(logger, messageConfig));
            }

            // Add to the next parent command down if we haven't reached the bottom yet
            ParentCommand nextInLine = (ParentCommand) subCommands.get(currentName);
            nextInLine.recursivelyAdd(ArrayUtil.removeFirstElement(remainingElements), command);
        }
    }
}
