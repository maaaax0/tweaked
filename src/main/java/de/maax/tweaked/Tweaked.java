package de.maax.tweaked;

import com.mojang.logging.LogUtils;
import de.maax.tweaked.menu.TweakedMenus;
import de.maax.tweaked.network.GameRuleSync;
import de.maax.tweaked.server.ServerTweaks;
import de.maax.tweaked.world.TweakedGameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Tweaked.MOD_ID)
public class Tweaked {
    public static final String MOD_ID = "tweaked";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Tweaked(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, TweakedConfig.SPEC);
        TweakedMenus.register(modEventBus);
        GameRuleSync.register(modEventBus);
        registerClientMenus(modEventBus, modContainer);
        TweakedGameRules.register();
        ServerTweaks.register();
        LOGGER.info("Loaded {}", MOD_ID);
    }

    private static void registerClientMenus(IEventBus modEventBus, ModContainer modContainer) {
        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        try {
            Class.forName("de.maax.tweaked.client.TweakedClientMenus")
                    .getMethod("register", IEventBus.class, ModContainer.class)
                    .invoke(null, modEventBus, modContainer);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to register client menus", exception);
        }
    }
}
