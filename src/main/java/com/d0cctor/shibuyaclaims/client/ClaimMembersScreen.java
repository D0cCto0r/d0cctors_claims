package com.d0cctor.shibuyaclaims.client;

import com.d0cctor.shibuyaclaims.network.ClaimActionPayload;
import com.d0cctor.shibuyaclaims.network.OpenMembersScreenPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ClaimMembersScreen extends Screen {
    private static final int PER_PAGE = 5;

    private final OpenMembersScreenPayload data;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int invitePage = 0;
    private int trustedPage = 0;
    private final List<Button> memberButtons = new ArrayList<>();

    public ClaimMembersScreen(OpenMembersScreenPayload data) {
        super(Component.literal("Miembros del Claim"));
        this.data = data;
    }

    @Override
    protected void init() {
        panelW = 394;
        panelH = 268;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        memberButtons.clear();

        int leftX = panelX + 18;
        int rightX = panelX + 210;
        int y = panelY + 70;
        int buttonW = 166;

        int inviteMaxPage = maxPage(data.inviteNames().size());
        int trustedMaxPage = maxPage(data.trustedNames().size());
        invitePage = Math.max(0, Math.min(invitePage, inviteMaxPage));
        trustedPage = Math.max(0, Math.min(trustedPage, trustedMaxPage));

        int inviteStart = invitePage * PER_PAGE;
        int inviteEnd = Math.min(inviteStart + PER_PAGE, data.inviteNames().size());
        for (int i = inviteStart; i < inviteEnd; i++) {
            final String uuid = data.inviteIds().get(i);
            String label = "+ " + data.inviteNames().get(i);
            addMemberButton(Button.builder(Component.literal(label), b -> send(ClaimActionPayload.TRUST_TARGET, uuid))
                    .bounds(leftX, y + ((i - inviteStart) * 24), buttonW, 20)
                    .build());
        }

        int trustedStart = trustedPage * PER_PAGE;
        int trustedEnd = Math.min(trustedStart + PER_PAGE, data.trustedNames().size());
        for (int i = trustedStart; i < trustedEnd; i++) {
            final String uuid = data.trustedIds().get(i);
            String label = "- " + data.trustedNames().get(i);
            addMemberButton(Button.builder(Component.literal(label), b -> send(ClaimActionPayload.UNTRUST_TARGET, uuid))
                    .bounds(rightX, y + ((i - trustedStart) * 24), buttonW, 20)
                    .build());
        }

        addMemberButton(Button.builder(Component.literal("<"), b -> { invitePage--; rebuildButtons(); })
                .bounds(leftX, panelY + 194, 32, 18)
                .build());
        addMemberButton(Button.builder(Component.literal(">"), b -> { invitePage++; rebuildButtons(); })
                .bounds(leftX + 134, panelY + 194, 32, 18)
                .build());

        addMemberButton(Button.builder(Component.literal("<"), b -> { trustedPage--; rebuildButtons(); })
                .bounds(rightX, panelY + 194, 32, 18)
                .build());
        addMemberButton(Button.builder(Component.literal(">"), b -> { trustedPage++; rebuildButtons(); })
                .bounds(rightX + 134, panelY + 194, 32, 18)
                .build());

        addMemberButton(Button.builder(Component.literal("Volver"), b -> send(ClaimActionPayload.BACK_TO_MAIN, ""))
                .bounds(panelX + 104, panelY + panelH - 30, 88, 20)
                .build());
        addMemberButton(Button.builder(Component.literal("Cerrar"), b -> this.onClose())
                .bounds(panelX + panelW - 192, panelY + panelH - 30, 88, 20)
                .build());
    }

    private int maxPage(int size) {
        if (size <= 0) return 0;
        return (size - 1) / PER_PAGE;
    }

    private void addMemberButton(Button button) {
        memberButtons.add(button);
        addRenderableWidget(button);
    }

    private void send(int action, String target) {
        PacketDistributor.sendToServer(new ClaimActionPayload(data.dimension(), data.x(), data.y(), data.z(), action, target));
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
        drawCentered(graphics, "GESTIÓN DE MIEMBROS", panelX + panelW / 2, panelY + 18, 0xFFF2F2F2);
        drawCentered(graphics, "D0cCtor´s Claims", panelX + panelW / 2, panelY + 30, 0xFF9A9A9A);

        int leftX = panelX + 18;
        int rightX = panelX + 210;
        graphics.drawString(this.font, "Jugadores online", leftX, panelY + 52, 0xFFF2F2F2, false);
        graphics.drawString(this.font, "Autorizados", rightX, panelY + 52, 0xFFF2F2F2, false);
        graphics.drawString(this.font, pageText(invitePage, data.inviteNames().size()), leftX + 52, panelY + 198, 0xFF8A8A8A, false);
        graphics.drawString(this.font, pageText(trustedPage, data.trustedNames().size()), rightX + 52, panelY + 198, 0xFF8A8A8A, false);

        if (data.inviteNames().isEmpty()) {
            graphics.drawString(this.font, "No hay jugadores para invitar", leftX, panelY + 78, 0xFF8A8A8A, false);
        }
        if (data.trustedNames().isEmpty()) {
            graphics.drawString(this.font, "No hay miembros autorizados", rightX, panelY + 78, 0xFF8A8A8A, false);
        }

        for (Button button : memberButtons) {
            button.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private String pageText(int page, int size) {
        return "Pág " + (page + 1) + " / " + (maxPage(size) + 1);
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
