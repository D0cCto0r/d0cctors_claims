package com.d0cctor.shibuyaclaims.gui;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.events.ClaimEvents;
import com.d0cctor.shibuyaclaims.network.ClaimActionPayload;
import com.d0cctor.shibuyaclaims.network.OpenClaimScreenPayload;
import com.d0cctor.shibuyaclaims.network.OpenMembersScreenPayload;
import com.d0cctor.shibuyaclaims.registry.ModItems;
import com.d0cctor.shibuyaclaims.registry.ClaimCoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ClaimGui {
    private static final long FUEL_HOURS_PER_ITEM = 24L;

    // Costo para pasar al próximo nivel.
    // Índice = nivel actual. Ej: nivel 1 -> 2 cuesta 1 fragmento.
    private static final int[] UPGRADE_COSTS = {0, 1, 2, 4, 6, 8, 12, 16};

    private ClaimGui() {}

    public static void open(ServerPlayer player, ClaimRecord claim) {
        PacketDistributor.sendToPlayer(player, toPayload(player, claim));
    }

    public static void openMembers(ServerPlayer player, ClaimRecord claim) {
        PacketDistributor.sendToPlayer(player, toMembersPayload(player, claim));
    }

    private static OpenClaimScreenPayload toPayload(ServerPlayer player, ClaimRecord claim) {
        return new OpenClaimScreenPayload(
                claim.dimension.toString(),
                claim.pos.getX(),
                claim.pos.getY(),
                claim.pos.getZ(),
                claim.ownerName == null ? "?" : claim.ownerName,
                claim.isActive(),
                ClaimEvents.formatFuel(claim),
                claim.level,
                claim.radius,
                claim.trusted.size(),
                ClaimEvents.isPreviewEnabled(player, claim),
                claim.coreHealth,
                ClaimRecord.MAX_CORE_HEALTH
        );
    }

    private static OpenMembersScreenPayload toMembersPayload(ServerPlayer player, ClaimRecord claim) {
        List<String> inviteNames = new ArrayList<>();
        List<String> inviteIds = new ArrayList<>();
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            if (claim.trusted.contains(other.getUUID())) continue;
            inviteNames.add(other.getGameProfile().getName());
            inviteIds.add(other.getUUID().toString());
        }

        List<String> trustedNames = new ArrayList<>();
        List<String> trustedIds = new ArrayList<>();
        for (UUID uuid : claim.trusted) {
            trustedNames.add(resolveName(player, uuid));
            trustedIds.add(uuid.toString());
        }

        return new OpenMembersScreenPayload(
                claim.dimension.toString(),
                claim.pos.getX(),
                claim.pos.getY(),
                claim.pos.getZ(),
                inviteNames,
                inviteIds,
                trustedNames,
                trustedIds
        );
    }

    public static void handleAction(ServerPlayer player, ClaimActionPayload payload) {
        Optional<ClaimRecord> optional = getClaim(player, payload.dimension(), new BlockPos(payload.x(), payload.y(), payload.z()));
        if (optional.isEmpty()) {
            player.sendSystemMessage(Component.literal("✦ Este nexo ya no existe.").withStyle(ChatFormatting.RED));
            return;
        }

        ClaimRecord claim = optional.get();
        if (!claim.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            sound(player, SoundEvents.VILLAGER_NO, 0.95F, 0.75F);
            player.sendSystemMessage(Component.literal("✦ Solo el dueño puede usar esta interfaz.").withStyle(ChatFormatting.RED));
            return;
        }

        ClaimSavedData data = ClaimSavedData.get(player.server);

        if (payload.action() == ClaimActionPayload.DEACTIVATE) {
            if (!claim.owner.equals(player.getUUID())) {
                sound(player, SoundEvents.VILLAGER_NO, 0.95F, 0.75F);
                player.sendSystemMessage(Component.literal("✦ Solo el dueño puede desactivar este Núcleo.").withStyle(ChatFormatting.RED));
                return;
            }

            ServerLevel level = getLevel(player, claim);
            data.removeClaim(claim);
            if (level != null) {
                ClaimCoreItem.removeVisual(level, claim.pos);
                level.playSound(null, claim.pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 0.72F);
            }

            ItemStack returned = new ItemStack(ModItems.CLAIM_CORE.get());
            if (!player.getInventory().add(returned)) {
                player.drop(returned, false);
            }

            player.sendSystemMessage(Component.literal("✦ Núcleo desactivado. El nexo quedó removido y recuperaste el item.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        if (payload.action() == ClaimActionPayload.ADD_FUEL) {
            if (!consumeOne(player, ModItems.COMBUSTIBLE_DEL_CICLO.get())) {
                sound(player, SoundEvents.VILLAGER_NO, 0.95F, 0.75F);
                player.sendSystemMessage(Component.literal("✦ Necesitás Energía del Vacío en tu inventario.").withStyle(ChatFormatting.RED));
                open(player, claim);
                return;
            }
            claim.addFuelHours(FUEL_HOURS_PER_ITEM);
            data.setDirty();
            sound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.15F, 1.25F);
            player.sendSystemMessage(Component.literal("✦ Agregaste energía. Tiempo: " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.GREEN));
            open(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.UPGRADE) {
            if (claim.level >= ClaimRecord.MAX_LEVEL) {
                sound(player, SoundEvents.VILLAGER_NO, 0.95F, 0.75F);
                player.sendSystemMessage(Component.literal("✦ Este nexo ya está al nivel máximo. Radio: " + ClaimRecord.MAX_RADIUS + " bloques.").withStyle(ChatFormatting.YELLOW));
                open(player, claim);
                return;
            }

            int cost = getUpgradeCost(claim.level);
            if (!consumeItems(player, ModItems.FRAGMENTO_EXPANSION.get(), cost)) {
                sound(player, SoundEvents.VILLAGER_NO, 0.95F, 0.75F);
                player.sendSystemMessage(Component.literal("✦ Necesitás " + cost + " Fragmento(s) del Núcleo para mejorar al nivel " + (claim.level + 1) + ".").withStyle(ChatFormatting.RED));
                open(player, claim);
                return;
            }

            int oldRadius = claim.radius;
            claim.upgrade();
            data.setDirty();
            sound(player, SoundEvents.BEACON_ACTIVATE, 1.1F, 1.55F);
            player.sendSystemMessage(Component.literal("✦ Nexo mejorado a nivel " + claim.level + ". Radio: " + oldRadius + " → " + claim.radius + " bloques.").withStyle(ChatFormatting.LIGHT_PURPLE));
            open(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.TOGGLE_PREVIEW) {
            boolean enabled = ClaimEvents.togglePreview(player, claim);
            sound(player, enabled ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, 0.9F, enabled ? 1.35F : 0.75F);
            player.sendSystemMessage(Component.literal(enabled ? "✦ Preview ACTIVADA." : "✦ Preview DESACTIVADA.").withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            open(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.INFO) {
            sendInfo(player, claim);
            open(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.OPEN_MEMBERS) {
            openMembers(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.LIST_MEMBERS) {
            openMembers(player, claim);
            return;
        }


        if (payload.action() == ClaimActionPayload.BACK_TO_MAIN) {
            open(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.TRUST_TARGET) {
            try {
                UUID target = UUID.fromString(payload.target());
                if (target.equals(player.getUUID())) {
                    player.sendSystemMessage(Component.literal("✦ No podés invitarte a vos mismo.").withStyle(ChatFormatting.RED));
                } else if (claim.trusted.add(target)) {
                    data.setDirty();
                    player.sendSystemMessage(Component.literal("✦ Miembro autorizado agregado: " + resolveName(player, target)).withStyle(ChatFormatting.GREEN));
                }
            } catch (Exception ignored) {}
            openMembers(player, claim);
            return;
        }

        if (payload.action() == ClaimActionPayload.UNTRUST_TARGET) {
            try {
                UUID target = UUID.fromString(payload.target());
                if (claim.trusted.remove(target)) {
                    data.setDirty();
                    player.sendSystemMessage(Component.literal("✦ Miembro removido: " + resolveName(player, target)).withStyle(ChatFormatting.YELLOW));
                }
            } catch (Exception ignored) {}
            openMembers(player, claim);
        }
    }

    private static ServerLevel getLevel(ServerPlayer player, ClaimRecord claim) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, claim.dimension);
        return player.server.getLevel(key);
    }

    private static void sound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.serverLevel().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static Optional<ClaimRecord> getClaim(ServerPlayer player, String dimension, BlockPos corePos) {
        ResourceLocation dimId;
        try {
            dimId = ResourceLocation.parse(dimension);
        } catch (Exception e) {
            return Optional.empty();
        }

        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimId);
        ServerLevel level = player.server.getLevel(key);
        if (level == null) return Optional.empty();
        return ClaimSavedData.get(player.server).findByCore(level, corePos);
    }

    private static int getUpgradeCost(int currentLevel) {
        if (currentLevel < 1) return 1;
        if (currentLevel >= UPGRADE_COSTS.length) return UPGRADE_COSTS[UPGRADE_COSTS.length - 1];
        return UPGRADE_COSTS[currentLevel];
    }

    private static int countItems(ServerPlayer player, Item item) {
        if (player.isCreative()) return Integer.MAX_VALUE;

        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean consumeItems(ServerPlayer player, Item item, int amount) {
        if (amount <= 0) return true;
        if (player.isCreative()) return true;
        if (countItems(player, item) < amount) return false;

        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(item)) continue;

            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
        }

        inv.setChanged();
        return true;
    }

    private static boolean consumeOne(ServerPlayer player, Item item) {
        if (player.isCreative()) return true;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inv.setChanged();
                return true;
            }
        }
        return false;
    }

    private static String resolveName(ServerPlayer player, UUID uuid) {
        ServerPlayer online = player.server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getGameProfile().getName();
        String s = uuid.toString();
        return s.substring(0, Math.min(8, s.length()));
    }

    private static void sendInfo(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Nexo de Protección").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Dueño: " + claim.ownerName).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Estado: " + (claim.isActive() ? "Activo" : "Sin energía")).withStyle(claim.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED));
        player.sendSystemMessage(Component.literal("Energía: " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Nivel: " + claim.level + "/" + ClaimRecord.MAX_LEVEL + " | Radio: " + claim.radius + "/" + ClaimRecord.MAX_RADIUS).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("Miembros autorizados: " + claim.trusted.size()).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal("Comandos: /claim trust <jugador>, /claim untrust <jugador>, /claim preview").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void sendMembersList(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Miembros autorizados").withStyle(ChatFormatting.AQUA));
        if (claim.trusted.isEmpty()) {
            player.sendSystemMessage(Component.literal("No hay miembros autorizados.").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (UUID uuid : claim.trusted) {
            player.sendSystemMessage(Component.literal("- " + resolveName(player, uuid)).withStyle(ChatFormatting.WHITE));
        }
    }
}
