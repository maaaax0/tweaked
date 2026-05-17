package de.maax.tweaked.mixin;

import de.maax.tweaked.server.FillHistory;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(FillCommand.class)
public abstract class FillCommandMixin {
    private static final ThreadLocal<FillHistory.PendingCapture> TWEAKED_CAPTURE = new ThreadLocal<>();

    @Inject(method = "fillBlocks", at = @At("HEAD"))
    private static void tweaked$captureBefore(
            CommandSourceStack source,
            BoundingBox area,
            BlockInput newBlock,
            @Coerce Object mode,
            Predicate<BlockInWorld> replacingPredicate,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        TWEAKED_CAPTURE.set(FillHistory.beginBlocks(player, source.getLevel(), area));
    }

    @Inject(method = "fillBlocks", at = @At("RETURN"))
    private static void tweaked$captureAfter(
            CommandSourceStack source,
            BoundingBox area,
            BlockInput newBlock,
            @Coerce Object mode,
            Predicate<BlockInWorld> replacingPredicate,
            CallbackInfoReturnable<Integer> cir
    ) {
        FillHistory.finishBlocks(TWEAKED_CAPTURE.get(), source.getLevel());
        TWEAKED_CAPTURE.remove();
    }
}
