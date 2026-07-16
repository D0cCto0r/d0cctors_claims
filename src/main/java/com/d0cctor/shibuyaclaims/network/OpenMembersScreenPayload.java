package com.d0cctor.shibuyaclaims.network;

import com.d0cctor.shibuyaclaims.ShibuyaClaims;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OpenMembersScreenPayload(
        String dimension,
        int x,
        int y,
        int z,
        List<String> inviteNames,
        List<String> inviteIds,
        List<String> trustedNames,
        List<String> trustedIds
) implements CustomPacketPayload {
    public static final Type<OpenMembersScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ShibuyaClaims.MOD_ID, "open_members_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMembersScreenPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenMembersScreenPayload decode(RegistryFriendlyByteBuf buf) {
            String dimension = buf.readUtf();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            List<String> inviteNames = readList(buf);
            List<String> inviteIds = readList(buf);
            List<String> trustedNames = readList(buf);
            List<String> trustedIds = readList(buf);
            return new OpenMembersScreenPayload(dimension, x, y, z, inviteNames, inviteIds, trustedNames, trustedIds);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OpenMembersScreenPayload payload) {
            buf.writeUtf(payload.dimension());
            buf.writeInt(payload.x());
            buf.writeInt(payload.y());
            buf.writeInt(payload.z());
            writeList(buf, payload.inviteNames());
            writeList(buf, payload.inviteIds());
            writeList(buf, payload.trustedNames());
            writeList(buf, payload.trustedIds());
        }

        private List<String> readList(RegistryFriendlyByteBuf buf) {
            int size = buf.readInt();
            List<String> out = new ArrayList<>();
            for (int i = 0; i < size; i++) out.add(buf.readUtf());
            return out;
        }

        private void writeList(RegistryFriendlyByteBuf buf, List<String> list) {
            buf.writeInt(list.size());
            for (String s : list) buf.writeUtf(s);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
