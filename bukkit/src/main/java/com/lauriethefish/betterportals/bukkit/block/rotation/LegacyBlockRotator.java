package com.lauriethefish.betterportals.bukkit.block.rotation;

import com.google.inject.Inject;
import com.lauriethefish.betterportals.bukkit.block.data.BlockData;
import com.lauriethefish.betterportals.bukkit.block.data.LegacyBlockData;
import com.lauriethefish.betterportals.bukkit.math.Matrix;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Rails;
import org.jetbrains.annotations.NotNull;

public class LegacyBlockRotator implements IBlockRotator    {
    private final Logger logger;

    @Inject
    public LegacyBlockRotator(Logger logger) {
        this.logger = logger;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockData rotateByMatrix(@NotNull Matrix matrix, @NotNull BlockData wrappedData) {
        MaterialData data = (MaterialData) wrappedData.getUnderlying();

        if(data instanceof Directional) {
            Directional directional = (Directional) data.clone();

            BlockFace currentFace = directional.getFacing();
            BlockFace rotatedFace = BlockFaceUtil.rotateFace(currentFace, matrix);

            if(rotatedFace != null) {
                directional.setFacingDirection(rotatedFace);
                return new LegacyBlockData((MaterialData) directional);
            }
        }

        if(data instanceof Rails) {
            Rails rails = (Rails) data.clone();

            BlockFace currentFace = rails.getDirection();
            BlockFace rotatedFace = BlockFaceUtil.rotateFace(currentFace, matrix);

            if(rotatedFace != null) {
                rails.setDirection(rotatedFace, rails.isOnSlope());
                return new LegacyBlockData(rails);
            }
        }

        return wrappedData;
    }
}
