package com.lauriethefish.betterportals.bukkit.portal;

import java.util.Random;

import com.lauriethefish.betterportals.bukkit.ReflectUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;

public class BlockBlender {
    private final double INITIAL_CHANCE = 1.0;

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
        this.blockRadius = (int) (1.0 / fallOffRate + 4.0 + INITIAL_CHANCE);

        swapBlocksRandomly();
    }

    // Randomly swaps some of the blocks at the origin with those at the destination
    private void swapBlocksRandomly() {
        Environment destEnvironment = positionB.getWorld().getEnvironment();
        
        // Chose a block to swap in if the block at the destination is air
        Material fillInBlock = null;
        switch(destEnvironment) {
            case NETHER:
                fillInBlock = Material.NETHERRACK;
                break;
            case NORMAL:
                fillInBlock = Material.STONE;
                break;
            case THE_END:
                fillInBlock = ReflectUtils.isLegacy ? Material.valueOf("ENDER_STONE") : Material.valueOf("END_STONE");
                break;
        }

        for(int z = -blockRadius; z < blockRadius; z++) {
            for(int y = -blockRadius; y < blockRadius; y++) {
                for(int x = -blockRadius; x < blockRadius; x++) {
                    Vector relativePos = new Vector(x, y, z);

                    double swapChance = calculateSwapChance(relativePos);
                    // Apply the random chance
                    if(random.nextDouble() > swapChance) {continue;}

                    Location originPos = positionA.clone().add(relativePos);
                    Location destPos = positionB.clone().add(applyRandomOffset(relativePos, 10.0));

                    Material originType = originPos.getBlock().getType(); // Save the origin type first
                    Material destType = destPos.getBlock().getType();

                    if(!destType.isSolid()) {destType = fillInBlock;}
                    // Don't replace air or obsidian blocks so the portal doesn't get broken and we don't get blocks in the air.
                    if(originType != Material.AIR && originType != Material.OBSIDIAN && originType != ReflectUtils.portalMaterial) {
                        // Swap the block states
                        originPos.getBlock().setType(destType);
                    }
                }
            }
        }
    }

    // Returns a new vector with its coordinates randomly moved by up to 1/2 of power
    private Vector applyRandomOffset(Vector vec, double power) {
        Vector other = new Vector(); // Avoid mutating the original vector
        // Apply a random offset multiplied by the power
        other.setX(vec.getX() + (random.nextDouble() - 0.5) * power);
        other.setY(vec.getY() + (random.nextDouble() - 0.5) * power);
        other.setZ(vec.getZ() + (random.nextDouble() - 0.5) * power);

        return other;
    }

    private double calculateSwapChance(Vector relativePos) {
        double distance = relativePos.length();
        return INITIAL_CHANCE - distance * fallOffRate;
    }
}
