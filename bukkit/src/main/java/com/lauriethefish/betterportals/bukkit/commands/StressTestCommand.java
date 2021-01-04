package com.lauriethefish.betterportals.bukkit.commands;

import com.lauriethefish.betterportals.bukkit.BetterPortals;
import com.lauriethefish.betterportals.bukkit.stresstest.StressTestManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Command used for invoking stress tests
// This has no error handling, as it is not registered by default - only during testing
public class StressTestCommand implements CommandExecutor {
    private final StressTestManager stressTestManager;

    public StressTestCommand(BetterPortals pl) {
        stressTestManager = new StressTestManager(pl);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        stressTestManager.runStressTest(args[0]); // Run the stress test specified by the argument
        return true;
    }
}
