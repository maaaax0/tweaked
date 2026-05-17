package de.maax.tweaked.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldCreationUiState.class)
public interface WorldCreationUiStateAccessor {
    @Accessor("allowCommands")
    Boolean tweaked$getAllowCommands();

    @Accessor("allowCommands")
    void tweaked$setAllowCommands(Boolean allowCommands);
}
