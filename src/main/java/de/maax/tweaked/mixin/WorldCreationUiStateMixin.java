package de.maax.tweaked.mixin;

import de.maax.tweaked.client.SandboxGameModeClient;
import de.maax.tweaked.client.SandboxWorldTypeClient;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true)
    private void tweaked$handleSandboxGameMode(WorldCreationUiState.SelectedGameMode gameMode, CallbackInfo ci) {
        if (SandboxGameModeClient.handleGameModeSelection((WorldCreationUiState)(Object)this, gameMode)) {
            ci.cancel();
        }
    }

    @Inject(method = "setWorldType", at = @At("TAIL"))
    private void tweaked$applySandboxWorldTypeDefaults(WorldCreationUiState.WorldTypeEntry worldType, CallbackInfo ci) {
        SandboxWorldTypeClient.applyDefaultsIfSandbox((WorldCreationUiState)(Object)this);
    }

    @Inject(method = "setGenerateStructures", at = @At("TAIL"))
    private void tweaked$applySandboxStructures(boolean generateStructures, CallbackInfo ci) {
        SandboxWorldTypeClient.applyGenerateStructuresIfSandbox((WorldCreationUiState)(Object)this);
    }
}
