package de.maax.tweaked.menu;

import de.maax.tweaked.Tweaked;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TweakedMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, Tweaked.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InvSeeMenu>> INVSEE = MENUS.register(
            "invsee",
            () -> IMenuTypeExtension.create(InvSeeMenu::new)
    );

    private TweakedMenus() {
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
