package com.lauriethefish.betterportals.bukkit.portal.blockarray;

enum UpdateMode {
    ORIGIN, // Check only the origin position
    DESTINATION, // Check only the destination position
    LOCAL // The portal is local on both sides, so we can check both
}
