package com.lauriethefish.betterportals.bukkit.util.performance;

import lombok.Getter;

import java.time.Duration;

public class Operation {
    @Getter private Duration totalTime = Duration.ZERO;
    @Getter private long invocationTimes;

    private Duration highestTime = null;
    private Duration lowestTime = null;

    private int invocationsUntilStart;
    public Operation(int invocationsUntilStart) {
        this.invocationsUntilStart = invocationsUntilStart;
    }

    void update(Duration duration) {
        if(invocationsUntilStart > 0) {
            invocationsUntilStart--;
            return;
        }

        totalTime = totalTime.plus(duration);
        invocationTimes++;

        // This returns greater than 0 if more, less than 0 if lesser
        if(highestTime == null || duration.compareTo(highestTime) > 0) {
            highestTime = duration;
        }

        // This returns less than 0 if lesser, greater than 0 if more
        if(lowestTime == null || duration.compareTo(lowestTime) < 0) {
            lowestTime = duration;
        }
    }

    public Duration getAverageTime() {
        // Avoid dividing by zero exceptions
        if(invocationTimes == 0) {return Duration.ZERO;}

        return totalTime.dividedBy(invocationTimes);
    }

    public Duration getHighestTime() {
        return highestTime == null ? Duration.ZERO : highestTime;
    }

    public Duration getLowestTime() {
        return lowestTime == null ? Duration.ZERO : lowestTime;
    }
}
