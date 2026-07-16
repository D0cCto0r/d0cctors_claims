package com.d0cctor.shibuyaclaims.network;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenClaimScreenPayload(
        String dimension,
        int x,
        int y,
        int z,
        String ownerName,
        boolean active,
        String fuelText,
        int level,
        int radius,
        int trustedCount,
        boolean previewEnabled,
        int coreHealth,
        int maxCoreHealth
) implements CustomPacketPayload {
    public static final Type<OpenClaimScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShibuyaClaims.MOD_ID, "open_claim_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenClaimScreenPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenClaimScreenPayload decode(RegistryFriendlyByteBuf buf) {
            return new OpenClaimScreenPayload(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(),
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OpenClaimScreenPayload payload) {
            buf.writeUtf(payload.dimension());
            buf.writeInt(payload.x());
            buf.writeInt(payload.y());
            buf.writeInt(payload.z());
            buf.writeUtf(payload.ownerName());
            buf.writeBoolean(payload.active());
            buf.writeUtf(payload.fuelText());
            buf.writeInt(payload.level());
            buf.writeInt(payload.radius());
            buf.writeInt(payload.trustedCount());
            buf.writeBoolean(payload.previewEnabled());
            buf.writeInt(payload.coreHealth());
            buf.writeInt(payload.maxCoreHealth());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
