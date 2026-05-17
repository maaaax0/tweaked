package de.maax.tweaked.mixin;

import de.maax.tweaked.client.SandboxGameModeClient;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class CreateWorldScreenGameTabMixin {
    @Shadow
    @Final
    private CreateWorldScreen this$0;

    @Unique
    private CycleButton<WorldCreationUiState.SelectedGameMode> tweaked$gameModeButton;

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/CycleButton$Builder;withValues([Ljava/lang/Object;)Lnet/minecraft/client/gui/components/CycleButton$Builder;",
            ordinal = 0
        )
    )
    private CycleButton.Builder<WorldCreationUiState.SelectedGameMode> tweaked$addSandboxGameModeValue(
        CycleButton.Builder<WorldCreationUiState.SelectedGameMode> builder,
        Object[] values
    ) {
        return builder.withValues(
            WorldCreationUiState.SelectedGameMode.SURVIVAL,
            WorldCreationUiState.SelectedGameMode.CREATIVE,
            WorldCreationUiState.SelectedGameMode.HARDCORE,
            WorldCreationUiState.SelectedGameMode.CREATIVE
        );
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/CycleButton$Builder;create(IIIILnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/CycleButton$OnValueChange;)Lnet/minecraft/client/gui/components/CycleButton;",
            ordinal = 0
        )
    )
    private CycleButton<WorldCreationUiState.SelectedGameMode> tweaked$captureGameModeButton(
        CycleButton.Builder<WorldCreationUiState.SelectedGameMode> builder,
        int x,
        int y,
        int width,
        int height,
        Component message,
        CycleButton.OnValueChange<WorldCreationUiState.SelectedGameMode> onValueChange
    ) {
        this.tweaked$gameModeButton = builder.create(x, y, width, height, message, onValueChange);
        return this.tweaked$gameModeButton;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tweaked$updateSandboxGameModeDisplay(CallbackInfo ci) {
        WorldCreationUiState uiState = ((CreateWorldScreenAccessor)this.this$0).tweaked$getUiState();
        uiState.addListener(state -> SandboxGameModeClient.updateGameModeButton(state, this.tweaked$gameModeButton));
        SandboxGameModeClient.updateGameModeButton(uiState, this.tweaked$gameModeButton);
    }
}
