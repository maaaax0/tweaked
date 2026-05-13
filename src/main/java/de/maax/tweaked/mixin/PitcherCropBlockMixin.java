package de.maax.tweaked.mixin;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.PitcherCropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PitcherCropBlock.class)
public abstract class PitcherCropBlockMixin {
    @Redirect(
        method = "entityInside",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"
        )
    )
    private boolean tweaked$useRavagerGriefingRule(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key) {
        return gameRules.getBoolean(TweakedGameRules.RAVAGER_GRIEFING);
    }
}
