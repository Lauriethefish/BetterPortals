package com.lauriethefish.betterportals.bukkit.command.framework;

// Thrown when a command is attempted to be loaded from a method and is invalid somehow
public class InvalidCommandException extends RuntimeException   {
    public InvalidCommandException(String message) {
        super(message);
    }
}
