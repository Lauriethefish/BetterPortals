package com.lauriethefish.betterportals.bukkit.util.nms;

import lombok.Getter;

/**
 * Represents an NMS animation ID
 */
public enum AnimationType {
    MAIN_HAND(0),
    DAMAGE(1),
    LEAVE_BED(2),
    OFF_HAND(3);

    @Getter private final int nmsId;
    AnimationType(int nmsId) {
        this.nmsId = nmsId;
    }
}
