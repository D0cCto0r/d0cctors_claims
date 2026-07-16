package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import com.d0cctor.shibuyaclaims.entity.ClaimCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class ClaimCoreModel extends EntityModel<ClaimCoreEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ShibuyaClaims.MOD_ID, "claim_core_entity"),
            "main"
    );

    private final ModelPart nucleo;

    public ClaimCoreModel(ModelPart root) {
        this.nucleo = root.getChild("Nucleo");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("Nucleo",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-6.0F, -7.0F, -6.0F, 11.0F, 11.0F, 11.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0)
                        .addBox(-6.5F, -7.5F, -6.5F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 17.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(ClaimCoreEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.nucleo.yRot = ageInTicks * 0.08F;
        this.nucleo.xRot = 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.nucleo.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
