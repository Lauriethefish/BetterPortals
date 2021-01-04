package com.lauriethefish.betterportals.bukkit.stresstest;

import com.lauriethefish.betterportals.bukkit.BetterPortals;

public class StressTestManager {
    private final BetterPortals pl;

    public StressTestManager(BetterPortals pl) {
        this.pl = pl;
    }

    // Invokes the specified stress test
    public void runStressTest(String name) {
        name = name.toLowerCase(); // Non case-sensitive

        pl.logDebug("Invoking stress test %s", name);
        // Find the right test to invoke
        switch(name) {
            case "highportalcount":
                new HighPortalCountTest().runTest(pl);
                return;
            default:
                throw new IllegalArgumentException("Invalid stress test name");
        }
    }
}
