package de.maax.tweaked.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class TweakedGameMenuButtons {
    private TweakedGameMenuButtons() {
    }

    public static void addGameMenuButton(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!canOpenTweakedMenu(minecraft)) {
            return;
        }

        int width = 204;
        int height = 20;
        int x = screen.width / 2 - width / 2;
        int y = findLowestButtonY(event) + height + 4;

        event.addListener(Button.builder(
                Component.translatable("menu.tweaked.admin"),
                button -> minecraft.setScreen(new TweakedAdminScreen(screen))
        ).bounds(x, y, width, height).build());
    }

    public static boolean canOpenTweakedMenu(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    private static int findLowestButtonY(ScreenEvent.Init.Post event) {
        int lowestY = event.getScreen().height / 4 + 8;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget) {
                lowestY = Math.max(lowestY, widget.getY());
            }
        }
        return lowestY;
    }
}
