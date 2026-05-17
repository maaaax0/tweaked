package de.maax.tweaked.mixin;

import de.maax.tweaked.server.SpawnControl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
    @Inject(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private static void tweaked$blockDisabledWitherSpawn(Level level, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        if (!level.isClientSide && SpawnControl.isDisabled(EntityType.WITHER)) {
            ci.cancel();
        }
    }

    @Inject(method = "canSpawnMob", at = @At("HEAD"), cancellable = true)
    private static void tweaked$hideDisabledWitherSpawnPreview(Level level, BlockPos pos, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!level.isClientSide && SpawnControl.isDisabled(EntityType.WITHER)) {
            cir.setReturnValue(false);
        }
    }
}
