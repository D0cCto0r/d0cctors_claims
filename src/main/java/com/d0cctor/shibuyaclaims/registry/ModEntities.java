package com.d0cctor.shibuyaclaims.registry;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import com.d0cctor.shibuyaclaims.entity.ClaimCoreEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ShibuyaClaims.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ClaimCoreEntity>> CLAIM_CORE_ENTITY = ENTITIES.register("claim_core_entity", () ->
            EntityType.Builder.<ClaimCoreEntity>of(ClaimCoreEntity::new, MobCategory.MISC)
                    .sized(1.6F, 2.4F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("claim_core_entity")
    );

    private ModEntities() {}
}
