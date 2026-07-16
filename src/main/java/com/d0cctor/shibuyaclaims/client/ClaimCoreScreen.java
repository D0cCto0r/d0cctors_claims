package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.network.ClaimActionPayload;
import com.d0cctor.shibuyaclaims.network.OpenClaimScreenPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ClaimCoreScreen extends Screen {
    private final OpenClaimScreenPayload data;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private final List<Button> claimButtons = new ArrayList<>();

    public ClaimCoreScreen(OpenClaimScreenPayload data) {
        super(Component.literal("Nexo de Protección"));
        this.data = data;
    }

    @Override
    protected void init() {
        claimButtons.clear();
        panelW = 340;
        panelH = 284;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int buttonW = 132;
        int buttonH = 20;
        int left = panelX + 24;
        int right = panelX + panelW - 24 - buttonW;
        int row1 = panelY + 156;
        int row2 = panelY + 184;
        int row3 = panelY + 212;
        int row4 = panelY + 242;

        addClaimButton(Button.builder(Component.literal("Agregar energía"), b -> send(ClaimActionPayload.ADD_FUEL))
                .bounds(left, row1, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Mejorar rango"), b -> send(ClaimActionPayload.UPGRADE))
                .bounds(right, row1, buttonW, buttonH)
                .build());

        String previewText = data.previewEnabled() ? "Preview: ON" : "Preview: OFF";
        addClaimButton(Button.builder(Component.literal(previewText), b -> send(ClaimActionPayload.TOGGLE_PREVIEW))
                .bounds(left, row2, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Info en chat"), b -> send(ClaimActionPayload.INFO))
                .bounds(right, row2, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Invitar miembro"), b -> send(ClaimActionPayload.OPEN_MEMBERS))
                .bounds(left, row3, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Ver miembros"), b -> send(ClaimActionPayload.OPEN_MEMBERS))
                .bounds(right, row3, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Desactivar"), b -> send(ClaimActionPayload.DEACTIVATE))
                .bounds(left, row4, buttonW, buttonH)
                .build());

        addClaimButton(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(right, row4, buttonW, buttonH)
                .build());
    }

    private void addClaimButton(Button button) {
        claimButtons.add(button);
        addRenderableWidget(button);
    }

    private void send(int action) {
        PacketDistributor.sendToServer(new ClaimActionPayload(data.dimension(), data.x(), data.y(), data.z(), action, ""));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x66000000);
        graphics.fill(panelX - 4, panelY - 4, panelX + panelW + 4, panelY + panelH + 4, 0xAA000000);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE050505);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, 0xEE111111);
        graphics.fill(panelX + 3, panelY + 3, panelX + panelW - 3, panelY + panelH - 3, 0xEE0A0A0A);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFFF2F2F2);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF5E5E5E);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFFF2F2F2);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF5E5E5E);

        graphics.fill(panelX + 12, panelY + 10, panelX + panelW - 12, panelY + 42, 0xAA000000);
        drawCentered(graphics, "NEXO DE PROTECCIÓN", panelX + panelW / 2, panelY + 23, 0xFFF2F2F2);

        int statusColor = data.active() ? 0xFF34C759 : 0xFFFF453A;
        String statusText = data.active() ? "ACTIVO" : "SIN COMBUSTIBLE";
        graphics.fill(panelX + 24, panelY + 56, panelX + 154, panelY + 78, 0xAA000000);
        graphics.fill(panelX + 26, panelY + 58, panelX + 38, panelY + 76, statusColor);
        graphics.drawString(this.font, Component.literal(statusText).withStyle(data.active() ? ChatFormatting.GREEN : ChatFormatting.RED), panelX + 46, panelY + 63, 0xFFFFFFFF, false);

        graphics.fill(panelX + panelW - 154, panelY + 56, panelX + panelW - 24, panelY + 78, 0xAA000000);
        graphics.fill(panelX + panelW - 152, panelY + 58, panelX + panelW - 140, panelY + 76, 0xFFB8B8B8);
        graphics.drawString(this.font, "VIDA " + data.coreHealth() + "/" + data.maxCoreHealth(), panelX + panelW - 134, panelY + 63, 0xFFF2F2F2, false);

        int leftX = panelX + 24;
        int rightX = panelX + 170;
        int textY = panelY + 96;
        drawLine(graphics, "Dueño", data.ownerName(), leftX, textY);
        drawLine(graphics, "Energía", data.fuelText(), leftX, textY + 16);
        drawLine(graphics, "Miembros", String.valueOf(data.trustedCount()), leftX, textY + 32);
        drawLine(graphics, "Nivel", data.level() + " / 8", rightX, textY);
        drawLine(graphics, "Radio", data.radius() + " bloques", rightX, textY + 16);
        drawLine(graphics, "Centro", data.x() + " " + data.y() + " " + data.z(), rightX, textY + 32);

        graphics.fill(panelX + 24, panelY + 144, panelX + panelW - 24, panelY + 145, 0x66444444);

        for (Button button : claimButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawLine(GuiGraphics graphics, String label, String value, int x, int y) {
        graphics.drawString(this.font, label + ":", x, y, 0xFF8A8A8A, false);
        graphics.drawString(this.font, value, x + 64, y, 0xFFF2F2F2, false);
    }

    private void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int color) {
        int w = this.font.width(text);
        graphics.drawString(this.font, text, centerX - w / 2, y, color, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
