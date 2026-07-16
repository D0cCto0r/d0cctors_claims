package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.registry.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClaimClientEvents {
    private ClaimClientEvents() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLAIM_CORE_ENTITY.get(), ClaimCoreEntityRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClaimCoreModel.LAYER_LOCATION, ClaimCoreModel::createBodyLayer);
    }
}
