package de.maax.tweaked.mixin;

import com.mojang.brigadier.context.CommandContext;
import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {
    @Inject(method = "setRule", at = @At("TAIL"))
    private static <T extends GameRules.Value<T>> void tweaked$applyMobGriefingHierarchy(
        CommandContext<CommandSourceStack> source,
        GameRules.Key<T> gameRule,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (gameRule == GameRules.RULE_MOBGRIEFING) {
            GameRules gameRules = source.getSource().getServer().getGameRules();
            TweakedGameRules.applyMobGriefingHierarchy(gameRules, GameRules.RULE_MOBGRIEFING, gameRules.getBoolean(GameRules.RULE_MOBGRIEFING), source.getSource().getServer());
        }
    }
}
