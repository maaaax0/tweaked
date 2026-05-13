package de.maax.tweaked.client;

import de.maax.tweaked.menu.TweakedMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;

public final class TweakedClientMenus {
    private TweakedClientMenus() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(TweakedClientMenus::registerMenuScreens);
        NeoForge.EVENT_BUS.addListener(TweakedClientMenus::hideInvSeePreviewNameTags);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TweakedMenus.INVSEE.get(), InvSeeScreen::new);
    }

    private static void hideInvSeePreviewNameTags(RenderNameTagEvent event) {
        if (InvSeeScreen.suppressPreviewNameTags()) {
            event.setCanRender(TriState.FALSE);
        }
    }
}
