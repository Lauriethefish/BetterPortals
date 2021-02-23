package com.lauriethefish.betterportals.bukkit.portal;

public interface IPortalActivityManager {
    /**
     * May be called multiple times, should call {@link IPortal#onViewUpdate() only once for each portal}
     * @param portal The portal to be updated
     */
    void onPortalViewedThisTick(IPortal portal);

    /**
     * May be called multiple times, should call {@link IPortal#onUpdate()} only once for each portal}
     * @param portal The portal to be updated
     */
    void onPortalActivatedThisTick(IPortal portal);

    /**
     * Called once the view for all players has been updated
     */
    void postUpdate();

    void resetActivity();
}
