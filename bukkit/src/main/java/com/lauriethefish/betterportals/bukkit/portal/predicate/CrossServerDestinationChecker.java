package com.lauriethefish.betterportals.bukkit.portal.predicate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.api.BetterPortal;
import com.lauriethefish.betterportals.api.PortalPredicate;
import com.lauriethefish.betterportals.bukkit.net.IPortalClient;
import com.lauriethefish.betterportals.bukkit.net.requests.CheckDestinationValidityRequest;
import com.lauriethefish.betterportals.bukkit.util.VersionUtil;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.net.RequestException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class CrossServerDestinationChecker implements PortalPredicate {
    /**
     * Time, in seconds, between each check that the cross-server portal has a valid destination
     */
    private static final int VALIDITY_CHECK_INTERVAL = 1;

    private final Logger logger;
    private final IPortalClient portalClient;

    private final Map<BetterPortal, Boolean> cachedValidity = new HashMap<>();
    private final Map<BetterPortal, Instant> lastChecked = new HashMap<>();
    private final Set<BetterPortal> ongoingRequest = new HashSet<>();

    private boolean wasConnectedLastTick = true;

    @Inject
    public CrossServerDestinationChecker(Logger logger, IPortalClient portalClient) {
        this.logger = logger;
        this.portalClient = portalClient;
    }

    @Override
    public boolean test(@NotNull BetterPortal portal, @NotNull Player player) {
        if(!portal.isCrossServer()) {
            return true;
        }

        if(!portalClient.canReceiveRequests()) {
            if(wasConnectedLastTick) {
                wasConnectedLastTick = false;
                logger.warning("Cross server portals deactivating - disconnected from the proxy");
            }
            return false;
        }

        if(!wasConnectedLastTick) {
            logger.info("Cross-server portals reactivating! - proxy is connected");
            wasConnectedLastTick = true;
        }

        Boolean cachedValidityValue = checkCache(portal);
        if(cachedValidityValue != null) {
            return cachedValidityValue;
        }   else    {
            // If it has been more than VALIDITY_CHECK_INTERVAL seconds since the last check, or the validity has not been cached yet, send a request to check it
            runValidityCheck(portal);

            // If the previous validity check returned successful, don't temporarily deactivate the portal while waiting to receive the response
            Boolean nonExpiredCachedValue = cachedValidity.get(portal);
            return nonExpiredCachedValue != null && nonExpiredCachedValue;
        }
    }

    /**
     * Checks if there is a recent enough cached validity value for <code>portal</code>.
     * @param portal The portal to check the cache for
     * @return The cached validity value, or null if there is none or the value is out of date.
     */
    private @Nullable Boolean checkCache(@NotNull BetterPortal portal) {
        Instant lastTimeChecked = lastChecked.get(portal);
        if(lastTimeChecked == null) {return null;}
        double secondsElapsed = Duration.between(lastTimeChecked, Instant.now()).getSeconds();

        if(secondsElapsed >= VALIDITY_CHECK_INTERVAL) {
            return null;
        }   else    {
            return cachedValidity.get(portal);
        }
    }

    /**
     * Sends a request to the proxy to check that the portal can be activated
     * @param portal The portal to check the validity of
     */
    private void runValidityCheck(@NotNull BetterPortal portal) {
        if(ongoingRequest.contains(portal)) {
            return;
        }
        ongoingRequest.add(portal);

        logger.finer("Checking validity of portal %s", portal.getId());
        CheckDestinationValidityRequest request = new CheckDestinationValidityRequest();
        request.setOriginGameVersion(VersionUtil.getCurrentVersion());
        request.setDestinationWorldId(portal.getDestPos().getWorldId());
        request.setDestinationWorldName(portal.getDestPos().getWorldName());

        portalClient.sendRequestToServer(request, portal.getDestPos().getServerName(), (response) -> {
            try {
                response.checkForErrors();
                putValidityValue(portal, true);
                logger.finer("Destination validity OK!");
            }   catch(RequestException ex) {
                // Avoid spamming validity messages by only logging when the validity changes to invalid
                Boolean previousValidityValue = cachedValidity.get(portal);
                if(previousValidityValue == null || previousValidityValue) {
                    logger.warning("Not activating cross server portal - destination is invalid: %s", ex.getMessage());
                }
                putValidityValue(portal, false);
            }
            ongoingRequest.remove(portal);
        });
    }

    /**
     * Caches <code>newValue</code> at the current time.
     * @param portal The portal to cache the validity for
     * @param newValue The new validity value
     */
    private void putValidityValue(@NotNull BetterPortal portal, boolean newValue) {
        cachedValidity.put(portal, newValue);
        lastChecked.put(portal, Instant.now());
    }
}
