package de.maax.tweaked.mixin;

import de.maax.tweaked.client.SandboxWorldTypeClient;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    @Inject(method = "onCreate", at = @At("HEAD"))
    private void tweaked$prepareSpawnOptions(CallbackInfo ci) {
        SandboxWorldTypeClient.prepareSpawnOptions(((CreateWorldScreenAccessor)this).tweaked$getUiState());
    }
}
