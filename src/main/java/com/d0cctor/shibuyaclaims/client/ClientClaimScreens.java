package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.network.OpenClaimScreenPayload;
import com.d0cctor.shibuyaclaims.network.OpenMembersScreenPayload;
import net.minecraft.client.Minecraft;

public final class ClientClaimScreens {
    private ClientClaimScreens() {}

    public static void open(OpenClaimScreenPayload payload) {
        Minecraft.getInstance().setScreen(new ClaimCoreScreen(payload));
    }

    public static void openMembers(OpenMembersScreenPayload payload) {
        Minecraft.getInstance().setScreen(new ClaimMembersScreen(payload));
    }
}
