package com.d0cctor.shibuyaclaims.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClaimRecord {
    public static final int BASE_RADIUS = 16;
    public static final int MAX_LEVEL = 8;
    public static final int MAX_RADIUS = 150;
    public static final int SPACING = 8;
    public static final int MAX_CORE_HEALTH = 1000;
    public static final long REGEN_DELAY_MS = 10L * 60L * 1000L;
    public static final int REGEN_HEALTH_PER_SECOND = 5;

    // Progresión balanceada hasta 150 bloques.
    // Nivel 1 empieza chico, después escala fuerte, y los últimos niveles cuestan más para llegar al máximo.
    private static final int[] LEVEL_RADII = {0, 16, 32, 48, 72, 96, 120, 135, 150};

    public static int radiusForLevel(int level) {
        int clamped = Math.max(1, Math.min(MAX_LEVEL, level));
        return LEVEL_RADII[clamped];
    }

    public ResourceLocation dimension;
    public BlockPos pos;
    public UUID owner;
    public String ownerName;
    public int level;
    public int radius;
    public long fuelUntilMs;
    public int coreHealth;
    public long lastDamagedMs;
    public final Set<UUID> trusted = new HashSet<>();

    public ClaimRecord(ResourceLocation dimension, BlockPos pos, UUID owner, String ownerName) {
        this.dimension = dimension;
        this.pos = pos;
        this.owner = owner;
        this.ownerName = ownerName;
        this.level = 1;
        this.radius = radiusForLevel(this.level);
        this.fuelUntilMs = 0L; // inicia sin energía para evitar spam de núcleos activos
        this.coreHealth = MAX_CORE_HEALTH;
        this.lastDamagedMs = 0L;
    }

    public ClaimRecord() {}

    public boolean isActive() {
        return System.currentTimeMillis() < fuelUntilMs;
    }

    public boolean canBuild(UUID playerId) {
        return owner.equals(playerId) || trusted.contains(playerId);
    }

    public boolean contains(ResourceLocation dim, BlockPos target) {
        if (!dimension.equals(dim)) return false;
        return Math.abs(target.getX() - pos.getX()) <= radius
                && Math.abs(target.getZ() - pos.getZ()) <= radius
                && Math.abs(target.getY() - pos.getY()) <= radius;
    }

    public boolean overlaps(ClaimRecord other, int extraSpacing) {
        if (!dimension.equals(other.dimension)) return false;
        int needed = this.radius + other.radius + extraSpacing;
        int dx = Math.abs(this.pos.getX() - other.pos.getX());
        int dz = Math.abs(this.pos.getZ() - other.pos.getZ());
        int dy = Math.abs(this.pos.getY() - other.pos.getY());
        return dx <= needed && dz <= needed && dy <= needed;
    }

    public void addFuelHours(long hours) {
        long now = System.currentTimeMillis();
        long base = Math.max(now, fuelUntilMs);
        long max = now + 30L * 24L * 60L * 60L * 1000L;
        fuelUntilMs = Math.min(max, base + hours * 60L * 60L * 1000L);
    }

    public boolean upgrade() {
        if (level >= MAX_LEVEL) return false;
        level++;
        radius = radiusForLevel(level);
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        tag.putLong("pos", pos.asLong());
        tag.putUUID("owner", owner);
        tag.putString("ownerName", ownerName == null ? "" : ownerName);
        tag.putInt("level", level);
        tag.putInt("radius", radius);
        tag.putLong("fuelUntilMs", fuelUntilMs);
        tag.putInt("coreHealth", coreHealth);
        tag.putLong("lastDamagedMs", lastDamagedMs);

        ListTag trustedList = new ListTag();
        for (UUID uuid : trusted) {
            trustedList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put("trusted", trustedList);
        return tag;
    }

    public static ClaimRecord load(CompoundTag tag) {
        ClaimRecord record = new ClaimRecord();
        record.dimension = ResourceLocation.parse(tag.getString("dimension"));
        record.pos = BlockPos.of(tag.getLong("pos"));
        record.owner = tag.getUUID("owner");
        record.ownerName = tag.getString("ownerName");
        record.level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("level")));
        record.radius = tag.contains("radius") ? tag.getInt("radius") : radiusForLevel(record.level);
        record.radius = Math.max(BASE_RADIUS, Math.min(MAX_RADIUS, record.radius));
        record.fuelUntilMs = tag.getLong("fuelUntilMs");
        record.coreHealth = tag.contains("coreHealth") ? tag.getInt("coreHealth") : MAX_CORE_HEALTH;
        record.coreHealth = Math.max(0, Math.min(MAX_CORE_HEALTH, record.coreHealth));
        record.lastDamagedMs = tag.contains("lastDamagedMs") ? tag.getLong("lastDamagedMs") : 0L;

        ListTag trustedList = tag.getList("trusted", 8);
        for (int i = 0; i < trustedList.size(); i++) {
            try {
                record.trusted.add(UUID.fromString(trustedList.getString(i)));
            } catch (Exception ignored) {}
        }
        return record;
    }
}
