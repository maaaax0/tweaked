package de.maax.tweaked.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import de.maax.tweaked.TweakedConfig;
import de.maax.tweaked.menu.InvSeeMenu;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminCommands {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.literal("Expected a single player"));
    private static final SimpleCommandExceptionType ERROR_NO_STORED_PLAYER_DATA = new SimpleCommandExceptionType(Component.literal("No stored player data found"));
    private static final SimpleCommandExceptionType ERROR_DIMENSION_NOT_FOUND = new SimpleCommandExceptionType(Component.literal("Target dimension is not loaded"));
    private static final Set<UUID> FLYING_PLAYERS = new HashSet<>();
    private static final Set<UUID> GOD_PLAYERS = new HashSet<>();

    private AdminCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AdminCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerChangedGameMode);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("fly")
                .requires(source -> source.hasPermission(2) && TweakedConfig.flyEnabled())
                .executes(context -> setFly(context, self(context), null))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> setFly(context, EntityArgument.getPlayers(context, "targets"), null))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setFly(context, EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled"))))));

        dispatcher.register(Commands.literal("god")
                .requires(source -> source.hasPermission(2) && TweakedConfig.godEnabled())
                .executes(context -> setGod(context, self(context), null))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> setGod(context, EntityArgument.getPlayers(context, "targets"), null))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setGod(context, EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled"))))));

        dispatcher.register(Commands.literal("heal")
                .requires(source -> source.hasPermission(2) && TweakedConfig.healEnabled())
                .executes(context -> heal(context, self(context)))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> heal(context, EntityArgument.getPlayers(context, "targets")))));

        dispatcher.register(Commands.literal("feed")
                .requires(source -> source.hasPermission(2) && TweakedConfig.feedEnabled())
                .executes(context -> feed(context, self(context)))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> feed(context, EntityArgument.getPlayers(context, "targets")))));

        dispatcher.register(Commands.literal("invsee")
                .requires(source -> source.hasPermission(2) && TweakedConfig.invSeeEnabled())
                .executes(context -> openInventory(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openInventory(context, singleProfile(context, "target")))));

        dispatcher.register(Commands.literal("endersee")
                .requires(source -> source.hasPermission(2) && TweakedConfig.enderSeeEnabled())
                .executes(context -> openEnderChest(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openEnderChest(context, singleProfile(context, "target")))));

        dispatcher.register(Commands.literal("tweaked")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("gamerules")
                        .then(Commands.literal("reset")
                                .executes(AdminCommands::resetGameRules))));

        dispatcher.register(Commands.literal("undo")
                .requires(source -> source.hasPermission(2))
                .executes(context -> FillHistory.undo(context.getSource())));

        dispatcher.register(Commands.literal("redo")
                .requires(source -> source.hasPermission(2))
                .executes(context -> FillHistory.redo(context.getSource())));

        registerDimensionTeleportShortcuts(dispatcher);
    }

    private static void registerDimensionTeleportShortcuts(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> tp = dispatcher.getRoot().getChild("tp");
        if (tp == null) {
            dispatcher.register(Commands.literal("tp").requires(source -> source.hasPermission(2)));
            tp = dispatcher.getRoot().getChild("tp");
        }

        addDimensionTeleportShortcuts(tp);

        CommandNode<CommandSourceStack> teleport = dispatcher.getRoot().getChild("teleport");
        if (teleport != null && teleport != tp) {
            addDimensionTeleportShortcuts(teleport);
        }
    }

    private static void addDimensionTeleportShortcuts(CommandNode<CommandSourceStack> command) {
        command.addChild(Commands.literal("end")
                .requires(source -> source.hasPermission(2))
                .executes(AdminCommands::teleportToEnd)
                .build());
        command.addChild(Commands.literal("nether")
                .requires(source -> source.hasPermission(2))
                .executes(AdminCommands::teleportToNether)
                .build());
        command.addChild(Commands.literal("overworld")
                .requires(source -> source.hasPermission(2))
                .executes(AdminCommands::teleportToOverworld)
                .build());

        CommandNode<CommandSourceStack> targets = command.getChild("targets");
        if (targets == null) {
            command.addChild(Commands.argument("targets", EntityArgument.players())
                    .requires(source -> source.hasPermission(2))
                    .build());
            targets = command.getChild("targets");
        }

        targets.addChild(Commands.literal("end")
                .requires(source -> source.hasPermission(2))
                .executes(context -> teleportToEnd(context, EntityArgument.getPlayers(context, "targets")))
                .build());
        targets.addChild(Commands.literal("nether")
                .requires(source -> source.hasPermission(2))
                .executes(context -> teleportToNether(context, EntityArgument.getPlayers(context, "targets")))
                .build());
        targets.addChild(Commands.literal("overworld")
                .requires(source -> source.hasPermission(2))
                .executes(context -> teleportToOverworld(context, EntityArgument.getPlayers(context, "targets")))
                .build());
    }

    private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return Set.of(context.getSource().getPlayerOrException());
    }

    private static int resetGameRules(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        server.getGameRules().assignFrom(new net.minecraft.world.level.GameRules(), server);
        sendSuccess(context.getSource(), "Reset game rules to defaults");
        return 1;
    }

    private static GameProfile singleProfile(CommandContext<CommandSourceStack> context, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, name);
        if (profiles.size() != 1) {
            throw ERROR_SINGLE_PLAYER.create();
        }
        return profiles.iterator().next();
    }

    private static int setFly(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, Boolean enabled) {
        Boolean finalState = null;
        for (ServerPlayer player : targets) {
            boolean shouldEnable = enabled != null ? enabled : !FLYING_PLAYERS.contains(player.getUUID());
            setFly(player, shouldEnable);
            finalState = shouldEnable;
        }

        sendSuccess(context.getSource(), abilityFeedback("flight", enabled, finalState, targets));
        return targets.size();
    }

    private static void setFly(ServerPlayer player, boolean enabled) {
        if (enabled) {
            FLYING_PLAYERS.add(player.getUUID());
        } else {
            FLYING_PLAYERS.remove(player.getUUID());
        }
        applyFlyAbility(player);
    }

    private static int setGod(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets, Boolean enabled) {
        Boolean finalState = null;
        for (ServerPlayer player : targets) {
            boolean shouldEnable = enabled != null ? enabled : !GOD_PLAYERS.contains(player.getUUID());
            if (shouldEnable) {
                GOD_PLAYERS.add(player.getUUID());
            } else {
                GOD_PLAYERS.remove(player.getUUID());
            }
            finalState = shouldEnable;
        }

        sendSuccess(context.getSource(), abilityFeedback("god mode", enabled, finalState, targets));
        return targets.size();
    }

    private static int heal(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.setHealth(player.getMaxHealth());
        }

        sendSuccess(context.getSource(), targets.size() == 1
                ? "Healed " + targetLabel(targets)
                : "Healed " + targets.size() + " players");
        return targets.size();
    }

    private static int feed(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            FoodData foodData = player.getFoodData();
            foodData.setFoodLevel(20);
            foodData.setSaturation(20.0F);
        }

        sendSuccess(context.getSource(), targets.size() == 1
                ? "Fed " + targetLabel(targets)
                : "Fed " + targets.size() + " players");
        return targets.size();
    }

    private static int teleportToEnd(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return teleportToEnd(context, Set.of(context.getSource().getPlayerOrException()));
    }

    private static int teleportToEnd(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int teleported = 0;
        for (ServerPlayer player : players) {
        if (player.level().dimension() == Level.END) {
                continue;
        }

        ServerLevel end = player.getServer().getLevel(Level.END);
        if (end == null) {
                continue;
        }

        Vec3 destination = ServerLevel.END_SPAWN_POINT.getBottomCenter().subtract(0.0, 1.0, 0.0);
        EndPlatformFeature.createEndPlatform(end, ServerLevel.END_SPAWN_POINT.below(), true);
        player.changeDimension(new DimensionTransition(
                end,
                destination,
                Vec3.ZERO,
                Direction.WEST.toYRot(),
                0.0F,
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
        ));
            teleported++;
        }

        sendSuccess(context.getSource(), "Teleported " + targetLabel(players) + " to the End");
        return teleported;
    }

    private static int teleportToNether(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return teleportToNether(context, Set.of(context.getSource().getPlayerOrException()));
    }

    private static int teleportToNether(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int teleported = 0;
        for (ServerPlayer player : players) {
        if (player.level().dimension() == Level.NETHER) {
                continue;
        }

        ServerLevel currentLevel = player.serverLevel();
        ServerLevel nether = player.getServer().getLevel(Level.NETHER);
        if (nether == null) {
                continue;
        }

        double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), nether.dimensionType());
        WorldBorder worldBorder = nether.getWorldBorder();
        BlockPos scaledPosition = worldBorder.clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale);

        Vec3 position = new Vec3(scaledPosition.getX() + 0.5, scaledPosition.getY(), scaledPosition.getZ() + 0.5);
        Vec3 collisionFreePosition = PortalShape.findCollisionFreePosition(position, nether, player, player.getDimensions(player.getPose()));
        player.changeDimension(new DimensionTransition(
                nether,
                collisionFreePosition,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND
        ));
            teleported++;
        }

        sendSuccess(context.getSource(), "Teleported " + targetLabel(players) + " to the Nether");
        return teleported;
    }

    private static int teleportToOverworld(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return teleportToOverworld(context, Set.of(context.getSource().getPlayerOrException()));
    }

    private static int teleportToOverworld(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int teleported = 0;
        for (ServerPlayer player : players) {
        if (player.level().dimension() == Level.OVERWORLD) {
                continue;
        }

        ServerLevel currentLevel = player.serverLevel();
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
                continue;
        }

        if (currentLevel.dimension() == Level.END) {
            DimensionTransition transition;
            if (player.getRespawnDimension() == Level.OVERWORLD && player.getRespawnPosition() != null) {
                transition = player.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
            } else {
                transition = new DimensionTransition(overworld, player, DimensionTransition.DO_NOTHING);
            }

            player.changeDimension(transition);
                teleported++;
                continue;
        }

        double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), overworld.dimensionType());
        WorldBorder worldBorder = overworld.getWorldBorder();
        BlockPos scaledPosition = worldBorder.clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale);
        int surfaceY = overworld.getHeight(Heightmap.Types.WORLD_SURFACE, scaledPosition.getX(), scaledPosition.getZ());
        Vec3 surfacePosition = new Vec3(scaledPosition.getX() + 0.5, surfaceY, scaledPosition.getZ() + 0.5);
        Vec3 collisionFreePosition = PortalShape.findCollisionFreePosition(
                surfacePosition,
                overworld,
                player,
                player.getDimensions(player.getPose())
        );

        player.changeDimension(new DimensionTransition(
                overworld,
                collisionFreePosition,
                Vec3.ZERO,
                player.getYRot(),
                player.getXRot(),
                DimensionTransition.DO_NOTHING
        ));
            teleported++;
        }

        sendSuccess(context.getSource(), "Teleported " + targetLabel(players) + " to the Overworld");
        return teleported;
    }

    private static int openInventory(CommandContext<CommandSourceStack> context, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, viewerInventory, player) -> new InvSeeMenu(containerId, viewerInventory, target),
                Component.literal(target.getGameProfile().getName() + " Inventory")
        );
        viewer.openMenu(provider, buffer -> buffer.writeInt(target.getId()));
        return 1;
    }

    private static int openInventory(CommandContext<CommandSourceStack> context, GameProfile targetProfile) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer onlineTarget = context.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());
        if (onlineTarget != null) {
            return openInventory(context, onlineTarget);
        }

        ServerPlayer viewer = context.getSource().getPlayerOrException();
        Path playerDataDirectory = context.getSource().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
        if (!OfflinePlayerData.exists(playerDataDirectory, targetProfile.getId())) {
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, viewerInventory, player) -> new InvSeeMenu(
                        containerId,
                        viewerInventory,
                        context.getSource().getServer(),
                        targetProfile.getId(),
                        -1
                ),
                Component.literal(targetProfile.getName() + " Inventory")
        );
        viewer.openMenu(provider, buffer -> buffer.writeInt(-1));
        return 1;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer viewer = context.getSource().getPlayerOrException();
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, viewerInventory, player) -> new RefreshingChestMenu(
                        containerId,
                        viewerInventory,
                        new LiveOrOfflineEnderChestContainer(target)
                ),
                Component.literal(target.getGameProfile().getName() + "'s Ender Chest")
        );
        viewer.openMenu(provider);
        return 1;
    }

    private static int openEnderChest(CommandContext<CommandSourceStack> context, GameProfile targetProfile) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer onlineTarget = context.getSource().getServer().getPlayerList().getPlayer(targetProfile.getId());
        if (onlineTarget != null) {
            return openEnderChest(context, onlineTarget);
        }

        ServerPlayer viewer = context.getSource().getPlayerOrException();
        Path playerDataDirectory = context.getSource().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
        if (!OfflinePlayerData.exists(playerDataDirectory, targetProfile.getId())) {
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, viewerInventory, player) -> new RefreshingChestMenu(
                        containerId,
                        viewerInventory,
                        new LiveOrOfflineEnderChestContainer(context.getSource().getServer(), targetProfile.getId())
                ),
                Component.literal(targetProfile.getName() + "'s Ender Chest")
        );
        viewer.openMenu(provider);
        return 1;
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> context, GameProfile profile) throws CommandSyntaxException {
        clearPlayerInventory(context, profile);
        clearPlayerEffects(context, profile);
        clearPlayerAdvancements(context, profile);
        clearPlayerRecipes(context, profile);
        clearPlayerStatistics(context, profile);
        setPlayerXp(context, profile, 0);
        clearPlayerEnderChest(context, profile);
        clearPlayerSpawnpoint(context, profile);
        sendSuccess(context.getSource(), "Reset " + profile.getName());
        return 1;
    }

    private static int clearPlayerInventory(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.getInventory().clearContent();
            player.containerMenu.broadcastChanges();
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), tag -> tag.put("Inventory", new ListTag()));
        }
        sendSuccess(context.getSource(), "Cleared inventory for " + profile.getName());
        return 1;
    }

    private static int clearPlayerEnderChest(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.getEnderChestInventory().clearContent();
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), tag -> tag.put("EnderItems", new ListTag()));
        }
        sendSuccess(context.getSource(), "Cleared ender chest for " + profile.getName());
        return 1;
    }

    private static int clearPlayerSpawnpoint(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.setRespawnPosition(Level.OVERWORLD, null, 0.0F, false, false);
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), AdminCommands::removeSpawnpointTags);
        }
        sendSuccess(context.getSource(), "Cleared spawnpoint for " + profile.getName());
        return 1;
    }

    private static int clearPlayerEffects(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.removeAllEffects();
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), tag -> tag.remove("active_effects"));
        }
        sendSuccess(context.getSource(), "Cleared effects for " + profile.getName());
        return 1;
    }

    private static int clearPlayerAdvancements(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            for (AdvancementHolder advancement : context.getSource().getServer().getAdvancements().getAllAdvancements()) {
                AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
                for (String criterion : progress.getCompletedCriteria()) {
                    player.getAdvancements().revoke(advancement, criterion);
                }
            }
        }
        deleteIfExists(context.getSource().getServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(profile.getId() + ".json"));
        sendSuccess(context.getSource(), "Cleared advancements for " + profile.getName());
        return 1;
    }

    private static int clearPlayerRecipes(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.resetRecipes(context.getSource().getServer().getRecipeManager().getRecipes());
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), tag -> tag.remove("recipeBook"));
        }
        sendSuccess(context.getSource(), "Cleared recipes for " + profile.getName());
        return 1;
    }

    private static int clearPlayerStatistics(CommandContext<CommandSourceStack> context, GameProfile profile) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            clearLiveStats(player);
        }
        deleteIfExists(context.getSource().getServer().getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(profile.getId() + ".json"));
        sendSuccess(context.getSource(), "Cleared statistics for " + profile.getName());
        return 1;
    }

    private static int setPlayerXp(CommandContext<CommandSourceStack> context, GameProfile profile, int levels) {
        ServerPlayer player = onlinePlayer(context, profile);
        if (player != null) {
            player.experienceProgress = 0.0F;
            player.experienceLevel = levels;
            player.totalExperience = 0;
        } else {
            updateStoredPlayerData(context.getSource().getServer(), profile.getId(), tag -> {
                tag.putFloat("XpP", 0.0F);
                tag.putInt("XpLevel", levels);
                tag.putInt("XpTotal", 0);
            });
        }
        sendSuccess(context.getSource(), "Set XP level for " + profile.getName() + " to " + levels);
        return 1;
    }

    public static int teleportToPlayer(CommandSourceStack commandSource, GameProfile profile) throws CommandSyntaxException {
        ServerPlayer source = commandSource.getPlayerOrException();
        ServerPlayer target = onlinePlayer(commandSource, profile);
        if (target != null) {
            teleportPlayer(source, target.serverLevel(), target.position(), target.getYRot(), target.getXRot());
            sendSuccess(commandSource, "Teleported to " + profile.getName());
            return 1;
        }

        CompoundTag playerData = storedPlayerData(commandSource.getServer(), profile.getId());
        ServerLevel targetLevel = storedPlayerLevel(commandSource.getServer(), playerData);
        Vec3 targetPosition = storedPlayerPosition(playerData);
        teleportPlayer(source, targetLevel, targetPosition, storedPlayerYaw(playerData), storedPlayerPitch(playerData));
        sendSuccess(commandSource, "Teleported to " + profile.getName() + "'s last logout position");
        return 1;
    }

    public static int teleportPlayerHere(CommandSourceStack commandSource, GameProfile profile) throws CommandSyntaxException {
        ServerPlayer source = commandSource.getPlayerOrException();
        ServerPlayer target = onlinePlayer(commandSource, profile);
        if (target != null) {
            teleportPlayer(target, source.serverLevel(), source.position(), source.getYRot(), source.getXRot());
            sendSuccess(commandSource, "Teleported " + profile.getName() + " here");
            return 1;
        }

        CompoundTag playerData = storedPlayerData(commandSource.getServer(), profile.getId());
        writeStoredPlayerLocation(playerData, source);
        saveStoredPlayerData(commandSource.getServer(), profile.getId(), playerData);
        sendSuccess(commandSource, "Moved " + profile.getName() + "'s next login position here");
        return 1;
    }

    private static void teleportPlayer(ServerPlayer player, ServerLevel targetLevel, Vec3 position, float yRot, float xRot) {
        player.changeDimension(new DimensionTransition(
                targetLevel,
                position,
                Vec3.ZERO,
                yRot,
                xRot,
                DimensionTransition.DO_NOTHING
        ));
    }

    private static ServerPlayer onlinePlayer(CommandContext<CommandSourceStack> context, GameProfile profile) {
        return context.getSource().getServer().getPlayerList().getPlayer(profile.getId());
    }

    private static ServerPlayer onlinePlayer(CommandSourceStack source, GameProfile profile) {
        return source.getServer().getPlayerList().getPlayer(profile.getId());
    }

    private static CompoundTag storedPlayerData(MinecraftServer server, UUID playerId) throws CommandSyntaxException {
        Path path = playerDataPath(server, playerId);
        if (!Files.isRegularFile(path)) {
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }

        try {
            return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        } catch (IOException exception) {
            LOGGER.warn("Failed to read stored player data for {}", playerId, exception);
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }
    }

    private static ServerLevel storedPlayerLevel(MinecraftServer server, CompoundTag playerData) throws CommandSyntaxException {
        ResourceLocation dimensionId = ResourceLocation.tryParse(playerData.getString("Dimension"));
        if (dimensionId == null) {
            throw ERROR_DIMENSION_NOT_FOUND.create();
        }

        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) {
            throw ERROR_DIMENSION_NOT_FOUND.create();
        }
        return level;
    }

    private static Vec3 storedPlayerPosition(CompoundTag playerData) throws CommandSyntaxException {
        ListTag position = playerData.getList("Pos", 6);
        if (position.size() < 3) {
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }
        return new Vec3(position.getDouble(0), position.getDouble(1), position.getDouble(2));
    }

    private static float storedPlayerYaw(CompoundTag playerData) {
        ListTag rotation = playerData.getList("Rotation", 5);
        return rotation.isEmpty() ? 0.0F : rotation.getFloat(0);
    }

    private static float storedPlayerPitch(CompoundTag playerData) {
        ListTag rotation = playerData.getList("Rotation", 5);
        return rotation.size() < 2 ? 0.0F : rotation.getFloat(1);
    }

    private static void writeStoredPlayerLocation(CompoundTag playerData, ServerPlayer source) {
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(source.getX()));
        position.add(DoubleTag.valueOf(source.getY()));
        position.add(DoubleTag.valueOf(source.getZ()));
        playerData.put("Pos", position);

        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(source.getYRot()));
        rotation.add(FloatTag.valueOf(source.getXRot()));
        playerData.put("Rotation", rotation);
        playerData.putString("Dimension", source.level().dimension().location().toString());
    }

    private static void saveStoredPlayerData(MinecraftServer server, UUID playerId, CompoundTag playerData) throws CommandSyntaxException {
        try {
            NbtIo.writeCompressed(playerData, playerDataPath(server, playerId));
        } catch (IOException exception) {
            LOGGER.warn("Failed to save stored player data for {}", playerId, exception);
            throw ERROR_NO_STORED_PLAYER_DATA.create();
        }
    }

    private static Path playerDataPath(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(playerId + ".dat");
    }

    private static void removeSpawnpointTags(CompoundTag tag) {
        tag.remove("SpawnX");
        tag.remove("SpawnY");
        tag.remove("SpawnZ");
        tag.remove("SpawnForced");
        tag.remove("SpawnAngle");
        tag.remove("SpawnDimension");
    }

    private static void updateStoredPlayerData(MinecraftServer server, UUID playerId, java.util.function.Consumer<CompoundTag> updater) {
        Path path = playerDataPath(server, playerId);
        if (!Files.isRegularFile(path)) {
            return;
        }

        try {
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            updater.accept(tag);
            NbtIo.writeCompressed(tag, path);
        } catch (IOException exception) {
            LOGGER.warn("Failed to update stored player data for {}", playerId, exception);
        }
    }

    private static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("Failed to delete {}", path, exception);
        }
    }

    private static void clearLiveStats(ServerPlayer player) {
        try {
            Field statsField = net.minecraft.stats.StatsCounter.class.getDeclaredField("stats");
            statsField.setAccessible(true);
            Object value = statsField.get(player.getStats());
            if (value instanceof it.unimi.dsi.fastutil.objects.Object2IntMap<?> stats) {
                stats.clear();
                player.getStats().markAllDirty();
                player.getStats().sendStats(player);
                player.getStats().save();
            }
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Failed to clear live statistics for {}", player.getUUID(), exception);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && GOD_PLAYERS.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && FLYING_PLAYERS.contains(player.getUUID())) {
            applyFlyAbility(player);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && FLYING_PLAYERS.contains(player.getUUID())) {
            applyFlyAbility(player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            try {
                OfflinePlayerData.save(player);
            } catch (IOException exception) {
                LOGGER.warn("Failed to save player data for {}", player.getUUID(), exception);
            }
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && FLYING_PLAYERS.contains(player.getUUID())) {
            applyFlyAbility(player);
        }
    }

    private static void onPlayerChangedGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getServer().execute(() -> applyFlyAbility(player));
        }
    }

    private static void applyFlyAbility(ServerPlayer player) {
        boolean adminFly = FLYING_PLAYERS.contains(player.getUUID());
        boolean naturallyCanFly = player.gameMode.getGameModeForPlayer() == GameType.CREATIVE || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;

        player.getAbilities().mayfly = adminFly || naturallyCanFly;
        if (!player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    private static String abilityFeedback(String ability, Boolean requestedState, Boolean finalState, Collection<ServerPlayer> targets) {
        if (requestedState == null && targets.size() > 1) {
            return "Toggled " + ability + " for " + targetLabel(targets);
        }

        boolean enabled = requestedState != null ? requestedState : Boolean.TRUE.equals(finalState);
        return (enabled ? "Enabled " : "Disabled ") + ability + " for " + targetLabel(targets);
    }

    private static String targetLabel(Collection<ServerPlayer> targets) {
        if (targets.size() == 1) {
            return targets.iterator().next().getGameProfile().getName();
        }

        return targets.size() + " players";
    }

    private static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
    }

    private static final class RefreshingChestMenu extends ChestMenu {
        private static final int CONTAINER_SLOTS = 27;
        private final NonNullList<ItemStack> lastSentContainerSlots = NonNullList.withSize(CONTAINER_SLOTS, ItemStack.EMPTY);

        private RefreshingChestMenu(int containerId, Inventory playerInventory, Container container) {
            super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
            this.rememberSentContainerSlots();
        }

        @Override
        public void broadcastChanges() {
            this.broadcastFullState();
        }

        @Override
        public void broadcastFullState() {
            this.rememberSentContainerSlots();
            super.broadcastFullState();
        }

        @Override
        public void sendAllDataToRemote() {
            this.rememberSentContainerSlots();
            super.sendAllDataToRemote();
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            if (this.containerChangedSinceLastSend()) {
                this.broadcastFullState();
                return;
            }

            super.clicked(slotId, button, clickType, player);
            this.broadcastFullState();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack stack = super.quickMoveStack(player, index);
            return stack;
        }

        private boolean containerChangedSinceLastSend() {
            for (int slot = 0; slot < CONTAINER_SLOTS; slot++) {
                if (!ItemStack.matches(this.lastSentContainerSlots.get(slot), this.slots.get(slot).getItem())) {
                    return true;
                }
            }
            return false;
        }

        private void rememberSentContainerSlots() {
            for (int slot = 0; slot < CONTAINER_SLOTS; slot++) {
                this.lastSentContainerSlots.set(slot, this.slots.get(slot).getItem().copy());
            }
        }
    }

    private static final class LiveOrOfflineEnderChestContainer implements Container {
        private final MinecraftServer server;
        private final UUID targetId;
        private final Container originalEnderChest;
        private final Path playerDataDirectory;
        private final HolderLookup.Provider registries;
        private Container offlineEnderChest;
        private boolean lastDelegateWasLive;

        private LiveOrOfflineEnderChestContainer(ServerPlayer target) {
            this(target.getServer(), target.getUUID(), target.getEnderChestInventory());
        }

        private LiveOrOfflineEnderChestContainer(MinecraftServer server, UUID targetId) {
            this(server, targetId, null);
        }

        private LiveOrOfflineEnderChestContainer(MinecraftServer server, UUID targetId, Container originalEnderChest) {
            this.server = server;
            this.targetId = targetId;
            this.originalEnderChest = originalEnderChest;
            this.playerDataDirectory = this.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR);
            this.registries = this.server.registryAccess();
        }

        @Override
        public int getContainerSize() {
            return this.delegate().getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return this.delegate().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.delegate().getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return this.delegate().removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return this.delegate().removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.delegate().setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            this.delegate().setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void startOpen(Player player) {
            this.delegate().startOpen(player);
        }

        @Override
        public void stopOpen(Player player) {
            this.delegate().stopOpen(player);
        }

        @Override
        public void clearContent() {
            this.delegate().clearContent();
        }

        private Container delegate() {
            ServerPlayer liveTarget = this.server.getPlayerList().getPlayer(this.targetId);
            if (liveTarget != null && !liveTarget.hasDisconnected()) {
                this.lastDelegateWasLive = true;
                this.offlineEnderChest = null;
                return liveTarget.getEnderChestInventory();
            }

            if (this.lastDelegateWasLive) {
                this.lastDelegateWasLive = false;
                this.offlineEnderChest = null;
            }

            if (this.offlineEnderChest == null) {
                try {
                    this.offlineEnderChest = OfflinePlayerData.enderChest(this.playerDataDirectory, this.targetId, this.registries);
                } catch (IOException exception) {
                    this.offlineEnderChest = this.originalEnderChest != null
                            ? this.originalEnderChest
                            : new net.minecraft.world.SimpleContainer(27);
                }
            }
            return this.offlineEnderChest;
        }
    }

}
