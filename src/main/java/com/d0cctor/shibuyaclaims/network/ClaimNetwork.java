package com.d0cctor.shibuyaclaims.network;

import com.d0cctor.shibuyaclaims.client.ClientClaimScreens;
import com.d0cctor.shibuyaclaims.gui.ClaimGui;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ClaimNetwork {
    private ClaimNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenClaimScreenPayload.TYPE, OpenClaimScreenPayload.STREAM_CODEC, ClaimNetwork::handleOpenScreen);
        registrar.playToClient(OpenMembersScreenPayload.TYPE, OpenMembersScreenPayload.STREAM_CODEC, ClaimNetwork::handleOpenMembersScreen);
        registrar.playToServer(ClaimActionPayload.TYPE, ClaimActionPayload.STREAM_CODEC, ClaimNetwork::handleAction);
    }

    private static void handleOpenScreen(OpenClaimScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClaimScreens.open(payload));
    }

    private static void handleOpenMembersScreen(OpenMembersScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClaimScreens.openMembers(payload));
    }

    private static void handleAction(ClaimActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ClaimGui.handleAction(player, payload);
            }
        });
    }
}
