package de.maax.tweaked.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void tweaked$registerForwardOnlyTimeAliases(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        dispatcher.register(
            Commands.literal("time")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.literal("set")
                        .then(Commands.literal("day").executes(context -> tweaked$advanceToNextTime(context.getSource(), 1000)))
                        .then(Commands.literal("noon").executes(context -> tweaked$advanceToNextTime(context.getSource(), 6000)))
                        .then(Commands.literal("night").executes(context -> tweaked$advanceToNextTime(context.getSource(), 13000)))
                        .then(Commands.literal("midnight").executes(context -> tweaked$advanceToNextTime(context.getSource(), 18000)))
                        .then(
                            Commands.argument("time", TimeArgument.time())
                                .executes(context -> TimeCommand.setTime(context.getSource(), IntegerArgumentType.getInteger(context, "time")))
                        )
                )
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("time", TimeArgument.time())
                                .executes(context -> TimeCommand.addTime(context.getSource(), IntegerArgumentType.getInteger(context, "time")))
                        )
                )
                .then(
                    Commands.literal("query")
                        .then(Commands.literal("daytime").executes(context -> tweaked$queryTime(context.getSource(), tweaked$getAbsoluteDayTime(context.getSource().getLevel()))))
                        .then(
                            Commands.literal("gametime")
                                .executes(context -> tweaked$queryTime(context.getSource(), (int)(context.getSource().getLevel().getGameTime() % 2147483647L)))
                        )
                        .then(
                            Commands.literal("day")
                                .executes(context -> tweaked$queryTime(
                                    context.getSource(),
                                    (int)(context.getSource().getLevel().getDayTime() / 24000L % 2147483647L)
                                ))
                        )
                )
        );

        ci.cancel();
    }

    @Unique
    private static int tweaked$advanceToNextTime(CommandSourceStack source, int targetTime) {
        long sourceTargetTime = tweaked$getNextAbsoluteTime(source.getLevel().getDayTime(), targetTime);
        long addedTime = sourceTargetTime - source.getLevel().getDayTime();

        for (ServerLevel level : source.getServer().getAllLevels()) {
            level.setDayTime(tweaked$getNextAbsoluteTime(level.getDayTime(), targetTime));
        }

        source.sendSuccess(() -> Component.translatable("commands.tweaked.time.advance", addedTime, sourceTargetTime), true);
        return tweaked$getDayTime(source.getLevel());
    }

    @Unique
    private static long tweaked$getNextAbsoluteTime(long currentTime, int targetTime) {
        long currentDay = Math.floorDiv(currentTime, 24000L);
        long nextTime = currentDay * 24000L + targetTime;
        return nextTime <= currentTime ? nextTime + 24000L : nextTime;
    }

    @Unique
    private static int tweaked$getDayTime(ServerLevel level) {
        return (int)Math.floorMod(level.getDayTime(), 24000L);
    }

    @Unique
    private static int tweaked$getAbsoluteDayTime(ServerLevel level) {
        return (int)(level.getDayTime() % 2147483647L);
    }

    @Unique
    private static int tweaked$queryTime(CommandSourceStack source, int time) {
        source.sendSuccess(() -> Component.translatable("commands.time.query", time), false);
        return time;
    }
}
