package com.lauriethefish.betterportals.shared.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Takes a parent logger and prints any log messages at DEBUG level or below by just using INFO
 * This is necessary because there's no good way to set bukkit's plugin logger to print debug messages
 */
public class OverrideLogger extends Logger {
    private final java.util.logging.Logger logger;

    public OverrideLogger(java.util.logging.Logger logger) {
        super(logger.getName(), null);
        super.setLevel(Level.INFO);
        this.logger = logger;
    }

    @Override
    public void log(LogRecord record) {
        // Return if the log level is too low
        if(record.getLevel().intValue() < super.getLevel().intValue()) {return;}

        // If the level is lower that what will be printed by minecraft's chain of loggers, but we should print it, add a [DEBUG] tag and set the level back to INFO
        Level originalLevel = record.getLevel();
        if(originalLevel.intValue() < Level.INFO.intValue()) {
            record.setLevel(Level.INFO);
            record.setMessage(String.format("[%s] %s", originalLevel.getName(), record.getMessage()));
        }

        logger.log(record);
    }
}
