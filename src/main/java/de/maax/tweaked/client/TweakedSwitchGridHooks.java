package de.maax.tweaked.client;

import de.maax.tweaked.world.SpawnOptions;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;

public final class TweakedSwitchGridHooks {
    private static final Component VILLAGE_SPAWN = Component.translatable("selectWorld.tweaked.villageSpawn");

    private TweakedSwitchGridHooks() {
    }

    public static Object addVillageSpawnSwitchBefore(
        Object switchGridBuilder,
        WorldCreationUiState uiState,
        Component label,
        BooleanSupplier stateSupplier,
        Consumer<Boolean> onClicked
    ) {
        addVillageSpawnSwitch(switchGridBuilder, uiState);
        return addSwitch(switchGridBuilder, label, stateSupplier, onClicked);
    }

    public static void addVillageSpawnSwitch(Object switchGridBuilder, WorldCreationUiState uiState) {
        Object switchBuilder = addSwitch(
            switchGridBuilder,
            VILLAGE_SPAWN,
            SpawnOptions::selectedVillageSpawn,
            villageSpawn -> SandboxWorldTypeClient.setVillageSpawn(uiState, villageSpawn)
        );
        try {
            Method withIsActiveCondition = switchBuilder.getClass().getMethod("withIsActiveCondition", BooleanSupplier.class);
            withIsActiveCondition.setAccessible(true);
            withIsActiveCondition.invoke(switchBuilder, (BooleanSupplier)(() -> !uiState.isDebug()));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to add Village Spawn switch", exception);
        }
    }

    private static Object addSwitch(Object switchGridBuilder, Component label, BooleanSupplier stateSupplier, Consumer<Boolean> onClicked) {
        try {
            Method addSwitch = switchGridBuilder.getClass().getMethod("addSwitch", Component.class, BooleanSupplier.class, Consumer.class);
            addSwitch.setAccessible(true);
            return addSwitch.invoke(switchGridBuilder, label, stateSupplier, onClicked);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to add switch", exception);
        }
    }
}
