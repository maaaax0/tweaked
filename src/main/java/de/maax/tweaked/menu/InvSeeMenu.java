package de.maax.tweaked.menu;

import com.mojang.datafixers.util.Pair;
import de.maax.tweaked.server.OfflinePlayerData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public final class InvSeeMenu extends AbstractContainerMenu {
    private static final int TARGET_SLOT_COUNT = 41;
    private static final int VIEWER_SLOT_COUNT = 41;
    private static final int MAIN_SLOT_START = 5;
    private static final int HOTBAR_SLOT_START = 32;
    private static final int VIEWER_SLOT_START = TARGET_SLOT_COUNT;
    private static final int PANEL_SPACING = 184;

    private final Container targetInventory;
    private final int targetEntityId;
    private final NonNullList<ItemStack> lastSentTargetSlots = NonNullList.withSize(TARGET_SLOT_COUNT, ItemStack.EMPTY);

    public InvSeeMenu(int containerId, Inventory viewerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, viewerInventory, new SimpleContainer(TARGET_SLOT_COUNT), data.readInt(), null);
    }

    public InvSeeMenu(int containerId, Inventory viewerInventory, ServerPlayer target) {
        this(containerId, viewerInventory, new PlayerInventoryContainer(target), target.getId(), target);
    }

    public InvSeeMenu(int containerId, Inventory viewerInventory, MinecraftServer server, UUID targetId, int targetEntityId) {
        this(containerId, viewerInventory, new PlayerInventoryContainer(server, targetId), targetEntityId, null);
    }

    public InvSeeMenu(int containerId, Inventory viewerInventory, Container targetInventory, int targetEntityId) {
        this(containerId, viewerInventory, targetInventory, targetEntityId, null);
    }

    private InvSeeMenu(int containerId, Inventory viewerInventory, Container targetInventory, int targetEntityId, LivingEntity targetEntity) {
        super(TweakedMenus.INVSEE.get(), containerId);
        this.targetInventory = targetInventory;
        this.targetEntityId = targetEntityId;
        this.targetInventory.startOpen(viewerInventory.player);

        addArmorSlot(this.targetInventory, 0, 39, 8, 8, EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, viewerInventory.player, targetEntity);
        addArmorSlot(this.targetInventory, 1, 38, 8, 26, EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, viewerInventory.player, targetEntity);
        addArmorSlot(this.targetInventory, 2, 37, 8, 44, EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, viewerInventory.player, targetEntity);
        addArmorSlot(this.targetInventory, 3, 36, 8, 62, EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, viewerInventory.player, targetEntity);
        this.addSlot(new IconSlot(this.targetInventory, 4, 40, 77, 62, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(this.targetInventory, MAIN_SLOT_START + column + row * 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(this.targetInventory, HOTBAR_SLOT_START + column, 8 + column * 18, 142));
        }

        addArmorSlot(viewerInventory, 39, 39, PANEL_SPACING + 8, 8, EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET, viewerInventory.player, viewerInventory.player);
        addArmorSlot(viewerInventory, 38, 38, PANEL_SPACING + 8, 26, EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE, viewerInventory.player, viewerInventory.player);
        addArmorSlot(viewerInventory, 37, 37, PANEL_SPACING + 8, 44, EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS, viewerInventory.player, viewerInventory.player);
        addArmorSlot(viewerInventory, 36, 36, PANEL_SPACING + 8, 62, EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS, viewerInventory.player, viewerInventory.player);
        this.addSlot(new IconSlot(viewerInventory, 40, 40, PANEL_SPACING + 77, 62, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(viewerInventory, column + (row + 1) * 9, PANEL_SPACING + 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(viewerInventory, column, PANEL_SPACING + 8 + column * 18, 142));
        }

        this.rememberSentTargetSlots();
    }

    private void addArmorSlot(
            Container container,
            int menuSlot,
            int targetSlot,
            int x,
            int y,
            EquipmentSlot equipmentSlot,
            ResourceLocation emptyIcon,
            Player viewer,
            LivingEntity targetEntity
    ) {
        this.addSlot(new EquipmentIconSlot(container, menuSlot, targetSlot, x, y, equipmentSlot, emptyIcon, targetEntity != null ? targetEntity : viewer));
    }

    public int targetEntityId() {
        return this.targetEntityId;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.targetInventory.stillValid(player);
    }

    @Override
    public void broadcastChanges() {
        this.broadcastFullState();
    }

    @Override
    public void broadcastFullState() {
        this.rememberSentTargetSlots();
        super.broadcastFullState();
    }

    @Override
    public void sendAllDataToRemote() {
        this.rememberSentTargetSlots();
        super.sendAllDataToRemote();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (this.targetChangedSinceLastSend()) {
            this.broadcastFullState();
            return;
        }

        super.clicked(slotId, button, clickType, player);
        this.broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            originalStack = stack.copy();

            if (index < TARGET_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, TARGET_SLOT_COUNT, TARGET_SLOT_COUNT + VIEWER_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, TARGET_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.targetInventory.stopOpen(player);
    }

    private static final class IconSlot extends Slot {
        private final ResourceLocation emptyIcon;

        private IconSlot(Container container, int menuSlot, int targetSlot, int x, int y, ResourceLocation emptyIcon) {
            super(container, menuSlot, x, y);
            this.emptyIcon = emptyIcon;
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon);
        }
    }

    private static final class EquipmentIconSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final ResourceLocation emptyIcon;
        private final LivingEntity owner;

        private EquipmentIconSlot(
                Container container,
                int menuSlot,
                int targetSlot,
                int x,
                int y,
                EquipmentSlot equipmentSlot,
                ResourceLocation emptyIcon,
                LivingEntity owner
        ) {
            super(container, menuSlot, x, y);
            this.equipmentSlot = equipmentSlot;
            this.emptyIcon = emptyIcon;
            this.owner = owner;
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.owner.onEquipItem(this.equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(this.equipmentSlot, this.owner);
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack stack = this.getItem();
            return stack.isEmpty() || player.isCreative() || !EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, this.emptyIcon);
        }
    }

    private static final class PlayerInventoryContainer implements Container {
        private static final int[] SLOT_MAPPING = {
                39, 38, 37, 36, 40,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35,
                0, 1, 2, 3, 4, 5, 6, 7, 8
        };

        private final MinecraftServer server;
        private final UUID targetId;
        private final ServerPlayer originalTarget;
        private final Path playerDataDirectory;
        private final HolderLookup.Provider registries;
        private Container offlineInventory;
        private boolean lastDelegateWasLive;

        private PlayerInventoryContainer(ServerPlayer target) {
            this(target.getServer(), target.getUUID(), target);
        }

        private PlayerInventoryContainer(MinecraftServer server, UUID targetId) {
            this(server, targetId, null);
        }

        private PlayerInventoryContainer(MinecraftServer server, UUID targetId, ServerPlayer originalTarget) {
            this.server = server;
            this.targetId = targetId;
            this.originalTarget = originalTarget;
            this.playerDataDirectory = this.server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            this.registries = this.server.registryAccess();
        }

        @Override
        public int getContainerSize() {
            return TARGET_SLOT_COUNT;
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < TARGET_SLOT_COUNT; slot++) {
                if (!getItem(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
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
        public void clearContent() {
            this.delegate().clearContent();
        }

        @Override
        public void stopOpen(Player player) {
            this.delegate().stopOpen(player);
        }

        private Container delegate() {
            ServerPlayer liveTarget = this.server.getPlayerList().getPlayer(this.targetId);
            if (liveTarget != null && !liveTarget.hasDisconnected()) {
                this.lastDelegateWasLive = true;
                this.offlineInventory = null;
                return new LivePlayerInventory(liveTarget);
            }

            if (this.lastDelegateWasLive) {
                this.lastDelegateWasLive = false;
                this.offlineInventory = null;
            }

            if (this.offlineInventory == null) {
                try {
                    this.offlineInventory = OfflinePlayerData.inventory(this.playerDataDirectory, this.targetId, this.registries);
                } catch (IOException exception) {
                    this.offlineInventory = this.originalTarget != null
                            ? new LivePlayerInventory(this.originalTarget)
                            : new SimpleContainer(TARGET_SLOT_COUNT);
                }
            }
            return this.offlineInventory;
        }

        private static int targetSlot(int menuSlot) {
            return SLOT_MAPPING[menuSlot];
        }

        private static final class LivePlayerInventory implements Container {
            private final ServerPlayer target;

            private LivePlayerInventory(ServerPlayer target) {
                this.target = target;
            }

            @Override
            public int getContainerSize() {
                return TARGET_SLOT_COUNT;
            }

            @Override
            public boolean isEmpty() {
                for (int slot = 0; slot < TARGET_SLOT_COUNT; slot++) {
                    if (!this.getItem(slot).isEmpty()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public ItemStack getItem(int slot) {
                return this.target.getInventory().getItem(targetSlot(slot));
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return this.target.getInventory().removeItem(targetSlot(slot), amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return this.target.getInventory().removeItemNoUpdate(targetSlot(slot));
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                this.target.getInventory().setItem(targetSlot(slot), stack);
            }

            @Override
            public void setChanged() {
                this.target.getInventory().setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }

            @Override
            public void clearContent() {
                this.target.getInventory().clearContent();
            }
        }
    }

    private boolean targetChangedSinceLastSend() {
        for (int slot = 0; slot < TARGET_SLOT_COUNT; slot++) {
            if (!ItemStack.matches(this.lastSentTargetSlots.get(slot), this.slots.get(slot).getItem())) {
                return true;
            }
        }
        return false;
    }

    private void rememberSentTargetSlots() {
        for (int slot = 0; slot < TARGET_SLOT_COUNT; slot++) {
            this.lastSentTargetSlots.set(slot, this.slots.get(slot).getItem().copy());
        }
    }
}
