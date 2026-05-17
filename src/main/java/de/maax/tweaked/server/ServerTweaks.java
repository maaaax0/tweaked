package de.maax.tweaked.server;

import de.maax.tweaked.world.SandboxPresets;
import de.maax.tweaked.world.SpawnOptions;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

public final class ServerTweaks {
    private ServerTweaks() {
    }

    public static void register() {
        AdminCommands.register();
        CountedSummonCommand.register();
        MobGriefingRules.register();
        SpawnControl.register();
        FillHistory.register();
        NeoForge.EVENT_BUS.addListener(ServerTweaks::onServerStarted);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        SpawnControl.load(server);
        SandboxPresets.applyStartupWeatherAndTime(server);
        SpawnOptions.applyPendingSpawn(server);
    }
}
