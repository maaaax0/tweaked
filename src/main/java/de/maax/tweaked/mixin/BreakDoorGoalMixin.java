package de.maax.tweaked.mixin;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {
    @Redirect(
        method = "canUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/CommonHooks;canEntityDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)Z"
        )
    )
    private boolean tweaked$useZombieDoorBreakingRule(Level level, BlockPos pos, LivingEntity entity) {
        BlockState state = level.getBlockState(pos);
        return level.getGameRules().getBoolean(TweakedGameRules.ZOMBIE_DOOR_BREAKING)
            && state.canEntityDestroy(level, pos, entity)
            && EventHooks.onEntityDestroyBlock(entity, pos, state);
    }
}
