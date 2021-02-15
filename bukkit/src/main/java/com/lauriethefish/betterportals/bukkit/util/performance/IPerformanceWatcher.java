package com.lauriethefish.betterportals.bukkit.util.performance;

import java.time.Duration;
import java.util.Map;

/**
 * Keeps track of the amount of time that certain operations of the plugin take.
 * This is used during testing primarily.
 * Examples include player update times, portal block array creation times, etc.
 */
public interface IPerformanceWatcher {
    void putTimeTaken(String label, Duration duration);
    default void putTimeTaken(String label, OperationTimer timer) {
        putTimeTaken(label, timer.getTimeTaken());
    }

    Map<String, Operation> getTimedOperations();
}
