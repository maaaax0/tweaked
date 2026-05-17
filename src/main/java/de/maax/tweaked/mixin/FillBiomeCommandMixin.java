package de.maax.tweaked.mixin;

import de.maax.tweaked.server.FillHistory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(FillBiomeCommand.class)
public abstract class FillBiomeCommandMixin {
    private static final ThreadLocal<FillHistory.PendingCapture> TWEAKED_CAPTURE = new ThreadLocal<>();

    @Inject(method = "fill(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder$Reference;Ljava/util/function/Predicate;)I", at = @At("HEAD"))
    private static void tweaked$captureBefore(
            CommandSourceStack source,
            BlockPos from,
            BlockPos to,
            Holder.Reference<Biome> biome,
            Predicate<Holder<Biome>> filter,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TWEAKED_CAPTURE.set(FillHistory.beginBiomes(player, source.getLevel(), BoundingBox.fromCorners(from, to)));
    }

    @Inject(method = "fill(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder$Reference;Ljava/util/function/Predicate;)I", at = @At("RETURN"))
    private static void tweaked$captureAfter(
            CommandSourceStack source,
            BlockPos from,
            BlockPos to,
            Holder.Reference<Biome> biome,
            Predicate<Holder<Biome>> filter,
            CallbackInfoReturnable<Integer> cir
    ) {
        FillHistory.finishBiomes(TWEAKED_CAPTURE.get(), source.getLevel());
        TWEAKED_CAPTURE.remove();
    }
}
