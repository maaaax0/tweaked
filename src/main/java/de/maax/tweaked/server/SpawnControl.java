package de.maax.tweaked.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.maax.tweaked.Tweaked;
import de.maax.tweaked.world.SandboxPresets;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class SpawnControl {
    private static final Logger LOGGER = Tweaked.LOGGER;
    private static final String FILE_NAME = "tweaked-spawning.properties";
    private static final SimpleCommandExceptionType ERROR_UNKNOWN_ENTITY = new SimpleCommandExceptionType(Component.literal("Unknown entity type"));
    private static final Set<ResourceLocation> DISABLED_TYPES = new HashSet<>();

    private static Path configPath;

    private SpawnControl() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SpawnControl::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SpawnControl::onSpawnPlacementCheck);
        NeoForge.EVENT_BUS.addListener(SpawnControl::onSpawnPositionCheck);
        NeoForge.EVENT_BUS.addListener(SpawnControl::onFinalizeSpawn);
        NeoForge.EVENT_BUS.addListener(SpawnControl::onEntityJoinLevel);
    }

    public static void load(MinecraftServer server) {
        configPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve(FILE_NAME);
        DISABLED_TYPES.clear();

        if (!Files.isRegularFile(configPath)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException exception) {
            LOGGER.warn("Failed to load spawn control config", exception);
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            if (Boolean.parseBoolean(properties.getProperty(key))) {
                ResourceLocation location = ResourceLocation.tryParse(key);
                if (location != null) {
                    DISABLED_TYPES.add(location);
                }
            }
        }
    }

    public static Set<ResourceLocation> disabledTypes() {
        return Collections.unmodifiableSet(DISABLED_TYPES);
    }

    public static boolean isDisabled(EntityType<?> entityType) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        return key != null && DISABLED_TYPES.contains(key);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("tweaked")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawning")
                        .then(Commands.literal("set")
                                .then(Commands.argument("entity", ResourceLocationArgument.id())
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> setSpawning(
                                                        context.getSource(),
                                                        ResourceLocationArgument.getId(context, "entity"),
                                                        BoolArgumentType.getBool(context, "enabled")
                                                )))))
                        .then(Commands.literal("reset")
                                .executes(context -> resetSpawning(context.getSource()))
                                .then(Commands.argument("namespace", StringArgumentType.word())
                                        .executes(context -> resetSpawning(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "namespace")
                                        ))))
                        .then(Commands.literal("list")
                                .executes(context -> listDisabled(context.getSource())))));
    }

    private static int setSpawning(CommandSourceStack source, ResourceLocation location, boolean enabled) throws CommandSyntaxException {
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(location);
        if (entityType == null || entityType == EntityType.PLAYER) {
            throw ERROR_UNKNOWN_ENTITY.create();
        }

        if (enabled) {
            DISABLED_TYPES.remove(location);
        } else {
            DISABLED_TYPES.add(location);
        }
        save();

        source.sendSuccess(() -> Component.literal((enabled ? "Enabled " : "Disabled ") + location + " spawning"), true);
        return 1;
    }

    private static int listDisabled(CommandSourceStack source) {
        if (DISABLED_TYPES.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No entity spawning is disabled"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Disabled spawning: " + String.join(", ", DISABLED_TYPES.stream().map(ResourceLocation::toString).sorted().toList())), false);
        return DISABLED_TYPES.size();
    }

    private static int resetSpawning(CommandSourceStack source) {
        DISABLED_TYPES.clear();
        save();
        source.sendSuccess(() -> Component.literal("Enabled all entity spawning"), true);
        return 1;
    }

    private static int resetSpawning(CommandSourceStack source, String namespace) {
        DISABLED_TYPES.removeIf(location -> location.getNamespace().equals(namespace));
        save();
        source.sendSuccess(() -> Component.literal("Enabled " + namespace + " entity spawning"), true);
        return 1;
    }

    private static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (SandboxPresets.hasSandboxGameRules(event.getLevel().getLevel().getGameRules()) || isDisabled(event.getEntityType())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static void onSpawnPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (shouldBlockMob(event.getEntity())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (shouldBlockMob(event.getEntity())) {
            event.setSpawnCancelled(true);
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!event.loadedFromDisk() && event.getEntity() instanceof Mob mob && shouldBlockMob(mob)) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldBlockMob(Mob mob) {
        return SandboxPresets.hasSandboxGameRules(mob.level().getGameRules()) || isDisabled(mob.getType());
    }

    private static void save() {
        if (configPath == null) {
            return;
        }

        Properties properties = new Properties();
        for (ResourceLocation disabledType : DISABLED_TYPES) {
            properties.setProperty(disabledType.toString(), Boolean.TRUE.toString());
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "Tweaked mob spawning settings. true means spawning is disabled.");
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save spawn control config", exception);
        }
    }
}
