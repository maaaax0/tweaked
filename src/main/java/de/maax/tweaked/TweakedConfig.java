package de.maax.tweaked;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TweakedConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue FLY;
    private static final ModConfigSpec.BooleanValue GOD;
    private static final ModConfigSpec.BooleanValue HEAL;
    private static final ModConfigSpec.BooleanValue FEED;
    private static final ModConfigSpec.BooleanValue INVSEE;
    private static final ModConfigSpec.BooleanValue ENDERSEE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        FLY = feature(builder, "fly");
        GOD = feature(builder, "god");
        HEAL = feature(builder, "heal");
        FEED = feature(builder, "feed");
        INVSEE = feature(builder, "invsee");
        ENDERSEE = feature(builder, "endersee");

        SPEC = builder.build();
    }

    private TweakedConfig() {
    }

    private static ModConfigSpec.BooleanValue feature(ModConfigSpec.Builder builder, String name) {
        return builder.translation("tweaked.configuration." + name)
                .define(name, true);
    }

    public static boolean flyEnabled() {
        return FLY.get();
    }

    public static boolean godEnabled() {
        return GOD.get();
    }

    public static boolean healEnabled() {
        return HEAL.get();
    }

    public static boolean feedEnabled() {
        return FEED.get();
    }

    public static boolean invSeeEnabled() {
        return INVSEE.get();
    }

    public static boolean enderSeeEnabled() {
        return ENDERSEE.get();
    }
}
