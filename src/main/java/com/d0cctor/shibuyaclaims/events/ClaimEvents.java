package com.d0cctor.shibuyaclaims.events;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.commands.ClaimCommands;
import com.d0cctor.shibuyaclaims.registry.ModBlocks;
import com.d0cctor.shibuyaclaims.gui.ClaimGui;
import com.d0cctor.shibuyaclaims.registry.ModItems;
import com.d0cctor.shibuyaclaims.registry.ClaimCoreItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClaimEvents {
    private static final long FUEL_HOURS_PER_ITEM = 24L;
    private static final Map<UUID, PreviewTarget> ACTIVE_PREVIEWS = new HashMap<>();
    private static final java.util.Set<UUID> BUILD_TEST_MODE = new java.util.HashSet<>();

    @SubscribeEvent
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();

        // Solo bloqueamos colocación de jugadores dentro de claims ajenos.
        if (entity instanceof ServerPlayer player) {
            ClaimSavedData data = ClaimSavedData.get(level.getServer());
            Optional<ClaimRecord> claimHere = data.findClaimAt(level, event.getPos());
            if (claimHere.isPresent() && claimHere.get().isActive() && !canPlayerBuild(player, claimHere.get())) {
                player.displayClientMessage(Component.literal("✦ No podés colocar bloques dentro de este nexo.").withStyle(ChatFormatting.RED), true);
                event.setCanceled(true);
                return;
            }
        }

        // Desde 0.2.1 el Núcleo se crea como entidad desde ClaimCoreItem.
        // Este bloque queda solo como modelo visual/compatibilidad.
        Block placed = event.getPlacedBlock().getBlock();
        if (placed == ModBlocks.CLAIM_CORE.get()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player playerRaw = event.getPlayer();
        if (!(playerRaw instanceof ServerPlayer player)) return;

        ClaimSavedData data = ClaimSavedData.get(level.getServer());

        // Si rompe el núcleo, solo el dueño puede quitarlo y se elimina el claim.
        if (level.getBlockState(event.getPos()).getBlock() == ModBlocks.CLAIM_CORE.get()) {
            Optional<ClaimRecord> coreClaim = data.findByCore(level, event.getPos());
            if (coreClaim.isPresent()) {
                ClaimRecord claim = coreClaim.get();
                if (!claim.owner.equals(player.getUUID())) {
                    player.displayClientMessage(Component.literal("✦ Solo el dueño puede romper este Núcleo.").withStyle(ChatFormatting.RED), true);
                    event.setCanceled(true);
                    return;
                }
                data.removeClaim(claim);
                player.sendSystemMessage(Component.literal("✦ Nexo eliminado.").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        Optional<ClaimRecord> claimHere = data.findClaimAt(level, event.getPos());
        if (claimHere.isEmpty()) return;

        ClaimRecord claim = claimHere.get();
        if (!claim.isActive()) return;
        if (canPlayerBuild(player, claim)) return;

        player.displayClientMessage(Component.literal("✦ No podés romper bloques dentro de este nexo.").withStyle(ChatFormatting.RED), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).getBlock() != ModBlocks.CLAIM_CORE.get()) return;

        ClaimSavedData data = ClaimSavedData.get(level.getServer());
        Optional<ClaimRecord> optional = data.findByCore(level, pos);
        if (optional.isEmpty()) return;

        ClaimRecord claim = optional.get();
        if (!claim.owner.equals(player.getUUID())) {
            showInfo(player, claim);
            event.setCanceled(true);
            return;
        }

        // El dueño abre la interfaz principal del claim.
        // Las acciones de energía/mejora/preview ahora se hacen desde la GUI.
        ClaimGui.open(player, claim);
        event.setCanceled(true);
        return;
    }




    private static void showInfo(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Nexo de Protección").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Dueño: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.ownerName).withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Estado: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.isActive() ? "Activo" : "Sin energía").withStyle(claim.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED)));
        player.sendSystemMessage(Component.literal("Energía: ").withStyle(ChatFormatting.GRAY).append(Component.literal(formatFuel(claim)).withStyle(ChatFormatting.YELLOW)));
        player.sendSystemMessage(Component.literal("Nivel: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(claim.level)).withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.sendSystemMessage(Component.literal("Radio: ").withStyle(ChatFormatting.GRAY).append(Component.literal(claim.radius + " bloques").withStyle(ChatFormatting.WHITE)));
        player.sendSystemMessage(Component.literal("Usá /claim trust <jugador>, /claim untrust <jugador>, /claim preview.").withStyle(ChatFormatting.DARK_GRAY));
    }


    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity source = event.getExplosion().getDirectSourceEntity();

        // Anti-grifeo: solo protegemos contra TNT en claims activos.
        // Creepers, Wither y explosiones de mobs siguen funcionando para evitar abuso de dueños.
        if (!(source instanceof PrimedTnt)) return;

        ClaimSavedData data = ClaimSavedData.get(level.getServer());
        event.getAffectedBlocks().removeIf(pos -> {
            Optional<ClaimRecord> claim = data.findClaimAt(level, pos);
            return claim.isPresent() && claim.get().isActive();
        });
    }

    @SubscribeEvent
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        if (tick % 10 != 0) return;

        // Partículas suaves del núcleo cuando el chunk está cargado.
        if (tick % 20 == 0) {
            ClaimSavedData data = ClaimSavedData.get(event.getServer());
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (ClaimRecord claim : data.claims) {
                    if (!claim.dimension.equals(level.dimension().location())) continue;
                    if (!level.isLoaded(claim.pos)) continue;

                    double x = claim.pos.getX() + 0.5;
                    double y = claim.pos.getY() + 1.4;
                    double z = claim.pos.getZ() + 0.5;
                    ClaimCoreItem.tickVisual(level, claim.pos, tick);
                    if (claim.isActive()) {
                        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.35, z, 2, 0.18, 0.18, 0.18, 0.01);
                    } else {
                        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.35, z, 2, 0.16, 0.16, 0.16, 0.005);
                    }

                    if (tick % 80 == 0) {
                        level.playSound(null, claim.pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.32F, claim.isActive() ? 1.35F : 0.65F);
                    }
                }
            }
        }

        if (ACTIVE_PREVIEWS.isEmpty()) return;

        ACTIVE_PREVIEWS.entrySet().removeIf(entry -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) return true;

            PreviewTarget target = entry.getValue();
            ServerLevel level = event.getServer().getLevel(target.dimension());
            if (level == null) return true;

            ClaimSavedData data = ClaimSavedData.get(event.getServer());
            Optional<ClaimRecord> optional = data.findByCore(level, target.corePos());
            if (optional.isEmpty()) {
                player.sendSystemMessage(Component.literal("✦ Preview desactivada: el nexo ya no existe.").withStyle(ChatFormatting.GRAY));
                return true;
            }

            ClaimCommands.drawPreview(level, optional.get(), player.getBlockY());
            return false;
        });
    }

    public static boolean toggleTestMode(ServerPlayer player) {
        UUID id = player.getUUID();
        if (BUILD_TEST_MODE.contains(id)) {
            BUILD_TEST_MODE.remove(id);
            return false;
        }
        BUILD_TEST_MODE.add(id);
        return true;
    }

    public static boolean isTestMode(ServerPlayer player) {
        return BUILD_TEST_MODE.contains(player.getUUID());
    }

    public static boolean canPlayerBuild(ServerPlayer player, ClaimRecord claim) {
        if (isTestMode(player)) return false;
        return claim.canBuild(player.getUUID());
    }


    public static boolean isPreviewEnabled(ServerPlayer player, ClaimRecord claim) {
        PreviewTarget target = new PreviewTarget(player.serverLevel().dimension(), claim.pos);
        return target.equals(ACTIVE_PREVIEWS.get(player.getUUID()));
    }

    public static boolean togglePreview(ServerPlayer player, ClaimRecord claim) {
        PreviewTarget target = new PreviewTarget(player.serverLevel().dimension(), claim.pos);
        PreviewTarget current = ACTIVE_PREVIEWS.get(player.getUUID());

        if (target.equals(current)) {
            ACTIVE_PREVIEWS.remove(player.getUUID());
            return false;
        }

        ACTIVE_PREVIEWS.put(player.getUUID(), target);
        ClaimCommands.drawPreview(player.serverLevel(), claim, player.getBlockY());
        return true;
    }

    private record PreviewTarget(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos corePos) {}

    public static String formatFuel(ClaimRecord claim) {
        long ms = claim.fuelUntilMs - System.currentTimeMillis();
        if (ms <= 0) return "0h";
        long totalMinutes = ms / 60000L;
        long days = totalMinutes / (60L * 24L);
        long hours = (totalMinutes / 60L) % 24L;
        long minutes = totalMinutes % 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
