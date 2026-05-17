package de.maax.tweaked.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class TweakedGameMenuButtons {
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 4;
    private static final int MENU_BUTTON_WIDTH = 204;
    private static final ItemStack COMMAND_BLOCK_ICON = new ItemStack(Items.COMMAND_BLOCK);

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

        int x = screen.width / 2 + MENU_BUTTON_WIDTH / 2 + BUTTON_GAP;
        int y = findLowestButtonY(event);

        Button button = new CommandBlockButton(
                x,
                y,
                Component.empty(),
                pressed -> minecraft.setScreen(new TweakedAdminScreen(screen))
        );
        event.addListener(button);
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

    private static final class CommandBlockButton extends Button {
        private CommandBlockButton(int x, int y, Component message, OnPress onPress) {
            super(x, y, BUTTON_SIZE, BUTTON_SIZE, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            guiGraphics.renderItem(COMMAND_BLOCK_ICON, this.getX() + 2, this.getY() + 2);
        }
    }
}
