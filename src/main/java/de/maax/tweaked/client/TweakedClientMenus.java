package de.maax.tweaked.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.maax.tweaked.menu.TweakedMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import org.lwjgl.glfw.GLFW;

public final class TweakedClientMenus {
    private static final KeyMapping OPEN_ADMIN_SCREEN = new KeyMapping(
            "key.tweaked.admin_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F12,
            KeyMapping.CATEGORY_MISC
    );

    private TweakedClientMenus() {
    }

    public static void register(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigurationScreen(container, parent));
        modEventBus.addListener(TweakedClientMenus::registerMenuScreens);
        modEventBus.addListener(TweakedClientMenus::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(TweakedGameMenuButtons::addGameMenuButton);
        NeoForge.EVENT_BUS.addListener(TweakedClientMenus::openAdminScreenWithHotkey);
        NeoForge.EVENT_BUS.addListener(TweakedClientMenus::hideInvSeePreviewNameTags);
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TweakedMenus.INVSEE.get(), InvSeeScreen::new);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ADMIN_SCREEN);
    }

    private static void hideInvSeePreviewNameTags(RenderNameTagEvent event) {
        if (InvSeeScreen.suppressPreviewNameTags()) {
            event.setCanRender(TriState.FALSE);
        }
    }

    private static void openAdminScreenWithHotkey(InputEvent.Key event) {
        if (event.getAction() != InputConstants.PRESS || !OPEN_ADMIN_SCREEN.matches(event.getKey(), event.getScanCode())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TweakedAdminScreen || !TweakedGameMenuButtons.canOpenTweakedMenu(minecraft)) {
            return;
        }

        minecraft.setScreen(new TweakedAdminScreen(minecraft.screen));
    }
}
