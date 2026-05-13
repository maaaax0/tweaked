package de.maax.tweaked.mixin;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HarvestFarmland.class)
public abstract class HarvestFarmlandMixin {
    @Redirect(
        method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;canEntityGrief(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean tweaked$useVillagerFarmingRule(Level level, Entity entity) {
        return level.getGameRules().getBoolean(TweakedGameRules.VILLAGER_FARMING);
    }
}
