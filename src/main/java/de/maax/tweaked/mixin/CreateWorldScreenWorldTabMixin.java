package de.maax.tweaked.mixin;

import de.maax.tweaked.client.SandboxWorldTypeClient;
import de.maax.tweaked.client.TweakedSwitchGridHooks;
import de.maax.tweaked.world.SpawnOptions;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class CreateWorldScreenWorldTabMixin {
    @Shadow
    @Final
    private Button customizeTypeButton;

    @Unique
    private CreateWorldScreen tweaked$screen;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/SwitchGrid$Builder;addSwitch(Lnet/minecraft/network/chat/Component;Ljava/util/function/BooleanSupplier;Ljava/util/function/Consumer;)Lnet/minecraft/client/gui/screens/worldselection/SwitchGrid$SwitchBuilder;",
            ordinal = 0
        )
    )
    private void tweaked$resetSpawnOptions(CreateWorldScreen screen, CallbackInfo ci) {
        this.tweaked$screen = screen;
        SpawnOptions.resetSelected();
        SandboxWorldTypeClient.resetSelectionState();
    }

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/SwitchGrid$Builder;addSwitch(Lnet/minecraft/network/chat/Component;Ljava/util/function/BooleanSupplier;Ljava/util/function/Consumer;)Lnet/minecraft/client/gui/screens/worldselection/SwitchGrid$SwitchBuilder;",
            ordinal = 1
        )
    )
    @Coerce
    private Object tweaked$addVillageSpawnBeforeBonusChest(
        @Coerce Object switchGridBuilder,
        Component label,
        BooleanSupplier stateSupplier,
        Consumer<Boolean> onClicked
    ) {
        WorldCreationUiState uiState = ((CreateWorldScreenAccessor)this.tweaked$screen).tweaked$getUiState();
        return TweakedSwitchGridHooks.addVillageSpawnSwitchBefore(switchGridBuilder, uiState, label, stateSupplier, onClicked);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tweaked$initSandboxPresetButton(CreateWorldScreen screen, CallbackInfo ci) {
        WorldCreationUiState uiState = ((CreateWorldScreenAccessor)screen).tweaked$getUiState();
        uiState.addListener(state -> SandboxWorldTypeClient.updateSandboxButton(state, this.customizeTypeButton));
        SandboxWorldTypeClient.updateSandboxButton(uiState, this.customizeTypeButton);
    }

    @Inject(method = "openPresetEditor", at = @At("HEAD"), cancellable = true)
    private void tweaked$cycleSandboxPreset(CallbackInfo ci) {
        WorldCreationUiState uiState = ((CreateWorldScreenAccessor)this.tweaked$screen).tweaked$getUiState();
        if (SandboxWorldTypeClient.cycleSandboxPreset(uiState)) {
            SandboxWorldTypeClient.updateSandboxButton(uiState, this.customizeTypeButton);
            ci.cancel();
        }
    }
}
