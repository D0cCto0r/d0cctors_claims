package com.d0cctor.shibuyaclaims.network;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClaimActionPayload(
        String dimension,
        int x,
        int y,
        int z,
        int action,
        String target
) implements CustomPacketPayload {
    public static final int ADD_FUEL = 0;
    public static final int UPGRADE = 1;
    public static final int TOGGLE_PREVIEW = 2;
    public static final int INFO = 3;
    public static final int OPEN_MEMBERS = 4;
    public static final int LIST_MEMBERS = 5;
    public static final int TRUST_TARGET = 6;
    public static final int UNTRUST_TARGET = 7;
    public static final int BACK_TO_MAIN = 8;
    public static final int DEACTIVATE = 9;

    public static final Type<ClaimActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShibuyaClaims.MOD_ID, "claim_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ClaimActionPayload decode(RegistryFriendlyByteBuf buf) {
            return new ClaimActionPayload(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ClaimActionPayload payload) {
            buf.writeUtf(payload.dimension());
            buf.writeInt(payload.x());
            buf.writeInt(payload.y());
            buf.writeInt(payload.z());
            buf.writeInt(payload.action());
            buf.writeUtf(payload.target() == null ? "" : payload.target());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
