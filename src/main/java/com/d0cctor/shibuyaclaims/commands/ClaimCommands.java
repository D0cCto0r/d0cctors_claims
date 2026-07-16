package com.d0cctor.shibuyaclaims.commands;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.events.ClaimEvents;
import com.d0cctor.shibuyaclaims.registry.ClaimCoreItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

public final class ClaimCommands {
    private ClaimCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("claim")
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("here")
                        .executes(ctx -> here(ctx.getSource())))
                .then(Commands.literal("remove")
                        .executes(ctx -> remove(ctx.getSource())))
                .then(Commands.literal("testmode")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> testmode(ctx.getSource())))
                .then(Commands.literal("setfuel")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("horas", IntegerArgumentType.integer(0, 720))
                                .executes(ctx -> setFuel(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "horas")))))
                .then(Commands.literal("trust")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), true))))
                .then(Commands.literal("untrust")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(ctx -> trust(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador"), false))))
                .then(Commands.literal("preview")
                        .executes(ctx -> preview(ctx.getSource())))
        );
    }

    private static Optional<ClaimRecord> getClaimWherePlayerIs(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ClaimSavedData data = ClaimSavedData.get(level.getServer());
        return data.findClaimAt(level, player.blockPosition());
    }

    private static boolean isOwnerOrOp(ServerPlayer player, ClaimRecord claim) {
        return claim.owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    private static int info(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("No estás dentro de ningún nexo."));
            return 0;
        }

        sendClaimInfo(player, optional.get());
        return 1;
    }

    private static int here(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendSuccess(() -> Component.literal("✦ Esta posición NO está dentro de ningún claim.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        ClaimRecord claim = optional.get();
        player.sendSystemMessage(Component.literal("✦ Esta posición está protegida por un nexo.").withStyle(ChatFormatting.AQUA));
        sendClaimInfo(player, claim);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        ClaimSavedData data = ClaimSavedData.get(player.server);
        int count = 0;
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Tus nexos").withStyle(ChatFormatting.AQUA));

        for (ClaimRecord claim : data.claims) {
            if (!claim.owner.equals(player.getUUID())) continue;
            count++;
            player.sendSystemMessage(Component.literal("#" + count + " ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(claim.dimension + " ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("x=" + claim.pos.getX() + " y=" + claim.pos.getY() + " z=" + claim.pos.getZ()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | Nv " + claim.level + " | R " + claim.radius + " | " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.YELLOW)));
        }

        if (count == 0) {
            player.sendSystemMessage(Component.literal("No tenés nexos creados todavía.").withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("Tenés que estar dentro del nexo que querés borrar."));
            return 0;
        }

        ClaimRecord claim = optional.get();
        if (!isOwnerOrOp(player, claim)) {
            source.sendFailure(Component.literal("Solo el dueño o un OP puede borrar este nexo."));
            return 0;
        }

        ClaimSavedData data = ClaimSavedData.get(player.server);
        data.removeClaim(claim);
        if (player.serverLevel() instanceof ServerLevel level) {
            ClaimCoreItem.removeVisual(level, claim.pos);
        }
        source.sendSuccess(() -> Component.literal("✦ Nexo borrado y núcleo visual eliminado.").withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int testmode(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        boolean enabled = ClaimEvents.toggleTestMode(player);
        if (enabled) {
            source.sendSuccess(() -> Component.literal("✦ TestMode ACTIVADO: aunque seas dueño, el nexo te va a bloquear romper/colocar. Usalo para probar solo.").withStyle(ChatFormatting.RED), false);
        } else {
            source.sendSuccess(() -> Component.literal("✦ TestMode DESACTIVADO: tus permisos vuelven a la normalidad.").withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    private static int setFuel(CommandSourceStack source, int hours) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("Tenés que estar dentro del nexo."));
            return 0;
        }

        ClaimRecord claim = optional.get();
        if (!player.hasPermissions(2)) {
            source.sendFailure(Component.literal("Solo un OP puede cambiar la energía del nexo."));
            return 0;
        }

        claim.fuelUntilMs = System.currentTimeMillis() + (hours * 60L * 60L * 1000L);
        ClaimSavedData.get(player.server).setDirty();
        source.sendSuccess(() -> Component.literal("✦ Combustible seteado a " + hours + "h. Estado: " + (claim.isActive() ? "Activo" : "Sin energía")).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int trust(CommandSourceStack source, ServerPlayer target, boolean add) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("Tenés que estar dentro de tu nexo."));
            return 0;
        }

        ClaimRecord claim = optional.get();
        if (!claim.owner.equals(player.getUUID())) {
            source.sendFailure(Component.literal("Solo el dueño puede gestionar miembros."));
            return 0;
        }

        if (target.getUUID().equals(player.getUUID())) {
            source.sendFailure(Component.literal("No necesitás darte permisos a vos mismo."));
            return 0;
        }

        ClaimSavedData data = ClaimSavedData.get(player.server);
        if (add) {
            claim.trusted.add(target.getUUID());
            source.sendSuccess(() -> Component.literal("✦ " + target.getGameProfile().getName() + " ahora puede construir en este nexo.").withStyle(ChatFormatting.GREEN), false);
        } else {
            claim.trusted.remove(target.getUUID());
            source.sendSuccess(() -> Component.literal("✦ " + target.getGameProfile().getName() + " ya no puede construir en este nexo.").withStyle(ChatFormatting.YELLOW), false);
        }
        data.setDirty();
        return 1;
    }

    private static int preview(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("Este comando solo puede usarlo un jugador."));
            return 0;
        }

        Optional<ClaimRecord> optional = getClaimWherePlayerIs(player);
        if (optional.isEmpty()) {
            source.sendFailure(Component.literal("No estás dentro de ningún nexo."));
            return 0;
        }

        ClaimRecord claim = optional.get();

        if (ClaimEvents.togglePreview(player, claim)) {
            source.sendSuccess(() -> Component.literal("✦ Preview del nexo ACTIVADA. Usá /claim preview otra vez para apagarla.").withStyle(ChatFormatting.AQUA), false);
        } else {
            source.sendSuccess(() -> Component.literal("✦ Preview del nexo DESACTIVADA.").withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }

    private static void sendClaimInfo(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Nexo").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Dueño: " + claim.ownerName).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Estado: " + (claim.isActive() ? "Activo" : "Sin energía")).withStyle(claim.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED));
        player.sendSystemMessage(Component.literal("Energía: " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Nivel: " + claim.level + " | Radio: " + claim.radius).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("Centro: " + claim.pos.getX() + " " + claim.pos.getY() + " " + claim.pos.getZ()).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal("Dimensión: " + claim.dimension).withStyle(ChatFormatting.DARK_GRAY));
        player.sendSystemMessage(Component.literal("Miembros autorizados: " + claim.trusted.size()).withStyle(ChatFormatting.WHITE));
        if (ClaimEvents.isTestMode(player)) {
            player.sendSystemMessage(Component.literal("TestMode: ACTIVADO. Te bloquea como si fueras ajeno.").withStyle(ChatFormatting.RED));
        }
    }

    public static void drawPreview(ServerLevel level, ClaimRecord claim, int y) {
        int minX = claim.pos.getX() - claim.radius;
        int maxX = claim.pos.getX() + claim.radius;
        int minZ = claim.pos.getZ() - claim.radius;
        int maxZ = claim.pos.getZ() + claim.radius;
        int minY = Math.max(level.getMinBuildHeight(), claim.pos.getY() - claim.radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, claim.pos.getY() + claim.radius);

        for (int x = minX; x <= maxX; x += 2) {
            particle(level, x, y, minZ);
            particle(level, x, y, maxZ);
            particle(level, x, minY, minZ);
            particle(level, x, maxY, minZ);
            particle(level, x, minY, maxZ);
            particle(level, x, maxY, maxZ);
        }

        for (int z = minZ; z <= maxZ; z += 2) {
            particle(level, minX, y, z);
            particle(level, maxX, y, z);
            particle(level, minX, minY, z);
            particle(level, minX, maxY, z);
            particle(level, maxX, minY, z);
            particle(level, maxX, maxY, z);
        }

        for (int yy = minY; yy <= maxY; yy += 2) {
            particle(level, minX, yy, minZ);
            particle(level, minX, yy, maxZ);
            particle(level, maxX, yy, minZ);
            particle(level, maxX, yy, maxZ);
        }
    }

    private static void particle(ServerLevel level, int x, int y, int z) {
        level.sendParticles(ParticleTypes.END_ROD, x + 0.5, y + 0.5, z + 0.5, 1, 0, 0, 0, 0.0);
    }
}
