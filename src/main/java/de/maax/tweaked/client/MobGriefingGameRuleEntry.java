package de.maax.tweaked.client;

import com.google.common.collect.ImmutableList;
import de.maax.tweaked.world.TweakedGameRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;

public final class MobGriefingGameRuleEntry extends EditGameRulesScreen.RuleEntry {
    private final GameRules gameRules;
    private final GameRules.Key<GameRules.BooleanValue> rule;
    private final List<FormattedCharSequence> label;
    private final CycleButton<Boolean> checkbox;
    private final List<AbstractWidget> children;
    private Runnable refreshGroup = () -> {
    };

    public MobGriefingGameRuleEntry(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> rule) {
        super(createTooltip(rule, gameRules.getRule(rule)));
        this.gameRules = gameRules;
        this.rule = rule;
        Component labelComponent = Component.translatable(rule.getDescriptionId());
        this.label = Minecraft.getInstance().font.split(labelComponent, 175);
        this.checkbox = CycleButton.onOffBuilder(gameRules.getBoolean(rule))
            .displayOnlyValue()
            .withCustomNarration(button -> button.createDefaultNarrationMessage().append("\n").append(rule.getId()))
            .create(10, 5, 44, 20, labelComponent, (button, enabled) -> {
                gameRules.getRule(rule).set(enabled, null);
                TweakedGameRules.applyMobGriefingHierarchy(gameRules, rule, enabled, null);
                this.refreshGroup.run();
            });
        this.children = List.of(this.checkbox);
    }

    public void setRefreshGroup(Runnable refreshGroup) {
        this.refreshGroup = refreshGroup;
    }

    public void refresh() {
        this.checkbox.setValue(this.gameRules.getBoolean(this.rule));
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.children;
    }

    @Override
    public void render(
        GuiGraphics guiGraphics,
        int index,
        int top,
        int left,
        int width,
        int height,
        int mouseX,
        int mouseY,
        boolean hovering,
        float partialTick
    ) {
        if (this.label.size() == 1) {
            guiGraphics.drawString(Minecraft.getInstance().font, this.label.get(0), left, top + 5, -1, false);
        } else if (this.label.size() >= 2) {
            guiGraphics.drawString(Minecraft.getInstance().font, this.label.get(0), left, top, -1, false);
            guiGraphics.drawString(Minecraft.getInstance().font, this.label.get(1), left + 10, top + 10, -1, false);
        }

        this.checkbox.setX(left + width - 45);
        this.checkbox.setY(top);
        this.checkbox.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static List<FormattedCharSequence> createTooltip(GameRules.Key<GameRules.BooleanValue> rule, GameRules.BooleanValue value) {
        Component id = Component.literal(rule.getId()).withStyle(ChatFormatting.YELLOW);
        Component defaultValue = Component.translatable("editGamerule.default", Component.literal(value.serialize())).withStyle(ChatFormatting.GRAY);
        String descriptionKey = rule.getDescriptionId() + ".description";
        if (!I18n.exists(descriptionKey)) {
            return ImmutableList.of(id.getVisualOrderText(), defaultValue.getVisualOrderText());
        }

        ImmutableList.Builder<FormattedCharSequence> builder = ImmutableList.<FormattedCharSequence>builder().add(id.getVisualOrderText());
        Component description = Component.translatable(descriptionKey);
        Minecraft.getInstance().font.split(description, 150).forEach(builder::add);
        return builder.add(defaultValue.getVisualOrderText()).build();
    }
}
