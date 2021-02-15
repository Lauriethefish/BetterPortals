package com.lauriethefish.betterportals.bukkit;

/**
 * Safely shuts down the plugin in the case of a critical error.
 * This is done in order to avoid spamming errors if some sort of crash happens.
 */
public interface ICrashHandler {
    String ISSUES_URL = "https://github.com/Lauriethefish/BetterPortals/issues";

    /**
     * Shuts down the plugin safely, printing the stack trace of <code>t</code> alongside a message to contact the developers.
     * @param t Exception that caused the crash.
     */
    void processCriticalError(Throwable t);
}
