package com.lauriethefish.betterportals.bukkit.entity;

import com.lauriethefish.betterportals.bukkit.portal.IPortal;

public interface PortalEntityListFactory {
    IPortalEntityList create(IPortal portal, boolean requireDestination);
}
