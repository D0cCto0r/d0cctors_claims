package com.d0cctor.shibuyaclaims.entity;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.gui.ClaimGui;
import com.d0cctor.shibuyaclaims.registry.ClaimCoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class ClaimCoreEntity extends Entity {
    private BlockPos corePos = BlockPos.ZERO;

    public ClaimCoreEntity(EntityType<? extends ClaimCoreEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ClaimCoreEntity(EntityType<? extends ClaimCoreEntity> type, Level level, BlockPos corePos) {
        this(type, level);
        this.corePos = corePos;
        this.setPos(corePos.getX() + 0.5D, corePos.getY() + 0.05D, corePos.getZ() + 0.5D);
    }

    public BlockPos getCorePos() {
        return corePos;
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos;
        this.setPos(corePos.getX() + 0.5D, corePos.getY() + 0.05D, corePos.getZ() + 0.5D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.corePos = BlockPos.of(tag.getLong("corePos"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("corePos", this.corePos.asLong());
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);

        if (!level().isClientSide) {
            this.setPos(corePos.getX() + 0.5D, corePos.getY() + 0.05D, corePos.getZ() + 0.5D);

            if (tickCount % 20 == 0 && level() instanceof ServerLevel serverLevel) {
                Optional<ClaimRecord> optional = ClaimSavedData.get(serverLevel.getServer()).findByCore(serverLevel, corePos);
                if (optional.isEmpty()) {
                    discard();
                    return;
                }

                ClaimRecord claim = optional.get();
                long now = System.currentTimeMillis();
                if (claim.coreHealth > 0
                        && claim.coreHealth < ClaimRecord.MAX_CORE_HEALTH
                        && now - claim.lastDamagedMs >= ClaimRecord.REGEN_DELAY_MS) {
                    claim.coreHealth = Math.min(ClaimRecord.MAX_CORE_HEALTH, claim.coreHealth + ClaimRecord.REGEN_HEALTH_PER_SECOND);
                    ClaimSavedData.get(serverLevel.getServer()).setDirty();

                    if (claim.coreHealth % 50 == 0 || claim.coreHealth == ClaimRecord.MAX_CORE_HEALTH) {
                        serverLevel.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35F, 1.6F);
                    }
                }
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        if (!(level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        Optional<ClaimRecord> optional = ClaimSavedData.get(level.getServer()).findByCore(level, corePos);
        if (optional.isEmpty()) {
            serverPlayer.sendSystemMessage(Component.literal("✦ Este núcleo no tiene claim asociado.").withStyle(ChatFormatting.RED));
            discard();
            return InteractionResult.SUCCESS;
        }

        ClaimRecord claim = optional.get();
        if (!claim.owner.equals(serverPlayer.getUUID())) {
            serverPlayer.displayClientMessage(Component.literal("✦ Solo el dueño puede interactuar con este Núcleo.").withStyle(ChatFormatting.RED), true);
            level.playSound(null, blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.9F, 0.8F);
            return InteractionResult.SUCCESS;
        }

        ClaimGui.open(serverPlayer, claim);
        level.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 1.35F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel level)) return false;
        if (!(source.getEntity() instanceof ServerPlayer player)) return false;

        Optional<ClaimRecord> optional = ClaimSavedData.get(level.getServer()).findByCore(level, corePos);
        if (optional.isEmpty()) {
            discard();
            return false;
        }

        ClaimRecord claim = optional.get();

        // Solo jugadores que NO pertenezcan al núcleo pueden dañarlo.
        // Dueño y miembros autorizados no pueden romperlo por accidente.
        if (claim.canBuild(player.getUUID()) && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("✦ No podés dañar tu propio Núcleo o uno donde estás autorizado.").withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7F, 1.2F);
            return false;
        }

        float cooldown = player.getAttackStrengthScale(0.5F);
        if (cooldown < 0.75F) {
            player.displayClientMessage(Component.literal("✦ Golpe demasiado débil.").withStyle(ChatFormatting.DARK_GRAY), true);
            return false;
        }

        // Usamos el daño real que Minecraft ya calculó para el golpe.
        // Esto hace que funcionen espada/hacha, Sharpness/Filo, Fuerza, críticos y otros modificadores.
        int damage = Math.max(1, (int)Math.ceil(amount));

        claim.coreHealth = Math.max(0, claim.coreHealth - damage);
        claim.lastDamagedMs = System.currentTimeMillis();
        ClaimSavedData data = ClaimSavedData.get(level.getServer());
        data.setDirty();

        level.playSound(null, blockPosition(), SoundEvents.ANVIL_HIT, SoundSource.BLOCKS, 1.0F, damage >= 12 ? 0.55F : 0.8F);
        level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, damage >= 12 ? 0.65F : 0.35F, 1.4F);
        level.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.55F, 0.55F + (cooldown * 0.25F));

        if (claim.coreHealth <= 0) {
            data.removeClaim(claim);
            ClaimCoreItem.removeVisual(level, corePos);

            level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5F, 0.55F);
            level.playSound(null, blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.BLOCKS, 0.7F, 1.15F);

            Component message = Component.literal("✦ ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" derrumbó el Núcleo de ").withStyle(ChatFormatting.RED))
                    .append(Component.literal(claim.ownerName == null ? "?" : claim.ownerName).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(". El nexo quedó desprotegido.").withStyle(ChatFormatting.RED));

            level.getServer().getPlayerList().broadcastSystemMessage(message, false);
            discard();
        } else {
            player.displayClientMessage(Component.literal("✦ Daño: " + damage + " | Vida del Núcleo: " + claim.coreHealth + "/" + ClaimRecord.MAX_CORE_HEALTH).withStyle(ChatFormatting.YELLOW), true);
        }

        return true;
    }

    private void sendInfo(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Nexo de Protección").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Dueño: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.ownerName).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Estado: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.isActive() ? "Activo" : "Sin energía").withStyle(claim.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED)));
        player.sendSystemMessage(Component.literal("Energía: ").withStyle(ChatFormatting.GRAY).append(Component.literal(com.d0cctor.shibuyaclaims.events.ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.YELLOW)));
        player.sendSystemMessage(Component.literal("Vida: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.coreHealth + "/" + ClaimRecord.MAX_CORE_HEALTH).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Nivel: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(claim.level)).withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.sendSystemMessage(Component.literal("Radio: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.radius + " bloques").withStyle(ChatFormatting.WHITE)));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }
}
