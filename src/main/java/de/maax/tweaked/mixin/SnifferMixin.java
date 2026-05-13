package de.maax.tweaked.mixin;

import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sniffer.class)
public abstract class SnifferMixin {
    @Inject(method = "dropSeed", at = @At("HEAD"), cancellable = true)
    private void tweaked$useSnifferDiggingRule(CallbackInfo ci) {
        Sniffer sniffer = (Sniffer)(Object)this;
        if (!sniffer.level().getGameRules().getBoolean(TweakedGameRules.SNIFFER_DIGGING)) {
            ci.cancel();
        }
    }
}
