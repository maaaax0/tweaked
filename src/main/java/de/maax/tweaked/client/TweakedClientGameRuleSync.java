package de.maax.tweaked.client;

import de.maax.tweaked.network.GameRuleSync;
import net.minecraft.client.Minecraft;

public final class TweakedClientGameRuleSync {
    private TweakedClientGameRuleSync() {
    }

    public static void handle(GameRuleSync.GameRuleSyncValues payload) {
        if (Minecraft.getInstance().screen instanceof TweakedAdminScreen screen) {
            screen.applyGameRuleValues(payload.values());
        }
    }

    public static void handle(GameRuleSync.SpawnControlSyncValues payload) {
        if (Minecraft.getInstance().screen instanceof TweakedAdminScreen screen) {
            screen.applyDisabledMobSpawningTypes(payload.disabledTypes());
        }
    }

    public static void handle(GameRuleSync.PlayerListSyncValues payload) {
        if (Minecraft.getInstance().screen instanceof TweakedAdminScreen screen) {
            screen.applyPlayerProfiles(payload.profiles());
        }
    }
}
