package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import com.d0cctor.shibuyaclaims.entity.ClaimCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ClaimCoreEntityRenderer extends EntityRenderer<ClaimCoreEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ShibuyaClaims.MOD_ID, "textures/entity/claim_core.png");

    private final ClaimCoreModel model;

    public ClaimCoreEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ClaimCoreModel(context.bakeLayer(ClaimCoreModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ClaimCoreEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTick;
        float bob = (float)Math.sin(time / 12.0F) * 0.10F;

        poseStack.translate(0.0D, -0.45D + bob, 0.0D);
        poseStack.scale(1.75F, 1.75F, 1.75F);

        this.model.setupAnim(entity, 0.0F, 0.0F, time, 0.0F, 0.0F);
        this.model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ClaimCoreEntity entity) {
        return TEXTURE;
    }
}
