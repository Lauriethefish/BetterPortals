package com.lauriethefish.betterportals.bukkit.entitymanipulation;

import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

// Stores the current configuration of an entities equipment
// We cannot store bukkit's EntityEquipment, since there is no clone method
public class EntityEquipmentState {
    private ItemStack mainHand;
    private ItemStack offHand;

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;

    // Copies the items in the EntityEquipment into this class
    public EntityEquipmentState(EntityEquipment equipment)   {
        // Do not set any values if the equipment is null
        if(equipment == null)   {
            return;
        }
        
        // Load all the values from the entity equipment
        this.mainHand = equipment.getItemInMainHand();
        this.offHand = equipment.getItemInOffHand();
        this.helmet = equipment.getHelmet();
        this.chestplate = equipment.getChestplate();
        this.leggings = equipment.getLeggings();
        this.boots = equipment.getBoots();
    }

    // Compares each item of the equipment with another, checks for null values
    public boolean equals(EntityEquipmentState other)    {
        if(other == null)   {return false;}
        
        return compareItemStacks(other.mainHand, mainHand) &&
        compareItemStacks(other.offHand, offHand) &&
        compareItemStacks(other.helmet, helmet) &&
        compareItemStacks(other.chestplate, chestplate) &&
        compareItemStacks(other.leggings, leggings) &&
        compareItemStacks(other.boots, boots);
    }

    // Deals with null checking when comparing two ItemStacks
    private boolean compareItemStacks(ItemStack a, ItemStack b) {
        if(a == null || b == null)  {
            return a == b;
        }   else    {
            return a.equals(b);
        }
    }
}