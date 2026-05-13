package de.maax.tweaked.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class OfflinePlayerData {
    private OfflinePlayerData() {
    }

    static Path playerDataPath(Path playerDataDirectory, UUID playerId) {
        return playerDataDirectory.resolve(playerId + ".dat");
    }

    public static boolean exists(Path playerDataDirectory, UUID playerId) {
        return Files.isRegularFile(playerDataPath(playerDataDirectory, playerId));
    }

    public static Container inventory(Path playerDataDirectory, UUID playerId, HolderLookup.Provider registries) throws IOException {
        CompoundTag playerData = read(playerDataDirectory, playerId);
        return new InventoryContainer(playerDataDirectory, playerId, registries, playerData);
    }

    public static Container enderChest(Path playerDataDirectory, UUID playerId, HolderLookup.Provider registries) throws IOException {
        CompoundTag playerData = read(playerDataDirectory, playerId);
        return new EnderChestContainer(playerDataDirectory, playerId, registries, playerData);
    }

    private static CompoundTag read(Path playerDataDirectory, UUID playerId) throws IOException {
        return NbtIo.readCompressed(playerDataPath(playerDataDirectory, playerId), NbtAccounter.unlimitedHeap());
    }

    private static void write(Path playerDataDirectory, UUID playerId, CompoundTag playerData) throws IOException {
        NbtIo.writeCompressed(playerData, playerDataPath(playerDataDirectory, playerId));
    }

    private static final class InventoryContainer implements Container {
        private static final int SLOT_COUNT = 41;
        private static final int[] MENU_TO_PLAYER_SLOT = {
                39, 38, 37, 36, 40,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35,
                0, 1, 2, 3, 4, 5, 6, 7, 8
        };

        private final Path playerDataDirectory;
        private final UUID playerId;
        private final HolderLookup.Provider registries;
        private final CompoundTag playerData;
        private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        private boolean dirty;

        private InventoryContainer(Path playerDataDirectory, UUID playerId, HolderLookup.Provider registries, CompoundTag playerData) {
            this.playerDataDirectory = playerDataDirectory;
            this.playerId = playerId;
            this.registries = registries;
            this.playerData = playerData;
            this.load(playerData.getList("Inventory", 10));
        }

        private void load(ListTag inventory) {
            for (int i = 0; i < inventory.size(); i++) {
                CompoundTag itemTag = inventory.getCompound(i);
                int playerSlot = nbtSlotToPlayerSlot(itemTag.getByte("Slot") & 255);
                if (playerSlot >= 0 && playerSlot < this.items.size()) {
                    this.items.set(playerSlot, ItemStack.parse(this.registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }

        @Override
        public int getContainerSize() {
            return MENU_TO_PLAYER_SLOT.length;
        }

        @Override
        public boolean isEmpty() {
            for (int menuSlot = 0; menuSlot < MENU_TO_PLAYER_SLOT.length; menuSlot++) {
                if (!this.getItem(menuSlot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.items.get(MENU_TO_PLAYER_SLOT[slot]);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = this.items.get(MENU_TO_PLAYER_SLOT[slot]);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack removed = stack.split(amount);
            if (!removed.isEmpty()) {
                this.setChanged();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            int playerSlot = MENU_TO_PLAYER_SLOT[slot];
            ItemStack stack = this.items.get(playerSlot);
            this.items.set(playerSlot, ItemStack.EMPTY);
            this.setChanged();
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.items.set(MENU_TO_PLAYER_SLOT[slot], stack);
            this.setChanged();
        }

        @Override
        public void setChanged() {
            this.dirty = true;
            this.saveIfDirty();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void stopOpen(Player player) {
            this.saveIfDirty();
        }

        @Override
        public void clearContent() {
            this.items.replaceAll(stack -> ItemStack.EMPTY);
            this.setChanged();
        }

        private void saveIfDirty() {
            if (!this.dirty) {
                return;
            }

            ListTag inventory = new ListTag();
            for (int playerSlot = 0; playerSlot < this.items.size(); playerSlot++) {
                ItemStack stack = this.items.get(playerSlot);
                int nbtSlot = playerSlotToNbtSlot(playerSlot);
                if (!stack.isEmpty() && nbtSlot != -1) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putByte("Slot", (byte)nbtSlot);
                    inventory.add(stack.save(this.registries, itemTag));
                }
            }

            this.playerData.put("Inventory", inventory);
            try {
                write(this.playerDataDirectory, this.playerId, this.playerData);
                this.dirty = false;
            } catch (IOException exception) {
                AdminCommands.LOGGER.warn("Failed to save offline inventory for {}", this.playerId, exception);
            }
        }

        private static int nbtSlotToPlayerSlot(int nbtSlot) {
            if (nbtSlot >= 0 && nbtSlot < 36) {
                return nbtSlot;
            }
            if (nbtSlot >= 100 && nbtSlot < 104) {
                return 36 + nbtSlot - 100;
            }
            if (nbtSlot == 150) {
                return 40;
            }
            return -1;
        }

        private static int playerSlotToNbtSlot(int playerSlot) {
            if (playerSlot >= 0 && playerSlot < 36) {
                return playerSlot;
            }
            if (playerSlot >= 36 && playerSlot < 40) {
                return 100 + playerSlot - 36;
            }
            if (playerSlot == 40) {
                return 150;
            }
            return -1;
        }
    }

    private static final class EnderChestContainer extends SimpleContainer {
        private final Path playerDataDirectory;
        private final UUID playerId;
        private final HolderLookup.Provider registries;
        private final CompoundTag playerData;
        private boolean dirty;
        private boolean loading;

        private EnderChestContainer(Path playerDataDirectory, UUID playerId, HolderLookup.Provider registries, CompoundTag playerData) {
            super(27);
            this.playerDataDirectory = playerDataDirectory;
            this.playerId = playerId;
            this.registries = registries;
            this.playerData = playerData;
            this.load(playerData.getList("EnderItems", 10));
        }

        private void load(ListTag enderItems) {
            this.loading = true;
            for (int i = 0; i < enderItems.size(); i++) {
                CompoundTag itemTag = enderItems.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < this.getContainerSize()) {
                    this.setItem(slot, ItemStack.parse(this.registries, itemTag).orElse(ItemStack.EMPTY));
                }
            }
            this.loading = false;
            this.dirty = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.dirty = true;
            if (!this.loading) {
                this.saveIfDirty();
            }
        }

        @Override
        public void stopOpen(Player player) {
            super.stopOpen(player);
            this.saveIfDirty();
        }

        private void saveIfDirty() {
            if (!this.dirty) {
                return;
            }

            ListTag enderItems = new ListTag();
            for (int slot = 0; slot < this.getContainerSize(); slot++) {
                ItemStack stack = this.getItem(slot);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putByte("Slot", (byte)slot);
                    enderItems.add(stack.save(this.registries, itemTag));
                }
            }

            this.playerData.put("EnderItems", enderItems);
            try {
                write(this.playerDataDirectory, this.playerId, this.playerData);
                this.dirty = false;
            } catch (IOException exception) {
                AdminCommands.LOGGER.warn("Failed to save offline ender chest for {}", this.playerId, exception);
            }
        }
    }
}
