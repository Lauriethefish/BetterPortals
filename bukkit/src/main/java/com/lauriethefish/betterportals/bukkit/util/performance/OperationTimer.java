package com.lauriethefish.betterportals.bukkit.util.performance;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Used to performance test certain parts of the plugin.
 * This should not be used for longer periods of time, since the time methods wrap around every second.
 */
public class OperationTimer {
    private final Instant before = getNowPrecise();

    /**
     * @return The duration representing the time taken
     */
    public Duration getTimeTaken() {
        return Duration.between(before, getNowPrecise());
    }

    /**
     * @return The time taken in nanoseconds. This wraps around every 1,000,000,000 nanoseconds
     */
    public long getTimeTakenNanoSeconds() {
        return getTimeTaken().getNano();
    }

    /**
     * @return The time taken in seconds, with nanosecond precision. This wraps around every second
     */
    public double getTimeTakenSeconds() {
        return getTimeTakenNanoSeconds() / 1_000_000_000d;
    }

    /**
     * @return The time taken in milliseconds, with nanosecond precision. This wraps around every 1,000 milliseconds.
     */
    public double getTimeTakenMillis() {
        return getTimeTakenNanoSeconds() / 1_000_000d;
    }

    /**
     * Gets an instant using System.nanoTime for more precision
     * @return The instant using nanosecond resolution time
     */
    private Instant getNowPrecise() {
        long timeNano = System.nanoTime();
        long leftOverNano = timeNano % 1_000_000_000;
        long seconds = (timeNano - leftOverNano) / 1_000_000_000;

        return Instant.ofEpochSecond(seconds, leftOverNano);
    }
}
