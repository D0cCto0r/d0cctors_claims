package com.d0cctor.shibuyaclaims.registry;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ShibuyaClaims.MOD_ID);

    public static final DeferredHolder<Item, Item> CLAIM_CORE = ITEMS.register("claim_core", () ->
            new ClaimCoreItem(new Item.Properties().stacksTo(16))
    );

    public static final DeferredHolder<Item, Item> COMBUSTIBLE_DEL_CICLO = ITEMS.register("combustible_del_ciclo", () ->
            new LoreItem(new Item.Properties().stacksTo(64),
                    "item.d0cctors_claims.combustible_del_ciclo.lore1",
                    "item.d0cctors_claims.combustible_del_ciclo.lore2")
    );

    public static final DeferredHolder<Item, Item> FRAGMENTO_EXPANSION = ITEMS.register("fragmento_expansion", () ->
            new LoreItem(new Item.Properties().stacksTo(64),
                    "item.d0cctors_claims.fragmento_expansion.lore1",
                    "item.d0cctors_claims.fragmento_expansion.lore2")
    );

    private ModItems() {}
}
