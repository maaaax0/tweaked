package de.maax.tweaked.client;

import de.maax.tweaked.mixin.CycleButtonAccessor;
import de.maax.tweaked.mixin.WorldCreationUiStateAccessor;
import de.maax.tweaked.world.TweakedGameRules;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;

public final class SandboxGameModeClient {
    private static final Component SANDBOX = Component.translatable("selectWorld.gameMode.sandbox");
    private static final Component SANDBOX_INFO = Component.translatable("selectWorld.gameMode.sandbox.info");
    private static final Set<WorldCreationUiState> SANDBOX_STATES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<WorldCreationUiState> APPLYING_STATES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final WeakHashMap<WorldCreationUiState, PreviousSettings> PREVIOUS_SETTINGS = new WeakHashMap<>();

    private SandboxGameModeClient() {
    }

    public static void resetSelectionState() {
        SANDBOX_STATES.clear();
        APPLYING_STATES.clear();
        PREVIOUS_SETTINGS.clear();
    }

    public static boolean isSandbox(WorldCreationUiState uiState) {
        return SANDBOX_STATES.contains(uiState);
    }

    public static boolean handleGameModeSelection(WorldCreationUiState uiState, WorldCreationUiState.SelectedGameMode gameMode) {
        if (APPLYING_STATES.contains(uiState)) {
            return false;
        }

        if (gameMode == WorldCreationUiState.SelectedGameMode.CREATIVE && uiState.getGameMode() == WorldCreationUiState.SelectedGameMode.SURVIVAL) {
            applySandboxDefaults(uiState);
            return true;
        }

        if (gameMode == WorldCreationUiState.SelectedGameMode.CREATIVE && uiState.getGameMode() == WorldCreationUiState.SelectedGameMode.CREATIVE) {
            if (isSandbox(uiState)) {
                restorePreviousSettings(uiState, WorldCreationUiState.SelectedGameMode.SURVIVAL);
                return true;
            }
        }

        if (isSandbox(uiState)) {
            restorePreviousSettings(uiState, gameMode);
            return true;
        }

        return false;
    }

    public static void updateGameModeButton(WorldCreationUiState uiState, CycleButton<WorldCreationUiState.SelectedGameMode> button) {
        if (isSandbox(uiState)) {
            button.setMessage(CommonComponents.optionNameValue(Component.translatable("selectWorld.gameMode"), SANDBOX));
            button.setTooltip(Tooltip.create(SANDBOX_INFO));
            return;
        }

        if (uiState.getGameMode() == WorldCreationUiState.SelectedGameMode.CREATIVE) {
            ((CycleButtonAccessor)button).tweaked$setIndex(3);
        }
    }

    public static GameRules createSandboxGameRules(GameRules source) {
        GameRules gameRules = source.copy();
        gameRules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, null);
        gameRules.getRule(GameRules.RULE_MOBGRIEFING).set(true, null);
        TweakedGameRules.ALL.forEach(rule -> gameRules.getRule(rule).set(true, null));
        return gameRules;
    }

    private static void applySandboxDefaults(WorldCreationUiState uiState) {
        PREVIOUS_SETTINGS.putIfAbsent(uiState, PreviousSettings.capture(uiState));
        SANDBOX_STATES.add(uiState);

        APPLYING_STATES.add(uiState);
        try {
            uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            uiState.setAllowCommands(true);
            uiState.setDifficulty(Difficulty.NORMAL);
            uiState.setGameRules(createSandboxGameRules(uiState.getGameRules()));
        } finally {
            APPLYING_STATES.remove(uiState);
        }
    }

    private static void restorePreviousSettings(WorldCreationUiState uiState, WorldCreationUiState.SelectedGameMode gameMode) {
        PreviousSettings settings = PREVIOUS_SETTINGS.remove(uiState);
        SANDBOX_STATES.remove(uiState);

        APPLYING_STATES.add(uiState);
        try {
            uiState.setGameMode(gameMode);
            if (settings != null) {
                ((WorldCreationUiStateAccessor)uiState).tweaked$setAllowCommands(settings.allowCommands);
                uiState.setDifficulty(settings.difficulty);
                uiState.setGameRules(settings.gameRules.copy());
                uiState.onChanged();
            }
        } finally {
            APPLYING_STATES.remove(uiState);
        }
    }

    private record PreviousSettings(
        Boolean allowCommands,
        Difficulty difficulty,
        GameRules gameRules
    ) {
        private static PreviousSettings capture(WorldCreationUiState uiState) {
            return new PreviousSettings(
                ((WorldCreationUiStateAccessor)uiState).tweaked$getAllowCommands(),
                uiState.getDifficulty(),
                uiState.getGameRules().copy()
            );
        }
    }
}
