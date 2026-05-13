package de.maax.tweaked.world;

import java.util.List;
import net.minecraft.world.level.GameRules;

public final class TweakedGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> CREEPER_GRIEFING = register("creeperGriefing");
    public static final GameRules.Key<GameRules.BooleanValue> WITHER_GRIEFING = register("witherGriefing");
    public static final GameRules.Key<GameRules.BooleanValue> GHAST_GRIEFING = register("ghastGriefing");
    public static final GameRules.Key<GameRules.BooleanValue> ENDERMAN_GRIEFING = register("endermanGriefing");
    public static final GameRules.Key<GameRules.BooleanValue> VILLAGER_FARMING = register("villagerFarming");
    public static final GameRules.Key<GameRules.BooleanValue> ZOMBIE_DOOR_BREAKING = register("zombieDoorBreaking");
    public static final GameRules.Key<GameRules.BooleanValue> RAVAGER_GRIEFING = register("ravagerGriefing");
    public static final GameRules.Key<GameRules.BooleanValue> SHEEP_EAT_GRASS = register("sheepEatGrass");
    public static final GameRules.Key<GameRules.BooleanValue> SNIFFER_DIGGING = register("snifferDigging");
    public static final GameRules.Key<GameRules.BooleanValue> MOB_ITEM_PICKUP = register("mobItemPickup");
    public static final GameRules.Key<GameRules.BooleanValue> PIGLIN_GOLD_PICKUP = register("piglinGoldPickup");
    public static final GameRules.Key<GameRules.BooleanValue> ALLAY_ITEM_PICKUP = register("allayItemPickup");

    public static final List<GameRules.Key<GameRules.BooleanValue>> ALL = List.of(
        CREEPER_GRIEFING,
        WITHER_GRIEFING,
        GHAST_GRIEFING,
        ENDERMAN_GRIEFING,
        VILLAGER_FARMING,
        ZOMBIE_DOOR_BREAKING,
        RAVAGER_GRIEFING,
        SHEEP_EAT_GRASS,
        SNIFFER_DIGGING,
        MOB_ITEM_PICKUP,
        PIGLIN_GOLD_PICKUP,
        ALLAY_ITEM_PICKUP
    );

    private TweakedGameRules() {
    }

    public static void register() {
    }

    private static GameRules.Key<GameRules.BooleanValue> register(String name) {
        return GameRules.register(name, GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
    }
}
