package com.d0cctor.shibuyaclaims.registry;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.entity.ClaimCoreEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ClaimCoreItem extends Item {
    public ClaimCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos corePos = context.getClickedPos().relative(context.getClickedFace());

        ClaimSavedData data = ClaimSavedData.get(serverLevel.getServer());
        ClaimRecord candidate = new ClaimRecord(serverLevel.dimension().location(), corePos, player.getUUID(), player.getGameProfile().getName());

        if (data.wouldOverlap(candidate)) {
            player.displayClientMessage(Component.literal("✦ Este Núcleo está demasiado cerca de otro claim.").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        data.addClaim(candidate);
        ensureVisual(serverLevel, corePos);

        if (!player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        player.sendSystemMessage(Component.literal("✦ Claim creado. Radio: " + candidate.radius + " bloques. Energía inicial: 0h. Vida del núcleo: " + candidate.coreHealth).withStyle(ChatFormatting.AQUA));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static void ensureVisual(ServerLevel level, BlockPos corePos) {
        cleanupLegacy(level, corePos);

        AABB area = new AABB(corePos).inflate(3.0D);
        boolean exists = !level.getEntitiesOfClass(ClaimCoreEntity.class, area, e -> e.getCorePos().equals(corePos)).isEmpty();

        if (!exists) {
            ClaimCoreEntity entity = new ClaimCoreEntity(ModEntities.CLAIM_CORE_ENTITY.get(), level, corePos);
            level.addFreshEntity(entity);
        }
    }

    public static void removeVisual(ServerLevel level, BlockPos corePos) {
        AABB area = new AABB(corePos).inflate(3.0D);

        for (ClaimCoreEntity entity : level.getEntitiesOfClass(ClaimCoreEntity.class, area, e -> e.getCorePos().equals(corePos))) {
            entity.discard();
        }

        cleanupLegacy(level, corePos);
    }

    public static void tickVisual(ServerLevel level, BlockPos corePos, int tick) {
        ensureVisual(level, corePos);
    }

    private static void cleanupLegacy(ServerLevel level, BlockPos corePos) {
        AABB area = new AABB(corePos).inflate(3.0D);

        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, area)) {
            if (hasOldCorePosTag(stand, corePos)) stand.discard();
        }

        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (hasOldCorePosTag(item, corePos)) item.discard();
        }

        for (EndCrystal crystal : level.getEntitiesOfClass(EndCrystal.class, area)) {
            if (hasOldCorePosTag(crystal, corePos) || crystal.getTags().contains("d0cctors_claim_core_crystal")) crystal.discard();
        }
    }

    private static boolean hasOldCorePosTag(Entity entity, BlockPos corePos) {
        String prefix = "d0cctors_claim_core_pos:";
        String expected = prefix + corePos.getX() + "," + corePos.getY() + "," + corePos.getZ();

        for (String tag : entity.getTags()) {
            if (tag.equals(expected)) return true;
        }

        return false;
    }
}
