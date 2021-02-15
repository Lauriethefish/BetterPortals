package com.lauriethefish.betterportals.bukkit.util.performance;

import com.google.inject.Singleton;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Used to profile certain parts of the plugin to see what is the slowest
@Singleton
public class PerformanceWatcher implements IPerformanceWatcher  {
    // Used to wait for a certain number of runs of each operation, this helps get round JVM optimisation taking a while
    private final int invocationsUntilStart = 15;

    @Getter private final Map<String, Operation> timedOperations = new ConcurrentHashMap<>();

    @Override
    public void putTimeTaken(String label, Duration duration) {
        Operation timer = timedOperations.get(label);
        if(timer == null) {
            timer = new Operation(invocationsUntilStart);
            timedOperations.put(label, timer);
        }

        timer.update(duration);
    }
}
