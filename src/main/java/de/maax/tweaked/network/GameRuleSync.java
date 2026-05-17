package de.maax.tweaked.network;

import de.maax.tweaked.Tweaked;
import de.maax.tweaked.server.AdminCommands;
import de.maax.tweaked.server.SpawnControl;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class GameRuleSync {
    private GameRuleSync() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GameRuleSync::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(GameRuleSyncRequest.TYPE, GameRuleSyncRequest.STREAM_CODEC, GameRuleSync::handleRequest);
        registrar.playToClient(GameRuleSyncValues.TYPE, GameRuleSyncValues.STREAM_CODEC, GameRuleSync::handleValues);
        registrar.playToServer(SpawnControlSyncRequest.TYPE, SpawnControlSyncRequest.STREAM_CODEC, GameRuleSync::handleSpawnControlRequest);
        registrar.playToClient(SpawnControlSyncValues.TYPE, SpawnControlSyncValues.STREAM_CODEC, GameRuleSync::handleSpawnControlValues);
        registrar.playToServer(PlayerListSyncRequest.TYPE, PlayerListSyncRequest.STREAM_CODEC, GameRuleSync::handlePlayerListRequest);
        registrar.playToClient(PlayerListSyncValues.TYPE, PlayerListSyncValues.STREAM_CODEC, GameRuleSync::handlePlayerListValues);
        registrar.playToServer(PlayerTeleportRequest.TYPE, PlayerTeleportRequest.STREAM_CODEC, GameRuleSync::handlePlayerTeleportRequest);
    }

    private static void handleRequest(GameRuleSyncRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
            return;
        }

        context.reply(new GameRuleSyncValues(serializeGameRules(player.server.getGameRules())));
    }

    private static void handleValues(GameRuleSyncValues payload, IPayloadContext context) {
        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        try {
            Class<?> handler = Class.forName("de.maax.tweaked.client.TweakedClientGameRuleSync");
            Method method = handler.getMethod("handle", GameRuleSyncValues.class);
            method.invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            Tweaked.LOGGER.warn("Failed to apply game rule sync payload", exception);
        }
    }

    private static void handleSpawnControlRequest(SpawnControlSyncRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
            return;
        }

        context.reply(new SpawnControlSyncValues(new HashSet<>(SpawnControl.disabledTypes())));
    }

    private static void handleSpawnControlValues(SpawnControlSyncValues payload, IPayloadContext context) {
        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        try {
            Class<?> handler = Class.forName("de.maax.tweaked.client.TweakedClientGameRuleSync");
            Method method = handler.getMethod("handle", SpawnControlSyncValues.class);
            method.invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            Tweaked.LOGGER.warn("Failed to apply spawn control sync payload", exception);
        }
    }

    private static void handlePlayerListRequest(PlayerListSyncRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
            return;
        }

        context.reply(new PlayerListSyncValues(knownPlayerProfiles(player.server)));
    }

    private static void handlePlayerListValues(PlayerListSyncValues payload, IPayloadContext context) {
        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        try {
            Class<?> handler = Class.forName("de.maax.tweaked.client.TweakedClientGameRuleSync");
            Method method = handler.getMethod("handle", PlayerListSyncValues.class);
            method.invoke(null, payload);
        } catch (ReflectiveOperationException exception) {
            Tweaked.LOGGER.warn("Failed to apply player list sync payload", exception);
        }
    }

    private static void handlePlayerTeleportRequest(PlayerTeleportRequest payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !player.hasPermissions(2)) {
            return;
        }

        try {
            if (payload.mode() == PlayerTeleportMode.TO_TARGET) {
                AdminCommands.teleportToPlayer(player.createCommandSourceStack(), payload.profile());
            } else {
                AdminCommands.teleportPlayerHere(player.createCommandSourceStack(), payload.profile());
            }
        } catch (CommandSyntaxException exception) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(exception.getMessage()));
        } catch (RuntimeException exception) {
            Tweaked.LOGGER.warn("Failed to run player teleport action for {}", payload.profile().getId(), exception);
        }
    }

    private static Map<String, String> serializeGameRules(GameRules gameRules) {
        Map<String, String> values = new HashMap<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                values.put(key.getId(), gameRules.getRule(key).serialize());
            }
        });
        return values;
    }

    private static List<GameProfile> knownPlayerProfiles(MinecraftServer server) {
        Map<UUID, GameProfile> profiles = new LinkedHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            profiles.put(player.getUUID(), player.getGameProfile());
        }

        Path playerDataDirectory = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (Files.isDirectory(playerDataDirectory)) {
            try (Stream<Path> paths = Files.list(playerDataDirectory)) {
                paths.map(GameRuleSync::uuidFromPlayerDataPath)
                        .flatMap(Optional::stream)
                        .forEach(uuid -> profiles.computeIfAbsent(uuid, id -> profileForOfflinePlayer(server, id)));
            } catch (IOException exception) {
                Tweaked.LOGGER.warn("Failed to scan stored player data", exception);
            }
        }

        return profiles.values().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    private static Optional<UUID> uuidFromPlayerDataPath(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".dat")) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(fileName.substring(0, fileName.length() - 4)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static GameProfile profileForOfflinePlayer(MinecraftServer server, UUID uuid) {
        Optional<GameProfile> cached = server.getProfileCache().get(uuid);
        GameProfile profile = cached.orElseGet(() -> new GameProfile(uuid, uuid.toString().substring(0, 8)));
        if (!profile.getProperties().containsKey("textures")) {
            try {
                ProfileResult result = server.getSessionService().fetchProfile(uuid, true);
                if (result != null) {
                    return result.profile();
                }
            } catch (RuntimeException exception) {
                Tweaked.LOGGER.debug("Could not fetch skin profile for {}", uuid, exception);
            }
        }
        return profile;
    }

    public record GameRuleSyncRequest() implements CustomPacketPayload {
        public static final GameRuleSyncRequest INSTANCE = new GameRuleSyncRequest();
        public static final Type<GameRuleSyncRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "game_rule_sync_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GameRuleSyncRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record GameRuleSyncValues(Map<String, String> values) implements CustomPacketPayload {
        public static final Type<GameRuleSyncValues> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "game_rule_sync_values"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GameRuleSyncValues> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                GameRuleSyncValues::values,
                GameRuleSyncValues::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SpawnControlSyncRequest() implements CustomPacketPayload {
        public static final SpawnControlSyncRequest INSTANCE = new SpawnControlSyncRequest();
        public static final Type<SpawnControlSyncRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "spawn_control_sync_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpawnControlSyncRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SpawnControlSyncValues(Set<ResourceLocation> disabledTypes) implements CustomPacketPayload {
        public static final Type<SpawnControlSyncValues> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "spawn_control_sync_values"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SpawnControlSyncValues> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(HashSet::new, ResourceLocation.STREAM_CODEC),
                SpawnControlSyncValues::disabledTypes,
                SpawnControlSyncValues::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerListSyncRequest() implements CustomPacketPayload {
        public static final PlayerListSyncRequest INSTANCE = new PlayerListSyncRequest();
        public static final Type<PlayerListSyncRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "player_list_sync_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListSyncRequest> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerListSyncValues(List<GameProfile> profiles) implements CustomPacketPayload {
        public static final Type<PlayerListSyncValues> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "player_list_sync_values"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerListSyncValues> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.GAME_PROFILE.apply(ByteBufCodecs.list()),
                PlayerListSyncValues::profiles,
                PlayerListSyncValues::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public enum PlayerTeleportMode {
        TO_TARGET,
        HERE
    }

    public record PlayerTeleportRequest(GameProfile profile, PlayerTeleportMode mode) implements CustomPacketPayload {
        public static final Type<PlayerTeleportRequest> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "player_teleport_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTeleportRequest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.GAME_PROFILE,
                PlayerTeleportRequest::profile,
                ByteBufCodecs.VAR_INT.map(index -> PlayerTeleportMode.values()[index], PlayerTeleportMode::ordinal),
                PlayerTeleportRequest::mode,
                PlayerTeleportRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
