package de.maax.tweaked.mixin;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "spawnMobsForChunkGeneration", at = @At("HEAD"), cancellable = true)
    private static void tweaked$respectDoMobSpawningForChunkGeneration(
        ServerLevelAccessor level,
        Holder<Biome> biome,
        ChunkPos chunkPos,
        RandomSource random,
        CallbackInfo ci
    ) {
        if (!level.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            ci.cancel();
        }
    }
}
