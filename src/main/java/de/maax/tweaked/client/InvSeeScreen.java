package de.maax.tweaked.client;

import de.maax.tweaked.Tweaked;
import de.maax.tweaked.menu.InvSeeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public final class InvSeeScreen extends AbstractContainerScreen<InvSeeMenu> {
    private static final ResourceLocation INVSEE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "textures/gui/container/invsee.png");
    private static boolean suppressPreviewNameTags;

    private float xMouse;
    private float yMouse;

    public InvSeeScreen(InvSeeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 360;
        this.imageHeight = 166;
        this.titleLabelX = 97;
        this.titleLabelY = 8;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.xMouse = mouseX;
        this.yMouse = mouseY;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawRightAlignedLabel(guiGraphics, targetLabel(), 168, this.titleLabelY);
        drawRightAlignedLabel(guiGraphics, "You", 352, this.titleLabelY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(INVSEE_TEXTURE, this.leftPos, this.topPos, 0, 0, 176, 166);
        guiGraphics.blit(INVSEE_TEXTURE, this.leftPos + 184, this.topPos, 0, 0, 176, 166);

        int entityLeft = this.leftPos + 26;
        int entityTop = this.topPos + 8;
        int entityRight = this.leftPos + 75;
        int entityBottom = this.topPos + 78;

        Entity entity = targetEntity();
        if (entity instanceof LivingEntity livingEntity) {
            renderEntityWithoutNameTag(guiGraphics, entityLeft, entityTop, entityRight, entityBottom, livingEntity);
        }

        if (this.minecraft.player != null) {
            renderEntityWithoutNameTag(guiGraphics, this.leftPos + 210, this.topPos + 8, this.leftPos + 259, this.topPos + 78, this.minecraft.player);
        }
    }

    public static boolean suppressPreviewNameTags() {
        return suppressPreviewNameTags;
    }

    private void drawRightAlignedLabel(GuiGraphics guiGraphics, String label, int rightX, int y) {
        guiGraphics.drawString(this.font, label, rightX - this.font.width(label), y, 4210752, false);
    }

    private String targetLabel() {
        String label = this.title.getString();
        String suffix = " Inventory";
        if (label.endsWith(suffix)) {
            return label.substring(0, label.length() - suffix.length());
        }
        return label;
    }

    private Entity targetEntity() {
        if (this.minecraft.level == null) {
            return null;
        }

        Entity entity = this.minecraft.level.getEntity(this.menu.targetEntityId());
        if (entity != null) {
            return entity;
        }

        String targetName = targetLabel();
        for (Player player : this.minecraft.level.players()) {
            if (player.getGameProfile().getName().equalsIgnoreCase(targetName)) {
                return player;
            }
        }

        return null;
    }

    private void renderEntityWithoutNameTag(GuiGraphics guiGraphics, int left, int top, int right, int bottom, LivingEntity entity) {
        suppressPreviewNameTags = true;
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    left,
                    top,
                    right,
                    bottom,
                    30,
                    0.0625F,
                    this.xMouse,
                    this.yMouse,
                    entity
            );
        } finally {
            suppressPreviewNameTags = false;
        }
    }
}
