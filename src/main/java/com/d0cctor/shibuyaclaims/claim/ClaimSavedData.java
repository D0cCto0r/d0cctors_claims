package com.d0cctor.shibuyaclaims.claim;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClaimSavedData extends SavedData {
    private static final String DATA_NAME = "d0cctors_claims";
    public final List<ClaimRecord> claims = new ArrayList<>();

    public static ClaimSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ClaimSavedData::new, ClaimSavedData::load, null),
                DATA_NAME
        );
    }

    public static ClaimSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        ClaimSavedData data = new ClaimSavedData();
        ListTag list = tag.getList("claims", 10);
        for (int i = 0; i < list.size(); i++) {
            data.claims.add(ClaimRecord.load(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (ClaimRecord record : claims) {
            list.add(record.save());
        }
        tag.put("claims", list);
        return tag;
    }

    public Optional<ClaimRecord> findClaimAt(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var dim = level.dimension().location();
        return claims.stream().filter(c -> c.contains(dim, pos)).findFirst();
    }

    public Optional<ClaimRecord> findByCore(ServerLevel level, net.minecraft.core.BlockPos pos) {
        var dim = level.dimension().location();
        return claims.stream().filter(c -> c.dimension.equals(dim) && c.pos.equals(pos)).findFirst();
    }

    public boolean wouldOverlap(ClaimRecord candidate) {
        for (ClaimRecord record : claims) {
            if (candidate.overlaps(record, ClaimRecord.SPACING)) return true;
        }
        return false;
    }

    public void addClaim(ClaimRecord record) {
        claims.add(record);
        setDirty();
    }

    public void removeClaim(ClaimRecord record) {
        claims.remove(record);
        setDirty();
    }
}
