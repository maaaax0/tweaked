package de.maax.tweaked.mixin;

import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditGameRulesScreen.CategoryRuleEntry.class)
public interface CategoryRuleEntryAccessor {
    @Accessor("label")
    Component tweaked$getLabel();
}
