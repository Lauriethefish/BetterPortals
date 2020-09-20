package com.lauriethefish.betterportals.events;

import java.util.HashMap;
import java.util.Map;

import com.lauriethefish.betterportals.BetterPortals;
import com.lauriethefish.betterportals.PlayerData;
import com.lauriethefish.betterportals.entitymanipulation.PlayerEntityManipulator;
import com.lauriethefish.betterportals.entitymanipulation.PlayerViewableEntity;

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

public class EntityReplicationEvents implements Listener    {
    private BetterPortals pl;
    private Map<Entity, EquipmentSlot> lastHandUsed = new HashMap<>();

    public EntityReplicationEvents(BetterPortals pl)    {
        this.pl = pl;
    }

    // Finds which players have the given entity currently visible through a portal
    private Map<PlayerViewableEntity, PlayerEntityManipulator> findViewingPlayers(Entity entity) {
        Map<PlayerViewableEntity, PlayerEntityManipulator> result = new HashMap<>();

        for(PlayerData player : pl.players.values()) {
            PlayerEntityManipulator manipulator = player.entityManipulator;
            // If this entity is in the replicated entities, add it to the map
            PlayerViewableEntity playerEntity = manipulator.getViewedEntity(entity);
            if(playerEntity != null)    {
                result.put(playerEntity, manipulator);
            }
        }
        return result;
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        // Right now there is only 1 animation type, so this code is sort of useless, but there may be more in the future
        if(event.getAnimationType() != PlayerAnimationType.ARM_SWING) {return;}

        Player player = event.getPlayer();

        EquipmentSlot handUsed = lastHandUsed.get(player);
        int animationType = handUsed == EquipmentSlot.HAND ? 0 : 3;
        for(Map.Entry<PlayerViewableEntity, PlayerEntityManipulator> entry : findViewingPlayers(player).entrySet())  {
            entry.getValue().sendAnimationPacket(entry.getKey(), animationType);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Store which hand the player last used, since PlayerAnimationEvent doesn't have this parameter
        EquipmentSlot hand = event.getHand();
        if(hand != null)    {
            lastHandUsed.put(event.getPlayer(), event.getHand());
        }
    }

    // Sends a packet to make replicated entities flash red when damaged
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        for(Map.Entry<PlayerViewableEntity, PlayerEntityManipulator> entry : findViewingPlayers(event.getEntity()).entrySet())  {
            entry.getValue().sendAnimationPacket(entry.getKey(), 1);
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        for(Map.Entry<PlayerViewableEntity, PlayerEntityManipulator> entry : findViewingPlayers(event.getEntity()).entrySet())  {
            // Find if the item is also visible through the portal
            PlayerEntityManipulator manipulator = entry.getValue();
            PlayerViewableEntity item = manipulator.getViewedEntity(event.getItem());
            if(item == null) {continue;}
            // Send a packet that displays the animation of the entity picking up the item
            manipulator.sendPickupItemPacket(entry.getKey(), item);
        }
    }
}
