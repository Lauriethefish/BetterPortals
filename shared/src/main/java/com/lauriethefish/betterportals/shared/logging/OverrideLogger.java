package com.lauriethefish.betterportals.shared.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Takes a parent logger and prints any log messages at DEBUG level or below by just using INFO
 * This is necessary because there's no good way to set bukkit's plugin logger to print debug messages (you have to go through the whole logger tree using reflection and I could never get it to work)
 */
public class OverrideLogger extends Logger {
    /**
     * Used only if the log is printed at INFO level using a prefix.
     * Makes the logs line up more by always using 3 characters for the prefix
     */
    private static final Map<Level, String> logLevelNames = new HashMap<>();
    static {
        logLevelNames.put(Level.SEVERE, "SEV");
        logLevelNames.put(Level.WARNING, "WRN");
        logLevelNames.put(Level.INFO, "INF");
        logLevelNames.put(Level.CONFIG, "CFG");
        logLevelNames.put(Level.FINE, "FNE");
        logLevelNames.put(Level.FINER, "FNR");
        logLevelNames.put(Level.FINEST, "FST");
    }

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
            String levelName = logLevelNames.get(originalLevel);
            if(levelName == null) {
                levelName = originalLevel.getName();
            }
            record.setMessage(String.format("[%s] %s", levelName, record.getMessage()));
        }

        logger.log(record);
    }
}
