package com.lauriethefish.betterportals.bukkit.chunk.chunkloading;

import com.lauriethefish.betterportals.bukkit.portal.PortalPosition;
import org.jetbrains.annotations.NotNull;

public interface IPortalChunkLoader {
    void forceloadPortalChunks(@NotNull PortalPosition destPosition);
    void unforceloadPortalChunks(@NotNull PortalPosition destPosition);
}
