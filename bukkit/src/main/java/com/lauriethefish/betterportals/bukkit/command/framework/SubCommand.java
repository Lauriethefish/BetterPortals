package com.lauriethefish.betterportals.bukkit.command.framework;

import com.lauriethefish.betterportals.bukkit.command.framework.annotations.*;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.player.IPlayerData;
import com.lauriethefish.betterportals.bukkit.player.IPlayerDataManager;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubCommand implements ICommand {
    private final Object instance;
    private final Method method;
    private final MessageConfig messageConfig;
    private final Logger logger;
    private final IPlayerDataManager playerDataManager;

    private boolean requiresPlayer = false;
    private boolean usePlayerData; // Whether or not we'll automatically fetch the IPlayerData for the first argument
    private String[] requiredPermissions = new String[0];
    private Argument[] arguments = new Argument[0];
    private Class<?>[] argumentTypes;
    private String description = ""; // Default is no description

    @Getter private String usage;

    SubCommand(Object instance, Method method, MessageConfig messageConfig, Logger logger, IPlayerDataManager playerDataManager) {
        this.instance = instance;
        this.method = method;
        this.messageConfig = messageConfig;
        this.logger = logger;
        this.playerDataManager = playerDataManager;

        if(method.getReturnType() != boolean.class) {
            throw new InvalidCommandException("Command annotated methods must return a boolean");
        }

        loadFromMethod();
        checkArgTypes();
        generateUsage();
    }

    // Uses the command annotations to load the command from this method
    private void loadFromMethod() {
        // The first parameter is the command sender, so we skip it
        argumentTypes = Arrays.copyOfRange(method.getParameterTypes(), 1, method.getParameterCount());

        boolean commandAnnotationFound = false;
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof Command) {
                commandAnnotationFound = true;
            } else if (annotation instanceof RequiresPlayer) {
                requiresPlayer = true;
            } else if (annotation instanceof RequiresPermissions) {
                requiredPermissions = ((RequiresPermissions) annotation).value();
            } else if (annotation instanceof Arguments) { // Otherwise, multiple arguments are wrapped in here
                arguments = ((Arguments) annotation).value();
            } else if(annotation instanceof Argument) { // If there is one argument then it isn't wrapped
                arguments = new Argument[]{(Argument) annotation};
            } else if (annotation instanceof Description) {
                description = ((Description) annotation).value();
            }
        }

        // Sanity checking
        if (!commandAnnotationFound) {
            throw new InvalidCommandException("Command methods require the command annotation");
        }
    }

    // Checks that the method has the correct number of parameters and a CommandSender as its first argument
    private void checkArgTypes() {
        Parameter[] methodParams = method.getParameters();
        if(methodParams.length != arguments.length + 1) {
            throw new InvalidCommandException("Incorrect number of arguments on command method. Commands must have 1 argument for the sender and one argument per annotated argument");
        }

        Class<?> firstParamType = methodParams[0].getType();

        boolean isFirstArgValid = true;
        if(requiresPlayer) {
            if(firstParamType.isAssignableFrom(IPlayerData.class)) {
                usePlayerData = true; // Command methods annotated with a player requirement can also use IPlayerData as their first argument
            }   else if(!firstParamType.isAssignableFrom(Player.class)) {
                isFirstArgValid = false; // Otherwise, set it to invalid if we can't assign it from a player
            }
        }   else if(!firstParamType.isAssignableFrom(CommandSender.class)) { // Non player requirement annotated commands must just take a CommandSender
            isFirstArgValid = false;
        }

        if(!isFirstArgValid) {
            throw new InvalidCommandException("The first argument for a command must be a CommandSender. (or a Player/IPlayerData if annotated with a player requirement)");
        }
    }

    private void generateUsage() {
        StringBuilder builder = new StringBuilder();
        for(Argument argument : arguments) {
            boolean required = argument.defaultValue().equals("");
            // Use square brackets for optional arguments, comparator signs for required ones
            if(required) {
                builder.append(String.format(" <%s>", argument.name()));
            }   else    {
                builder.append(String.format(" [%s]", argument.name()));
            }
        }
        // Avoid adding the colon for blank descriptions
        if(!description.isEmpty()) {
            builder.append(": ");
            builder.append(description);
        }
        usage = builder.toString();
    }

    // Attempts to parse an argument as various primitive types before attempting to call a static valueOf method on its class
    private Object parseArgument(Class<?> type, String argument) throws CommandException {
        logger.fine("Attempting to parse string \"%s\" as type %s", argument, type.getName());
        try {
            if(type == String.class) {
                return argument;
            }   else if(type == int.class) {
                return Integer.parseInt(argument);
            }   else if(type == short.class) {
                return Short.parseShort(argument);
            }   else if(type == long.class) {
                return Long.parseLong(argument);
            }   else if(type == byte.class) {
                return Byte.parseByte(argument);
            }   else if(type == boolean.class) {
                return Boolean.parseBoolean(argument);
            }   else if(type.isPrimitive()) {
                throw new InvalidCommandException("Unknown primitive type on command argument");
            }   else    {
                try {
                    return ReflectionUtil.runMethod(null, type, "valueOf", new Class[]{String.class}, new Object[]{argument});
                }   catch(RuntimeException ex) {
                    throw new CommandException(messageConfig.getErrorMessage("invalidArgs"), ex);
                }
            }

        }   catch(IllegalArgumentException ex) {
            throw new CommandException(messageConfig.getErrorMessage("invalidArgs"), ex);
        }
    }

    private void displayUsage(String pathToCall) throws CommandException {
        throw new CommandException("Usage: " + pathToCall + usage);
    }

    @Override
    public boolean execute(CommandSender sender, String pathToCall, String[] args) throws CommandException {
        for(String permission : requiredPermissions) {
            if(!sender.hasPermission(permission)) {
                throw new CommandException(messageConfig.getErrorMessage("notEnoughPerms"));
            }
        }

        if(requiresPlayer && !(sender instanceof Player)) {
            throw new CommandException(messageConfig.getErrorMessage("mustBePlayer"));
        }

        // Fetch the IPlayerData if required, or just add the sender
        List<Object> parsedArgs = new ArrayList<>();
        if(usePlayerData) {
            IPlayerData playerData = playerDataManager.getPlayerData((Player) sender);
            if(playerData == null) {
                throw new IllegalStateException("Player called command without registered player data");
            }
            parsedArgs.add(playerData);
        }   else {
            parsedArgs.add(sender);
        }


        int i = 0;
        for(Argument argument : arguments) {
            boolean wasEntered = i < args.length;
            boolean isRequired = argument.defaultValue().equals("");
            if(isRequired && !wasEntered) {
                displayUsage(pathToCall);
            }

            // Attempt to parse each argument
            String givenValue = wasEntered ? args[i] : argument.defaultValue();
            parsedArgs.add(parseArgument(argumentTypes[i], givenValue));
            i++;
        }

        // Call the method, making sure to rethrow CommandExceptions
        try {
            return (boolean) method.invoke(instance, parsedArgs.toArray());
        }   catch(IllegalAccessException ex) {
            throw new InvalidCommandException("Command annotated methods must be public");
        }   catch(InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if(cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }   else if(cause instanceof CommandException){
                throw (CommandException) cause;
            }   else    {
                // Box it as a RuntimeException, then Bukkit will tell the player what happened via an internal error message
                throw new RuntimeException(cause);
            }
        }
    }
}
