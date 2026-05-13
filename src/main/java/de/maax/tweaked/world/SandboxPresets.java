package de.maax.tweaked.world;

import de.maax.tweaked.Tweaked;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

public final class SandboxPresets {
    public static final ResourceKey<WorldPreset> SANDBOX_WORLD_PRESET = ResourceKey.create(
        Registries.WORLD_PRESET,
        ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "sandbox")
    );

    private SandboxPresets() {
    }

    public static FlatLevelGeneratorSettings createSettings(
        RegistryAccess registryAccess,
        Preset preset,
        SpawnOptions.BiomeCategory biomeCategory,
        boolean villageSpawn,
        long seed
    ) {
        HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderGetter<StructureSet> structureSets = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        FlatLevelGeneratorSettings settings = new FlatLevelGeneratorSettings(
            villageSpawn ? Optional.of(HolderSet.direct(structureSets.getOrThrow(BuiltinStructureSets.VILLAGES))) : Optional.empty(),
            biomes.getOrThrow(biomeCategory.selectBiome(seed)),
            FlatLevelGeneratorSettings.createLakesList(placedFeatures)
        );

        for (Layer layer : preset.layers) {
            settings.getLayersInfo().add(new FlatLayerInfo(layer.height, layer.block));
        }

        settings.updateLayers();
        return settings;
    }

    public static void applyStartupWeatherAndTime(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (!isSandboxLike(overworld) || !hasSandboxGameRules(overworld.getGameRules())) {
            return;
        }

        overworld.setDayTime(6000L);
        overworld.setWeatherParameters(6000, 0, false, false);
    }

    private static boolean isSandboxLike(ServerLevel level) {
        if (!(level.getChunkSource().getGenerator() instanceof FlatLevelSource flatLevelSource)) {
            return false;
        }

        List<FlatLayerInfo> layers = flatLevelSource.settings().getLayersInfo();
        for (Preset preset : Preset.values()) {
            if (matches(layers, preset)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(List<FlatLayerInfo> layers, Preset preset) {
        if (layers.size() != preset.layers.size()) {
            return false;
        }

        for (int i = 0; i < layers.size(); i++) {
            FlatLayerInfo layer = layers.get(i);
            Layer expected = preset.layers.get(i);
            if (layer.getHeight() != expected.height || layer.getBlockState().getBlock() != expected.block) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasSandboxGameRules(GameRules gameRules) {
        return !gameRules.getBoolean(GameRules.RULE_DAYLIGHT)
            && !gameRules.getBoolean(GameRules.RULE_WEATHER_CYCLE)
            && !gameRules.getBoolean(GameRules.RULE_DOMOBSPAWNING)
            && !gameRules.getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)
            && !gameRules.getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)
            && !gameRules.getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING)
            && gameRules.getBoolean(GameRules.RULE_DISABLE_RAIDS)
            && gameRules.getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    public enum Preset {
        NORMAL("normal", List.of(
            new Layer(1, Blocks.BEDROCK),
            new Layer(5, Blocks.STONE),
            new Layer(10, Blocks.DIRT),
            new Layer(1, Blocks.GRASS_BLOCK)
        )),
        REDSTONE("redstone", List.of(
            new Layer(1, Blocks.BEDROCK),
            new Layer(32, Blocks.SMOOTH_SANDSTONE)
        )),
        BUILDING("building", List.of(
            new Layer(1, Blocks.BEDROCK),
            new Layer(64, Blocks.STONE),
            new Layer(1, Blocks.GRASS_BLOCK)
        )),
        CLEAN("clean", List.of(
            new Layer(1, Blocks.BEDROCK),
            new Layer(64, Blocks.WHITE_CONCRETE)
        ));

        private final String id;
        private final List<Layer> layers;

        Preset(String id, List<Layer> layers) {
            this.id = id;
            this.layers = layers;
        }

        public String id() {
            return this.id;
        }

        public Preset next() {
            Preset[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    private record Layer(int height, Block block) {
    }
}
