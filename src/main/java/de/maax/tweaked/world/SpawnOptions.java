package de.maax.tweaked.world;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public final class SpawnOptions {
    private static boolean selectedVillageSpawn;
    private static BiomeCategory selectedBiome = BiomeCategory.PLAINS;
    private static Pending pending;

    private SpawnOptions() {
    }

    public static boolean selectedVillageSpawn() {
        return selectedVillageSpawn;
    }

    public static void setSelectedVillageSpawn(boolean villageSpawn) {
        selectedVillageSpawn = villageSpawn;
    }

    public static void resetSelected() {
        selectedVillageSpawn = false;
        selectedBiome = BiomeCategory.PLAINS;
    }

    public static BiomeCategory selectedBiome() {
        return selectedBiome;
    }

    public static void setSelectedBiome(BiomeCategory biome) {
        selectedBiome = biome;
    }

    public static void prepareForNextWorld(long seed) {
        pending = new Pending(selectedVillageSpawn, selectedBiome, seed);
    }

    public static void applyPendingSpawn(MinecraftServer server) {
        Pending options = pending;
        pending = null;
        if (options == null) {
            return;
        }

        ServerLevel overworld = server.overworld();
        BlockPos start = overworld.getSharedSpawnPos();
        BlockPos target = null;

        if (options.villageSpawn) {
            target = overworld.findNearestMapStructure(StructureTags.VILLAGE, start, 128, false);
        }

        if (target == null) {
            target = findBiomeSpawn(overworld, start, options.biome);
        }

        if (target != null) {
            BlockPos spawn = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target);
            overworld.setDefaultSpawnPos(spawn, 0.0F);
        }
    }

    private static BlockPos findBiomeSpawn(ServerLevel level, BlockPos start, BiomeCategory category) {
        Pair<BlockPos, Holder<Biome>> result = level.findClosestBiome3d(category::matches, start, 8192, 64, 64);
        return result != null ? result.getFirst() : null;
    }

    private record Pending(boolean villageSpawn, BiomeCategory biome, long seed) {
    }

    public enum BiomeCategory {
        PLAINS("plains", List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.SNOWY_PLAINS)),
        FOREST(
            "forest",
            List.of(
                Biomes.FOREST,
                Biomes.FLOWER_FOREST,
                Biomes.BIRCH_FOREST,
                Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.DARK_FOREST,
                Biomes.WINDSWEPT_FOREST
            )
        ),
        JUNGLE("jungle", List.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE)),
        SWAMP("swamp", List.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP)),
        DESERT("desert", List.of(Biomes.DESERT)),
        MUSHROOM("mushroom", List.of(Biomes.MUSHROOM_FIELDS)),
        CHERRY("cherry", List.of(Biomes.CHERRY_GROVE)),
        TAIGA(
            "taiga",
            List.of(
                Biomes.TAIGA,
                Biomes.SNOWY_TAIGA,
                Biomes.OLD_GROWTH_PINE_TAIGA,
                Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                Biomes.GROVE,
                Biomes.SNOWY_SLOPES,
                Biomes.FROZEN_PEAKS,
                Biomes.JAGGED_PEAKS,
                Biomes.ICE_SPIKES
            )
        ),
        SAVANNA(
            "savanna",
            List.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS)
        ),
        BEACH("beach", List.of(Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.RIVER, Biomes.FROZEN_RIVER)),
        BADLANDS("badlands", List.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS, Biomes.STONY_PEAKS));

        private final String id;
        private final List<ResourceKey<Biome>> biomes;

        BiomeCategory(String id, List<ResourceKey<Biome>> biomes) {
            this.id = id;
            this.biomes = biomes;
        }

        public String id() {
            return this.id;
        }

        public boolean matches(Holder<Biome> biome) {
            Optional<ResourceKey<Biome>> key = biome.unwrapKey();
            return key.isPresent() && this.biomes.contains(key.get());
        }

        public ResourceKey<Biome> selectBiome(long seed) {
            RandomSource random = RandomSource.create(seed + this.ordinal() * 341873128712L);
            return this.biomes.get(random.nextInt(this.biomes.size()));
        }

        public BiomeCategory next() {
            BiomeCategory[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
