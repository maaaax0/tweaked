package de.maax.tweaked.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    private static final int BAR_WIDTH = 256;
    private static final int BAR_HEIGHT = 2;
    private static final int BAR_BACKGROUND = 0xFF101010;
    private static final int BAR_PROGRESS = 0xFF00FF00;
    private static final Component LOADING_TERRAIN = Component.translatable("multiplayer.downloadingTerrain");

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void tweaked$renderProgressBarOnly(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LevelLoadingScreen screen = (LevelLoadingScreen)(Object)this;
        screen.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int barWidth = Math.min(BAR_WIDTH, screen.width - 32);
        int barX = (screen.width - barWidth) / 2;
        int barY = screen.height / 2;
        int progress = Mth.clamp(this.progressListener.getProgress(), 0, 100);
        int filledWidth = Math.round(barWidth * progress / 100.0F);

        guiGraphics.drawCenteredString(screen.getMinecraft().font, LOADING_TERRAIN, screen.width / 2, barY - 18, 0xFFFFFF);
        guiGraphics.fill(barX, barY, barX + barWidth, barY + BAR_HEIGHT, BAR_BACKGROUND);
        guiGraphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, BAR_PROGRESS);
        ci.cancel();
    }
}
