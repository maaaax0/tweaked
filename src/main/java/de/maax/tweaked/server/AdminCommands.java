package de.maax.tweaked.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import de.maax.tweaked.menu.InvSeeMenu;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminCommands {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType ERROR_SINGLE_PLAYER = new SimpleCommandExceptionType(Component.literal("Expected a single player"));
    private static final SimpleCommandExceptionType ERROR_NO_STORED_PLAYER_DATA = new SimpleCommandExceptionType(Component.literal("No stored player data found"));
    private static final Set<UUID> FLYING_PLAYERS = new HashSet<>();
    private static final Set<UUID> GOD_PLAYERS = new HashSet<>();

    private AdminCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(AdminCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(AdminCommands::onPlayerChangedGameMode);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("fly")
                .requires(source -> source.hasPermission(2))
                .executes(context -> setFly(context, self(context), null))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> setFly(context, EntityArgument.getPlayers(context, "targets"), null))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setFly(context, EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled"))))));

        dispatcher.register(Commands.literal("god")
                .requires(source -> source.hasPermission(2))
                .executes(context -> setGod(context, self(context), null))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> setGod(context, EntityArgument.getPlayers(context, "targets"), null))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setGod(context, EntityArgument.getPlayers(context, "targets"), BoolArgumentType.getBool(context, "enabled"))))));

        dispatcher.register(Commands.literal("heal")
                .requires(source -> source.hasPermission(2))
                .executes(context -> heal(context, self(context)))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> heal(context, EntityArgument.getPlayers(context, "targets")))));

        dispatcher.register(Commands.literal("feed")
                .requires(source -> source.hasPermission(2))
                .executes(context -> feed(context, self(context)))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> feed(context, EntityArgument.getPlayers(context, "targets")))));

        dispatcher.register(Commands.literal("invsee")
                .requires(source -> source.hasPermission(2))
                .executes(context -> openInventory(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openInventory(context, singleProfile(context, "target")))));

        dispatcher.register(Commands.literal("endersee")
                .requires(source -> source.hasPermission(2))
                .executes(context -> openEnderChest(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(context -> openEnderChest(context, singleProfile(context, "target")))));
    }

    private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return Set.of(context.getSource().getPlayerOrException());
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
                (containerId, viewerInventory, player) -> {
                    try {
                        return new InvSeeMenu(
                                containerId,
                                viewerInventory,
                                OfflinePlayerData.inventory(playerDataDirectory, targetProfile.getId(), context.getSource().getServer().registryAccess()),
                                -1
                        );
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to load offline inventory for " + targetProfile.getName(), exception);
                    }
                },
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
                (containerId, viewerInventory, player) -> {
                    try {
                        return new RefreshingChestMenu(
                                containerId,
                                viewerInventory,
                                OfflinePlayerData.enderChest(playerDataDirectory, targetProfile.getId(), context.getSource().getServer().registryAccess())
                        );
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to load offline ender chest for " + targetProfile.getName(), exception);
                    }
                },
                Component.literal(targetProfile.getName() + "'s Ender Chest")
        );
        viewer.openMenu(provider);
        return 1;
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

        private LiveOrOfflineEnderChestContainer(ServerPlayer target) {
            this.server = target.getServer();
            this.targetId = target.getUUID();
            this.originalEnderChest = target.getEnderChestInventory();
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
                return liveTarget.getEnderChestInventory();
            }

            if (this.offlineEnderChest == null) {
                try {
                    this.offlineEnderChest = OfflinePlayerData.enderChest(this.playerDataDirectory, this.targetId, this.registries);
                } catch (IOException exception) {
                    this.offlineEnderChest = this.originalEnderChest;
                }
            }
            return this.offlineEnderChest;
        }
    }

}
