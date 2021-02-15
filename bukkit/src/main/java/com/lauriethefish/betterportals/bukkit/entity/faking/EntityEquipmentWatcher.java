package com.lauriethefish.betterportals.bukkit.entity.faking;

import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Bukkit {@link EntityEquipment} is live and isn't a captured state.
 * This allows us to easily check for changes and convert it into the map used in packets
 */
public class EntityEquipmentWatcher {
    private final LivingEntity entity;

    private ItemStack mainHand;
    private ItemStack offHand;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;

    public EntityEquipmentWatcher(LivingEntity entity) {
        this.entity = entity;
    }

    /**
     * Checks for any changed pieces of equipment and returns a map of them
     * Will return every slot the first time
     * The values may be null
     * @return A map of which slots changed, and their new value
     */
    public Map<EnumWrappers.ItemSlot, ItemStack> checkForChanges() {
        Map<EnumWrappers.ItemSlot, ItemStack> result = new HashMap<>();

        EntityEquipment current = entity.getEquipment();
        if(current == null) {return result;}

        // Compare each item of equipment, and add it to the map if any changes are found
        if(isStateDifferent(mainHand, current.getItemInMainHand())) {
            mainHand = current.getItemInMainHand();
            result.put(EnumWrappers.ItemSlot.MAINHAND, mainHand);
        }
        if(isStateDifferent(offHand, current.getItemInOffHand())) {
            offHand = current.getItemInOffHand();
            result.put(EnumWrappers.ItemSlot.OFFHAND, offHand);
        }
        if(isStateDifferent(helmet, current.getHelmet())) {
            helmet = current.getHelmet();
            result.put(EnumWrappers.ItemSlot.HEAD, helmet);
        }
        if(isStateDifferent(chestplate, current.getChestplate())) {
            chestplate = current.getChestplate();
            result.put(EnumWrappers.ItemSlot.CHEST, chestplate);
        }
        if(isStateDifferent(leggings, current.getLeggings())) {
            leggings = current.getLeggings();
            result.put(EnumWrappers.ItemSlot.LEGS, leggings);
        }
        if(isStateDifferent(boots, current.getBoots())) {
            boots = current.getBoots();
            result.put(EnumWrappers.ItemSlot.FEET, boots);
        }

        return result;
    }

    private boolean isStateDifferent(ItemStack a, ItemStack b) {
        if(a == null || b == null) {
            return a != b;
        }   else    {
            return !a.equals(b);
        }
    }
}
