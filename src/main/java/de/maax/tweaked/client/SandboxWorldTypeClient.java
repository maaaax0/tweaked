package de.maax.tweaked.client;

import de.maax.tweaked.world.SandboxPresets;
import de.maax.tweaked.world.SandboxPresets.Preset;
import de.maax.tweaked.world.SpawnOptions;
import de.maax.tweaked.world.SpawnOptions.BiomeCategory;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;

public final class SandboxWorldTypeClient {
    private static Preset selectedPreset = Preset.NORMAL;
    private static PreviousSettings previousSettings;

    private SandboxWorldTypeClient() {
    }

    public static boolean isSandboxSelected(WorldCreationUiState uiState) {
        Holder<?> preset = uiState.getWorldType().preset();
        return preset != null
            && preset
            .unwrapKey()
            .filter(SandboxPresets.SANDBOX_WORLD_PRESET::equals)
            .isPresent();
    }

    public static void applyDefaultsIfSandbox(WorldCreationUiState uiState) {
        if (!isSandboxSelected(uiState)) {
            restorePreviousSettings(uiState);
            return;
        }

        if (previousSettings == null) {
            previousSettings = PreviousSettings.capture(uiState);
        }

        selectedPreset = Preset.NORMAL;
        uiState.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        uiState.setAllowCommands(true);
        uiState.setDifficulty(Difficulty.NORMAL);
        uiState.setGenerateStructures(SpawnOptions.selectedVillageSpawn());
        uiState.setGameRules(createSandboxGameRules(uiState.getGameRules()));
        applyPreset(uiState, selectedPreset);
    }

    public static void resetSelectionState() {
        previousSettings = null;
        selectedPreset = Preset.NORMAL;
    }

    public static boolean preventSandboxStructures(WorldCreationUiState uiState, boolean generateStructures) {
        return generateStructures && isSandboxSelected(uiState) && !SpawnOptions.selectedVillageSpawn();
    }

    public static void setVillageSpawn(WorldCreationUiState uiState, boolean villageSpawn) {
        SpawnOptions.setSelectedVillageSpawn(villageSpawn);
        if (villageSpawn) {
            uiState.setGenerateStructures(true);
        }

        if (isSandboxSelected(uiState)) {
            if (!villageSpawn) {
                uiState.setGenerateStructures(false);
            }

            applyPreset(uiState, selectedPreset);
        }
    }

    public static void setBiome(WorldCreationUiState uiState, BiomeCategory biome) {
        SpawnOptions.setSelectedBiome(biome);
        if (isSandboxSelected(uiState)) {
            applyPreset(uiState, selectedPreset);
        }
    }

    public static void prepareSpawnOptions(WorldCreationUiState uiState) {
        SpawnOptions.prepareForNextWorld(uiState.getSettings().options().seed());
    }

    public static void updateSandboxButton(WorldCreationUiState uiState, Button button) {
        if (isSandboxSelected(uiState)) {
            button.active = !uiState.isDebug();
            button.setMessage(CommonComponents.optionNameValue(Component.translatable("generator.tweaked.sandbox"), displayName(selectedPreset)));
        } else {
            button.setMessage(Component.translatable("selectWorld.customizeType"));
        }
    }

    public static boolean cycleSandboxPreset(WorldCreationUiState uiState) {
        if (!isSandboxSelected(uiState)) {
            return false;
        }

        selectedPreset = selectedPreset.next();
        applyPreset(uiState, selectedPreset);
        return true;
    }

    private static void applyPreset(WorldCreationUiState uiState, Preset preset) {
        uiState.updateDimensions((registryAccess, dimensions) -> dimensions.replaceOverworldGenerator(
            registryAccess,
            new FlatLevelSource(SandboxPresets.createSettings(
                registryAccess,
                preset,
                SpawnOptions.selectedBiome(),
                SpawnOptions.selectedVillageSpawn(),
                uiState.getSettings().options().seed()
            ))
        ));
    }

    private static GameRules createSandboxGameRules(GameRules source) {
        GameRules gameRules = source.copy();
        gameRules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        gameRules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, null);
        gameRules.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, null);
        gameRules.getRule(GameRules.RULE_MOBGRIEFING).set(true, null);
        return gameRules;
    }

    private static void restorePreviousSettings(WorldCreationUiState uiState) {
        if (previousSettings == null) {
            return;
        }

        PreviousSettings settings = previousSettings;
        previousSettings = null;
        uiState.setGameMode(settings.gameMode);
        uiState.setAllowCommands(settings.allowCommands);
        uiState.setDifficulty(settings.difficulty);
        uiState.setGenerateStructures(settings.generateStructures);
        uiState.setGameRules(settings.gameRules.copy());
    }

    private static Component displayName(Preset preset) {
        return Component.translatable("generator.tweaked.sandbox." + preset.id());
    }

    private record PreviousSettings(
        WorldCreationUiState.SelectedGameMode gameMode,
        boolean allowCommands,
        Difficulty difficulty,
        boolean generateStructures,
        GameRules gameRules
    ) {
        private static PreviousSettings capture(WorldCreationUiState uiState) {
            return new PreviousSettings(
                uiState.getGameMode(),
                uiState.isAllowCommands(),
                uiState.getDifficulty(),
                uiState.isGenerateStructures(),
                uiState.getGameRules().copy()
            );
        }
    }
}
