package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.lauriethefish.betterportals.bukkit.util.nms.AnimationType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Deals with sending all of the packets for entity processing
 * Doesn't handle any of the updating/logic of when the packets are sent
 * NOTE: Whenever you see movement offsets/positions in this class, this class will automatically transform them by the entity tracker's matrix
 * They must be in the destination "coordinate space"
 */
public interface IEntityPacketManipulator {
    /**
     * Shows the entity represented by <code>tracker</code>, sending all necessary spawning packets.
     * This includes head rotation, metadata, etc.
     * @param tracker Tracker to show the entity of
     * @param players Players to show the entity to
     */
    void showEntity(EntityInfo tracker, Collection<Player> players);
    default void showEntity(EntityInfo tracker, Player player) {
        showEntity(tracker, Collections.singleton(player));
    }

    /**
     * Hides the entity represented by <code>tracker</code> to the given players.
     * @param tracker The entity to hide
     * @param players The players to hide the entity for
     */
    void hideEntity(EntityInfo tracker, Collection<Player> players);
    default void hideEntity(EntityInfo tracker, Player player) {
        hideEntity(tracker, Collections.singleton(player));
    }

    /**
     * Sends a relative entity movement packet to <code>players</code> for <code>tracker</code>.
     * @param tracker Entity to move
     * @param offset Movement amount, in destination coordinate space. All of the coordinates must be less than 8 blocks!
     * @param players Players to send the packet to
     */
    void sendEntityMove(EntityInfo tracker, Vector offset, Collection<Player> players);

    /**
     * Sends a move & look packet to <code>players</code> for <code>tracker</code>.
     * This <i>must</i> be used instead of two separate move then look packets, as otherwise it'll break on newer versions.
     * @param tracker Entity to move/look
     * @param movementOffset Movement amount, in destination coordinate space. All of the coordinates must be less than 8 blocks!
     * @param players Players to send the packet to
     */
    void sendEntityMoveLook(EntityInfo tracker, Vector movementOffset, Collection<Player> players);

    /**
     * Sends an entity look packet to <code>players</code> for <code>tracker</code>.
     * @param tracker Entity to update the direction of
     * @param players Players to send the packet to
     */
    void sendEntityLook(EntityInfo tracker, Collection<Player> players);

    /**
     * Sends an entity teleport packet to <code>players</code> for <code>tracker</code>.
     * This packet should be used if moving further than 8 blocks.
     * @param tracker Entity to update the position of
     * @param players Players to send the packet to
     */
    void sendEntityTeleport(EntityInfo tracker, Collection<Player> players);

    /**
     * Sends an entity head rotation packet to <code>players</code> for <code>tracker</code>.
     * NOTE: Head rotation is separate from direction.
     * @param tracker Entity to update the head rotation of
     * @param players Players to send the packet to
     */
    void sendEntityHeadRotation(EntityInfo tracker, Collection<Player> players);

    /**
     * Sends a mount packet to <code>players</code> for <code>tracker</code> to be riding <code>riding</code>.
     * @param tracker The entity to update the mounts of
     * @param riding The new mounts
     * @param players Players to send the packet to
     */
    void sendMount(EntityInfo tracker, Collection<EntityInfo> riding, Collection<Player> players);

    /**
     * Sends an entity equipment packet that will update the <code>changes</code> to <code>players</code>.
     * NOTE: The values of <code>changes</code> can be null for "no equipment"
     * NOTE: Entity equipment only applies to {@link org.bukkit.entity.LivingEntity}s.
     * @param tracker Entity to update the equipment of
     * @param changes Changes in equipment state
     * @param players Players to send the update to
     */
    void sendEntityEquipment(EntityInfo tracker, Map<EnumWrappers.ItemSlot, ItemStack> changes, Collection<Player> players);

    /**
     * Sends a metadata packet for <code>tracker</code>.
     * This packet stores whether a mob is burning, dropped item type/count, item frame type, painting type, etc..
     * Unfortunately we're forced to do a full entity metadata update every time
     * The problem is that NMS's DataWatcher may have already been updated for players already viewing the entity on the other side
     * This means that we can't use the method of only sending the changed data :/
     * @param tracker Entity to update the metadata of
     * @param players Players to send the update to
     */
    void sendMetadata(EntityInfo tracker, Collection<Player> players);

    /**
     * Sends an entity velocity update packet for <code>tracker</code> to <code>players</code>.
     * This automatically rotates the entity velocity based on the matrices in {@link EntityInfo}.
     * @param tracker Entity to update the velocity of
     * @param newVelocity New velocity of the entity, in the destination coordinate space
     * @param players Players to send the update to
     */
    void sendEntityVelocity(EntityInfo tracker, Vector newVelocity, Collection<Player> players);

    /**
     * Sends an entity animation account for <code>tracker</code> to <code>players</code>.
     * Entity animations are used for left/right hand movement, leaving beds and damaging.
     * @param tracker Entity to show the animation for
     * @param players Players to show the animation to
     * @param animationType Wrapper over the NMS animation type
     */
    void sendEntityAnimation(EntityInfo tracker, Collection<Player> players, AnimationType animationType);

    /**
     * Sends an item pickup packet for <code>tracker</code> to pick up <code>pickedUp</code>.
     * @param tracker The picker-upper
     * @param pickedUp The item that was picked up
     * @param players Players to send the packet to
     */
    void sendEntityPickupItem(EntityInfo tracker, EntityInfo pickedUp, Collection<Player> players);
}
