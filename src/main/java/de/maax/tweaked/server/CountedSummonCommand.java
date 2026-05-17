package de.maax.tweaked.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class CountedSummonCommand {
    private CountedSummonCommand() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CountedSummonCommand::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("summon")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(
                            Commands.argument("count", IntegerArgumentType.integer(1))
                                .then(
                                    Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(command -> spawnEntities(
                                            command,
                                            Vec3Argument.getVec3(command, "pos"),
                                            new CompoundTag(),
                                            true
                                        ))
                                        .then(
                                            Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(command -> spawnEntities(
                                                    command,
                                                    Vec3Argument.getVec3(command, "pos"),
                                                    CompoundTagArgument.getCompoundTag(command, "nbt"),
                                                    false
                                                ))
                                        )
                                )
                        )
                )
        );
    }

    private static int spawnEntities(
        CommandContext<CommandSourceStack> command,
        Vec3 pos,
        CompoundTag tag,
        boolean randomizeProperties
    ) throws CommandSyntaxException {
        CommandSourceStack source = command.getSource();
        Holder.Reference<EntityType<?>> type = ResourceArgument.getSummonableEntityType(command, "entity");
        int count = IntegerArgumentType.getInteger(command, "count");
        Entity firstEntity = null;

        for (int index = 0; index < count; index++) {
            Entity entity = SummonCommand.createEntity(source, type, pos, tag, randomizeProperties);
            if (firstEntity == null) {
                firstEntity = entity;
            }
        }

        Entity displayEntity = firstEntity;
        source.sendSuccess(
            () -> count == 1
                ? Component.translatable("commands.summon.success", displayEntity.getDisplayName())
                : Component.translatable("commands.tweaked.summon.success.multiple", count, displayEntity.getDisplayName()),
            true
        );
        return count;
    }
}
