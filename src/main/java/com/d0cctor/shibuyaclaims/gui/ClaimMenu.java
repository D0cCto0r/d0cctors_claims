package com.d0cctor.shibuyaclaims.gui;

import com.d0cctor.shibuyaclaims.claim.ClaimRecord;
import com.d0cctor.shibuyaclaims.claim.ClaimSavedData;
import com.d0cctor.shibuyaclaims.commands.ClaimCommands;
import com.d0cctor.shibuyaclaims.events.ClaimEvents;
import com.d0cctor.shibuyaclaims.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class ClaimMenu extends ChestMenu {
    private static final long FUEL_HOURS_PER_ITEM = 6L;

    private final SimpleContainer display;
    private final ResourceLocation dimension;
    private final BlockPos corePos;

    private ClaimMenu(int containerId, Inventory playerInventory, ResourceLocation dimension, BlockPos corePos, SimpleContainer display) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, display, 3);
        this.display = display;
        this.dimension = dimension;
        this.corePos = corePos;
        refresh(playerInventory.player);
    }

    public ClaimMenu(int containerId, Inventory playerInventory, ResourceLocation dimension, BlockPos corePos) {
        this(containerId, playerInventory, dimension, corePos, new SimpleContainer(27));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 27) {
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            handleButton(slotId, serverPlayer);
            refresh(serverPlayer);
            broadcastChanges();
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Evita que los items falsos de la interfaz se muevan al inventario.
        return ItemStack.EMPTY;
    }

    private void handleButton(int slot, ServerPlayer player) {
        Optional<ClaimRecord> optional = getClaim(player);
        if (optional.isEmpty()) {
            player.closeContainer();
            player.sendSystemMessage(Component.literal("✦ Este claim ya no existe.").withStyle(ChatFormatting.RED));
            return;
        }

        ClaimRecord claim = optional.get();
        if (!claim.owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
            player.closeContainer();
            player.sendSystemMessage(Component.literal("✦ Solo el dueño puede usar la interfaz de este claim.").withStyle(ChatFormatting.RED));
            return;
        }

        ClaimSavedData data = ClaimSavedData.get(player.server);

        if (slot == 10) {
            if (!consumeOne(player, ModItems.COMBUSTIBLE_DEL_CICLO.get())) {
                player.sendSystemMessage(Component.literal("✦ Necesitás Combustible del Ciclo en tu inventario.").withStyle(ChatFormatting.RED));
                return;
            }
            claim.addFuelHours(FUEL_HOURS_PER_ITEM);
            data.setDirty();
            player.sendSystemMessage(Component.literal("✦ Agregaste combustible. Tiempo: " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.GREEN));
            return;
        }

        if (slot == 12) {
            if (claim.level >= ClaimRecord.MAX_LEVEL) {
                player.sendSystemMessage(Component.literal("✦ Este claim ya está al nivel máximo.").withStyle(ChatFormatting.YELLOW));
                return;
            }
            if (!consumeOne(player, ModItems.FRAGMENTO_EXPANSION.get())) {
                player.sendSystemMessage(Component.literal("✦ Necesitás Fragmento de Expansión en tu inventario.").withStyle(ChatFormatting.RED));
                return;
            }
            claim.upgrade();
            data.setDirty();
            player.sendSystemMessage(Component.literal("✦ Claim mejorado a nivel " + claim.level + ". Radio: " + claim.radius + " bloques.").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }

        if (slot == 14) {
            boolean enabled = ClaimEvents.togglePreview(player, claim);
            if (enabled) {
                player.sendSystemMessage(Component.literal("✦ Preview ACTIVADA. Tocá otra vez para apagarla.").withStyle(ChatFormatting.AQUA));
            } else {
                player.sendSystemMessage(Component.literal("✦ Preview DESACTIVADA.").withStyle(ChatFormatting.GRAY));
            }
            return;
        }

        if (slot == 16) {
            sendInfo(player, claim);
            return;
        }

        if (slot == 22) {
            player.closeContainer();
        }
    }

    private Optional<ClaimRecord> getClaim(ServerPlayer player) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
        ServerLevel level = player.server.getLevel(key);
        if (level == null) return Optional.empty();
        return ClaimSavedData.get(player.server).findByCore(level, corePos);
    }

    private boolean consumeOne(ServerPlayer player, Item item) {
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

    private void refresh(Player rawPlayer) {
        for (int i = 0; i < display.getContainerSize(); i++) {
            display.setItem(i, named(new ItemStack(Items.BLACK_STAINED_GLASS_PANE), " "));
        }

        ClaimRecord claim = null;
        if (rawPlayer instanceof ServerPlayer serverPlayer) {
            claim = getClaim(serverPlayer).orElse(null);
        }

        if (claim == null) {
            display.setItem(13, named(new ItemStack(Items.BARRIER), "§cClaim no encontrado"));
            return;
        }

        display.setItem(4, named(new ItemStack(Items.NETHER_STAR), "§bNúcleo de Claim"));
        display.setItem(10, named(new ItemStack(ModItems.COMBUSTIBLE_DEL_CICLO.get()), "§aAgregar combustible §7(+6h)"));
        display.setItem(12, named(new ItemStack(ModItems.FRAGMENTO_EXPANSION.get()), "§dMejorar tamaño §7(Nv " + claim.level + "/" + ClaimRecord.MAX_LEVEL + ")"));
        display.setItem(14, named(new ItemStack(Items.ENDER_EYE), "§bPreview ON/OFF"));
        display.setItem(16, named(new ItemStack(Items.PAPER), "§fMostrar info en chat"));
        display.setItem(22, named(new ItemStack(Items.BARRIER), "§cCerrar"));

        String estado = claim.isActive() ? "§aActivo" : "§cSin combustible";
        display.setItem(18, named(new ItemStack(Items.LIME_DYE), "§7Estado: " + estado));
        display.setItem(19, named(new ItemStack(Items.CLOCK), "§eCombustible: §f" + ClaimEvents.formatFuel(claim)));
        display.setItem(20, named(new ItemStack(Items.AMETHYST_SHARD), "§dNivel: §f" + claim.level));
        display.setItem(21, named(new ItemStack(Items.COMPASS), "§bRadio: §f" + claim.radius + " bloques"));
        display.setItem(23, named(new ItemStack(Items.PLAYER_HEAD), "§fDueño: §e" + claim.ownerName));
        display.setItem(24, named(new ItemStack(Items.WRITABLE_BOOK), "§fMiembros: §e" + claim.trusted.size()));
        display.setItem(25, named(new ItemStack(Items.REPEATER), "§7Centro: §f" + claim.pos.getX() + " " + claim.pos.getY() + " " + claim.pos.getZ()));
    }

    private ItemStack named(ItemStack stack, String name) {
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private void sendInfo(ServerPlayer player, ClaimRecord claim) {
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(Component.literal("✦ Núcleo de Claim").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Dueño: " + claim.ownerName).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Estado: " + (claim.isActive() ? "Activo" : "Sin combustible")).withStyle(claim.isActive() ? ChatFormatting.GREEN : ChatFormatting.RED));
        player.sendSystemMessage(Component.literal("Combustible: " + ClaimEvents.formatFuel(claim)).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Nivel: " + claim.level + " | Radio: " + claim.radius).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("Miembros autorizados: " + claim.trusted.size()).withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal("Comandos: /claim trust <jugador>, /claim untrust <jugador>, /claim preview").withStyle(ChatFormatting.DARK_GRAY));
    }
}
