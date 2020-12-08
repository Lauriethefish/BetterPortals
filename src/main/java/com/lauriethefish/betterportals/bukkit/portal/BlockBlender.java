package com.lauriethefish.betterportals.bukkit.portal;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.util.Vector;

public class BlockBlender {
    private Random random = new Random();
    
    // Blocks will be taken from position A and randomly swapped with the blocks around position B
    private Location positionA;
    private Location positionB;

    private double fallOffRate;
    private int blockRadius;

    public BlockBlender(Location positionA, Location positionB, double fallOffRate) {
        this.positionA = positionA;
        this.positionB = positionB;
        this.fallOffRate = fallOffRate;
        this.blockRadius = (int) (1.0 / fallOffRate) + 2;

        swapBlocksRandomly();
    }

    // Randomly swaps some of the blocks at the origin with those at the destination
    private void swapBlocksRandomly() {
        for(int z = -blockRadius; z < blockRadius; z++) {
            for(int y = -blockRadius; y < blockRadius; y++) {
                for(int x = -blockRadius; x < blockRadius; x++) {
                    Vector relativePos = new Vector(x, y, z);

                    double swapChance = calculateSwapChance(relativePos);
                    // Apply the random chance
                    if(random.nextDouble() > swapChance) {continue;}

                    Location originPos = positionA.clone().add(relativePos);
                    Location destPos = positionB.clone().add(relativePos);

                    BlockState originState = positionA.getBlock().getState(); // Save the origin state first
                    
                    // Swap the block states
                    originPos.getBlock().setType(destPos.getBlock().getType());
                    destPos.getBlock().setType(originState.getType());
                }
            }
        }
    }

    private double calculateSwapChance(Vector relativePos) {
        double distance = relativePos.length();
        return 1.0 - distance * fallOffRate;
    }
}
