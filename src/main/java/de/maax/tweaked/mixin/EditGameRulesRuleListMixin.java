package de.maax.tweaked.mixin;

import de.maax.tweaked.client.MobGriefingGameRuleEntry;
import de.maax.tweaked.world.TweakedGameRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditGameRulesScreen.RuleList.class)
public abstract class EditGameRulesRuleListMixin {
    private static final String MOB_GRIEFING_CATEGORY = "gamerule.category.tweaked.mob_griefing";
    private static final List<GameRules.Key<GameRules.BooleanValue>> MOB_GRIEFING_RULES = createMobGriefingRules();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tweaked$moveMobGriefingRulesToOwnCategory(EditGameRulesScreen screen, GameRules gameRules, CallbackInfo ci) {
        EditGameRulesScreen.RuleList ruleList = (EditGameRulesScreen.RuleList)(Object)this;
        List<EditGameRulesScreen.RuleEntry> entries = ruleList.children();
        entries.removeIf(EditGameRulesRuleListMixin::isMobGriefingRuleEntry);

        List<MobGriefingGameRuleEntry> mobGriefingEntries = new ArrayList<>();
        for (GameRules.Key<GameRules.BooleanValue> rule : MOB_GRIEFING_RULES) {
            mobGriefingEntries.add(new MobGriefingGameRuleEntry(gameRules, rule));
        }

        Runnable refresh = () -> mobGriefingEntries.forEach(MobGriefingGameRuleEntry::refresh);
        mobGriefingEntries.forEach(entry -> entry.setRefreshGroup(refresh));

        List<EditGameRulesScreen.RuleEntry> section = new ArrayList<>();
        section.add(screen.new CategoryRuleEntry(Component.translatable(MOB_GRIEFING_CATEGORY).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)));
        section.addAll(mobGriefingEntries);
        entries.add(insertIndex(entries), section.remove(0));
        entries.addAll(insertIndex(entries), section);
    }

    private static int insertIndex(List<EditGameRulesScreen.RuleEntry> entries) {
        for (int index = 0; index < entries.size(); index++) {
            EditGameRulesScreen.RuleEntry entry = entries.get(index);
            if (entry instanceof EditGameRulesScreen.CategoryRuleEntry category
                && hasTranslatableKey(((CategoryRuleEntryAccessor)category).tweaked$getLabel(), "gamerule.category.spawning")) {
                return index;
            }
        }

        return entries.size();
    }

    private static boolean isMobGriefingRuleEntry(EditGameRulesScreen.RuleEntry entry) {
        for (var child : entry.children()) {
            if (child instanceof CycleButton<?> cycleButton) {
                Component name = ((CycleButtonAccessor)cycleButton).tweaked$getName();
                if (hasMobGriefingRuleKey(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasMobGriefingRuleKey(Component component) {
        for (GameRules.Key<GameRules.BooleanValue> rule : MOB_GRIEFING_RULES) {
            if (hasTranslatableKey(component, rule.getDescriptionId())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasTranslatableKey(Component component, String key) {
        return component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
    }

    private static List<GameRules.Key<GameRules.BooleanValue>> createMobGriefingRules() {
        List<GameRules.Key<GameRules.BooleanValue>> rules = new ArrayList<>();
        rules.add(GameRules.RULE_MOBGRIEFING);
        rules.addAll(TweakedGameRules.ALL);
        return List.copyOf(rules);
    }
}
