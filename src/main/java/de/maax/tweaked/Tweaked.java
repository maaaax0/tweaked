package de.maax.tweaked;

import com.mojang.logging.LogUtils;
import de.maax.tweaked.server.ServerTweaks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Tweaked.MOD_ID)
public class Tweaked {
    public static final String MOD_ID = "tweaked";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Tweaked(IEventBus modEventBus) {
        ServerTweaks.register();
        LOGGER.info("Loaded {}", MOD_ID);
    }
}
