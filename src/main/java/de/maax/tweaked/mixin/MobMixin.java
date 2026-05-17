package de.maax.tweaked.mixin;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Redirect(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/event/EventHooks;canEntityGrief(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean tweaked$useItemPickupRule(Level level, Entity entity) {
        GameRules.Key<GameRules.BooleanValue> rule = TweakedGameRules.MOB_ITEM_PICKUP;
        if (entity instanceof Piglin) {
            rule = TweakedGameRules.PIGLIN_GOLD_PICKUP;
        } else if (entity instanceof Allay) {
            rule = TweakedGameRules.ALLAY_ITEM_PICKUP;
        } else if (entity instanceof Villager) {
            rule = TweakedGameRules.VILLAGER_ITEM_PICKUP;
        }

        return level.getGameRules().getBoolean(rule);
    }
}
