package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.events.IEventRegistrar;
import com.lauriethefish.betterportals.bukkit.portal.IPortal;
import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
import com.lauriethefish.betterportals.shared.logging.Logger;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Singleton
public class EntityTrackingManager implements IEntityTrackingManager, Listener {
    private final Logger logger;
    private final EntityTrackerFactory entityTrackerFactory;
    private final Map<IPortal, Map<Entity, IEntityTracker>> trackersByPortal = new HashMap<>(); // Used for separating trackers based on portal
    private final Map<Entity, Collection<IEntityTracker>> trackersByEntity = new HashMap<>(); // Used for calling events when the tracked entities perform animations

    /**
     * Bukkit doesn't allow us to get the hand used from {@link PlayerAnimationEvent}, so we get it from {@link PlayerInteractEvent} then store it for later.
     */
    private final Map<Entity, EquipmentSlot> lastHandUsed = new HashMap<>();

    @Inject
    public EntityTrackingManager(Logger logger, IEventRegistrar eventRegistrar, EntityTrackerFactory entityTrackerFactory) {
        this.logger = logger;
        this.entityTrackerFactory = entityTrackerFactory;
        eventRegistrar.register(this);
    }

    @Override
    public void setTracking(Entity entity, IPortal portal, Player player) {
        // Get the tracker from the map, adding a new one if necessary
        Map<Entity, IEntityTracker> portalMap = trackersByPortal.computeIfAbsent(portal, k -> new HashMap<>());
        IEntityTracker tracker = portalMap.computeIfAbsent(entity, k -> {
            IEntityTracker newTracker = entityTrackerFactory.create(entity, portal);
            trackersByEntity.computeIfAbsent(entity, l -> new ArrayList<>()).add(newTracker);

            return newTracker;
        });

        tracker.addTracking(player);
    }

    @Override
    public void setNoLongerTracking(Entity entity, IPortal portal, Player player, boolean sendPackets) {
        Map<Entity, IEntityTracker> portalMap = trackersByPortal.get(portal);

        IEntityTracker tracker = portalMap.get(entity);
        if(tracker == null) {
            logger.fine("Attempted to remove entity tracker that didn't exist. This should never happen!");
            return;
        }
        tracker.removeTracking(player, sendPackets);

        // If no players are tracking this entity, remove it from the map
        if(tracker.getTrackingPlayerCount() == 0) {
            // Make sure to remove the per-entity map if it is empty
            Collection<IEntityTracker> entityList = trackersByEntity.get(entity);
            entityList.remove(tracker);
            if(entityList.size() == 0) {
                trackersByEntity.remove(entity);
            }

            portalMap.remove(entity);
            if(portalMap.isEmpty()) {
                trackersByPortal.remove(portal);
            }
        }
    }

    /**
     * Performs <code>action</code> to each tracker of <code>entity</code>.
     * @param entity Entity to check for trackers
     * @param action Action to perform on the trackers
     */
    private void forEachTracker(Entity entity, Consumer<IEntityTracker> action) {
        Collection<IEntityTracker> trackers = trackersByEntity.get(entity);
        if(trackers == null) {return;}

        trackers.forEach(action);
    }

    /**
     * Handles making entities turn red upon being hit
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        forEachTracker(event.getEntity(), tracker -> tracker.onAnimation(AnimationType.DAMAGE));
    }

    /**
     * Handles moving the tracker's hand when the entity moves their hand
     */
    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if(event.getAnimationType() != PlayerAnimationType.ARM_SWING) {return;}

        EquipmentSlot hand = lastHandUsed.get(event.getPlayer());
        if(hand == null) {return;}

        AnimationType type = hand == EquipmentSlot.HAND ? AnimationType.MAIN_HAND : AnimationType.OFF_HAND;
        forEachTracker(event.getPlayer(), tracker -> tracker.onAnimation(type));
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();

        forEachTracker(entity, (tracker) -> {
            Map<Entity, IEntityTracker> portalTrackers = trackersByPortal.get(tracker.getPortal());
            IEntityTracker pickedUp = portalTrackers.get(event.getItem());

            if(pickedUp != null) {
                logger.fine("Sending pickup packet");
                tracker.onPickup(pickedUp.getEntityInfo());
            }   else    {
                logger.fine("Not sending pickup packet - the item isn't viewable");
            }
        });
    }

    /**
     * Workaround for missing API
     * @see EntityTrackingManager#lastHandUsed
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if(hand != null) {
            lastHandUsed.put(event.getPlayer(), hand);
        }
    }

    @Override
    public void update() {
        trackersByPortal.values().forEach((map) -> map.values().forEach(IEntityTracker::update));
    }

    @Override
    public @Nullable IEntityTracker getTracker(IPortal portal, Entity entity) {
        Map<Entity, IEntityTracker> portalTrackers = trackersByPortal.get(portal);
        if(portalTrackers == null) {return null;}

        return portalTrackers.get(entity);
    }
}
