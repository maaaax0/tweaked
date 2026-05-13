package de.maax.tweaked.server;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

public final class MobGriefingRules {
    private MobGriefingRules() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MobGriefingRules::onMobGriefing);
    }

    private static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        GameRules gameRules = entity.level().getGameRules();
        GameRules.Key<GameRules.BooleanValue> rule = ruleFor(entity);
        if (rule != null) {
            event.setCanGrief(gameRules.getBoolean(rule));
        }
    }

    private static GameRules.Key<GameRules.BooleanValue> ruleFor(Entity entity) {
        if (entity instanceof Creeper) {
            return TweakedGameRules.CREEPER_GRIEFING;
        }

        if (entity instanceof WitherBoss || entity instanceof WitherSkull) {
            return TweakedGameRules.WITHER_GRIEFING;
        }

        if (entity instanceof Ghast || entity instanceof LargeFireball fireball && fireball.getOwner() instanceof Ghast) {
            return TweakedGameRules.GHAST_GRIEFING;
        }

        if (entity instanceof EnderMan) {
            return TweakedGameRules.ENDERMAN_GRIEFING;
        }

        if (entity instanceof Ravager) {
            return TweakedGameRules.RAVAGER_GRIEFING;
        }

        if (entity instanceof Sheep) {
            return TweakedGameRules.SHEEP_EAT_GRASS;
        }

        if (entity instanceof Piglin) {
            return TweakedGameRules.PIGLIN_GOLD_PICKUP;
        }

        if (entity instanceof Allay) {
            return TweakedGameRules.ALLAY_ITEM_PICKUP;
        }

        return null;
    }
}
