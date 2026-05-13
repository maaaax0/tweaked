package de.maax.tweaked.mixin;

import de.maax.tweaked.client.SandboxWorldTypeClient;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
    @Inject(method = "setWorldType", at = @At("TAIL"))
    private void tweaked$applySandboxDefaults(WorldCreationUiState.WorldTypeEntry worldType, CallbackInfo ci) {
        SandboxWorldTypeClient.applyDefaultsIfSandbox((WorldCreationUiState)(Object)this);
    }

    @Inject(method = "setGenerateStructures", at = @At("HEAD"), cancellable = true)
    private void tweaked$keepSandboxStructuresDisabled(boolean generateStructures, CallbackInfo ci) {
        if (SandboxWorldTypeClient.preventSandboxStructures((WorldCreationUiState)(Object)this, generateStructures)) {
            ci.cancel();
        }
    }
}
