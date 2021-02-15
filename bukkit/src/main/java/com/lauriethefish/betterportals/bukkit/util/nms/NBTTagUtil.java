package com.lauriethefish.betterportals.bukkit.util.nms;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.lauriethefish.betterportals.shared.util.ReflectionUtil.*;
import static com.lauriethefish.betterportals.bukkit.util.nms.MinecraftReflectionUtil.*;

/**
 * Used to add simple marker tags to items, for example, the portal wand
 */
public class NBTTagUtil {
    private static final String MARKER_PREFIX = "BetterPortals_marker_";
    private static final String MARKER_VALUE = "marked";
    private static final Class<?> CRAFT_ITEM_STACK = findCraftBukkitClass("inventory.CraftItemStack");

    /**
     * Adds a marker NBT tag to <code>item</code>.
     * @param item Item to add the tag to
     * @param name Name of the tag
     * @return A new {@link ItemStack} with the tag. (original is unmodified)
     */
    @NotNull
    public static ItemStack addMarkerTag(@NotNull ItemStack item, @NotNull String name) {
        Object nmsItem = getNMSItemStack(item);

        // Get the NBT tag, or create one if the item doesn't have one
        Object itemTag = ((boolean) runMethod(nmsItem, "hasTag")) ? runMethod(nmsItem, "getTag") : newInstance(findNMSClass("NBTTagCompound"));
        Object stringValue = newInstance(findNMSClass("NBTTagString"), new Class[]{String.class}, MARKER_VALUE);

        runMethod(itemTag, "set", new Class[]{String.class, findNMSClass("NBTBase")}, MARKER_PREFIX + name, stringValue); // Set the value

        return getBukkitItemStack(nmsItem);
    }

    /**
     * Checks if <code>item</code> has a marker tag with <code>name</code>.
     * @param item The item to check
     * @param name The name of the NBT marker tag
     * @return Whether it has the tag
     */
    public static boolean hasMarkerTag(@NotNull ItemStack item, @NotNull String name)	{
        Object nmsItem = getNMSItemStack(item);

        if(!(boolean) runMethod(nmsItem, "hasTag")) {return false;} // Return null if it has no NBT data
        Object itemTag = runMethod(nmsItem, "getTag"); // Otherwise, get the item's NBT tag

        String value = (String) runMethod(itemTag, "getString", new Class[]{String.class}, MARKER_PREFIX + name);

        return MARKER_VALUE.equals(value); // Return the value of the key
    }

    @NotNull
    private static Object getNMSItemStack(@NotNull ItemStack item) {
        return runMethod(null, CRAFT_ITEM_STACK, "asNMSCopy", new Class[]{ItemStack.class}, new Object[]{item});
    }

    @NotNull
    private static ItemStack getBukkitItemStack(@NotNull Object nmsItem) {
        return (ItemStack) runMethod(null, CRAFT_ITEM_STACK, "asBukkitCopy", new Class[]{findNMSClass("ItemStack")}, new Object[]{nmsItem});
    }
}
