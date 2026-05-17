package de.maax.tweaked.client;

import de.maax.tweaked.world.SandboxPresets;
import de.maax.tweaked.world.SandboxPresets.Preset;
import de.maax.tweaked.world.SpawnOptions;
import de.maax.tweaked.world.SpawnOptions.BiomeCategory;
import java.util.Set;
import java.util.Collections;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.FlatLevelSource;

public final class SandboxWorldTypeClient {
    private static Preset selectedPreset = Preset.NORMAL;
    private static final WeakHashMap<WorldCreationUiState, Boolean> PREVIOUS_GENERATE_STRUCTURES = new WeakHashMap<>();
    private static final Set<WorldCreationUiState> APPLYING_STRUCTURE_DEFAULTS = Collections.newSetFromMap(new WeakHashMap<>());

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

    public static void resetSelectionState() {
        selectedPreset = Preset.NORMAL;
        PREVIOUS_GENERATE_STRUCTURES.clear();
        APPLYING_STRUCTURE_DEFAULTS.clear();
        SandboxGameModeClient.resetSelectionState();
    }

    public static void applyDefaultsIfSandbox(WorldCreationUiState uiState) {
        if (!isSandboxSelected(uiState)) {
            restorePreviousStructures(uiState);
            return;
        }

        PREVIOUS_GENERATE_STRUCTURES.putIfAbsent(uiState, uiState.isGenerateStructures());
        selectedPreset = Preset.NORMAL;
        SpawnOptions.setSelectedVillageSpawn(false);
        APPLYING_STRUCTURE_DEFAULTS.add(uiState);
        try {
            uiState.setGenerateStructures(false);
            applyPreset(uiState, selectedPreset);
        } finally {
            APPLYING_STRUCTURE_DEFAULTS.remove(uiState);
        }
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

    public static void applyGenerateStructuresIfSandbox(WorldCreationUiState uiState) {
        if (APPLYING_STRUCTURE_DEFAULTS.contains(uiState)) {
            return;
        }

        if (!isSandboxSelected(uiState)) {
            return;
        }

        SpawnOptions.setSelectedVillageSpawn(uiState.isGenerateStructures());
        applyPreset(uiState, selectedPreset);
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
                uiState.isGenerateStructures(),
                uiState.getSettings().options().seed()
            ))
        ));
    }

    private static void restorePreviousStructures(WorldCreationUiState uiState) {
        Boolean previousGenerateStructures = PREVIOUS_GENERATE_STRUCTURES.remove(uiState);
        if (previousGenerateStructures == null) {
            return;
        }

        APPLYING_STRUCTURE_DEFAULTS.add(uiState);
        try {
            uiState.setGenerateStructures(previousGenerateStructures);
        } finally {
            APPLYING_STRUCTURE_DEFAULTS.remove(uiState);
        }
    }

    private static Component displayName(Preset preset) {
        return Component.translatable("generator.tweaked.sandbox." + preset.id());
    }
}
