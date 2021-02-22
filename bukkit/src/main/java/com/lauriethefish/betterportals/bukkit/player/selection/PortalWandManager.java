package com.lauriethefish.betterportals.bukkit.player.selection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lauriethefish.betterportals.bukkit.config.MessageConfig;
import com.lauriethefish.betterportals.bukkit.util.nms.NBTTagUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PortalWandManager implements IPortalWandManager    {
    private static final String PORTAL_WAND_TAG = "portalWand";

    @Getter private ItemStack portalWand;

    @Inject
    public PortalWandManager(MessageConfig messageConfig) {
        portalWand = new ItemStack(Material.BLAZE_ROD);

        ItemMeta meta = portalWand.getItemMeta();
        assert meta != null;
        meta.setDisplayName(messageConfig.getPortalWandName());

        portalWand.setItemMeta(meta);
        // Portal wand checking is done with an NBT tag
        portalWand = NBTTagUtil.addMarkerTag(portalWand, PORTAL_WAND_TAG);
    }

    @Override
    public boolean isPortalWand(@NotNull ItemStack item) {
        return NBTTagUtil.hasMarkerTag(item, PORTAL_WAND_TAG);
    }
}
