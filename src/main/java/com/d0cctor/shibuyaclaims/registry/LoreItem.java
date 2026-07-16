package com.d0cctor.shibuyaclaims.registry;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LoreItem extends Item {
    private final String[] loreKeys;

    public LoreItem(Properties properties, String... loreKeys) {
        super(properties);
        this.loreKeys = loreKeys;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String key : loreKeys) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
