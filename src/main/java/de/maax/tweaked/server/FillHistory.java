package de.maax.tweaked.server;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class FillHistory {
    private static final int MAX_HISTORY = 32;
    private static final int BLOCK_TRANSACTION_TICKS = 20;
    private static final int BLOCK_NEIGHBOR_PADDING = 1;
    private static final Map<UUID, History> HISTORIES = new HashMap<>();
    private static final List<BlockTransaction> OPEN_BLOCK_TRANSACTIONS = new ArrayList<>();
    private static final ThreadLocal<Boolean> SUPPRESS_TRACKING = ThreadLocal.withInitial(() -> false);

    private FillHistory() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FillHistory::onServerTick);
    }

    public static PendingCapture beginBlocks(ServerPlayer player, ServerLevel level, BoundingBox area) {
        BlockTransaction transaction = new BlockTransaction(
                player.getUUID(),
                level.dimension(),
                area,
                Snapshot.captureEntities(level, area),
                BLOCK_TRANSACTION_TICKS
        );
        transaction.captureInitialNeighborhood(level);
        OPEN_BLOCK_TRANSACTIONS.add(transaction);
        return new PendingCapture(player.getUUID(), level.dimension(), area, transaction, null);
    }

    public static PendingCapture beginBiomes(ServerPlayer player, ServerLevel level, BoundingBox area) {
        return new PendingCapture(player.getUUID(), level.dimension(), area, null, Snapshot.captureBiomes(level, area));
    }

    public static void finishBlocks(PendingCapture capture, ServerLevel level) {
        if (capture == null) {
            return;
        }
        // Block transactions stay open briefly so neighbor updates and delayed block reactions are captured too.
    }

    public static void finishBiomes(PendingCapture capture, ServerLevel level) {
        if (capture == null) {
            return;
        }
        push(capture.playerId(), new Entry(capture.dimension(), capture.area(), null, null, capture.beforeBiomes(), Snapshot.captureBiomes(level, capture.area())));
    }

    public static void beforeBlockChange(ServerLevel level, BlockPos pos) {
        if (SUPPRESS_TRACKING.get()) {
            return;
        }
        for (BlockTransaction transaction : OPEN_BLOCK_TRANSACTIONS) {
            if (transaction.dimension().equals(level.dimension())) {
                transaction.captureBefore(level, pos);
            }
        }
    }

    public static void afterBlockChange(ServerLevel level, BlockPos pos) {
        if (SUPPRESS_TRACKING.get()) {
            return;
        }
        for (BlockTransaction transaction : OPEN_BLOCK_TRANSACTIONS) {
            if (transaction.dimension().equals(level.dimension())) {
                transaction.captureAfter(level, pos);
            }
        }
    }

    public static int undo(CommandSourceStack source) {
        return apply(source, true);
    }

    public static int redo(CommandSourceStack source) {
        return apply(source, false);
    }

    private static int apply(CommandSourceStack source, boolean undo) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }

        History history = HISTORIES.get(player.getUUID());
        if (history == null) {
            source.sendFailure(Component.literal(undo ? "Nothing to undo" : "Nothing to redo"));
            return 0;
        }

        Entry entry = undo ? history.undo.pollLast() : history.redo.pollLast();
        if (entry == null) {
            source.sendFailure(Component.literal(undo ? "Nothing to undo" : "Nothing to redo"));
            return 0;
        }

        ServerLevel level = source.getServer().getLevel(entry.dimension());
        if (level == null) {
            source.sendFailure(Component.literal("Target dimension is not loaded"));
            return 0;
        }

        if (entry.beforeBlocks() != null) {
            SUPPRESS_TRACKING.set(true);
            try {
                BlockSnapshot targetSnapshot = undo ? entry.beforeBlocks() : entry.afterBlocks();
                BlockSnapshot previousSnapshot = undo ? entry.afterBlocks() : entry.beforeBlocks();
                targetSnapshot.restore(level, source.getServer(), entitiesOnlyIn(previousSnapshot, targetSnapshot));
            } finally {
                SUPPRESS_TRACKING.set(false);
            }
        } else {
            (undo ? entry.beforeBiomes() : entry.afterBiomes()).restore(level);
        }

        (undo ? history.redo : history.undo).addLast(entry);
        source.sendSuccess(() -> Component.literal(undo ? "Undid last fill" : "Redid last fill"), true);
        return 1;
    }

    private static void push(UUID playerId, Entry entry) {
        History history = HISTORIES.computeIfAbsent(playerId, ignored -> new History());
        history.undo.addLast(entry);
        while (history.undo.size() > MAX_HISTORY) {
            history.undo.removeFirst();
        }
        history.redo.clear();
    }

    private static Set<UUID> entitiesOnlyIn(BlockSnapshot source, BlockSnapshot target) {
        Map<UUID, EntityRecord> targetEntities = target.entities();
        return source.entities().keySet().stream()
                .filter(uuid -> !targetEntities.containsKey(uuid))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public record PendingCapture(UUID playerId, ResourceKey<net.minecraft.world.level.Level> dimension, BoundingBox area, BlockTransaction blockTransaction, BiomeSnapshot beforeBiomes) {
    }

    private record Entry(
            ResourceKey<net.minecraft.world.level.Level> dimension,
            BoundingBox area,
            BlockSnapshot beforeBlocks,
            BlockSnapshot afterBlocks,
            BiomeSnapshot beforeBiomes,
            BiomeSnapshot afterBiomes
    ) {
    }

    private static final class History {
        private final Deque<Entry> undo = new ArrayDeque<>();
        private final Deque<Entry> redo = new ArrayDeque<>();
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (int index = OPEN_BLOCK_TRANSACTIONS.size() - 1; index >= 0; index--) {
            BlockTransaction transaction = OPEN_BLOCK_TRANSACTIONS.get(index);
            if (!transaction.tick()) {
                continue;
            }
            OPEN_BLOCK_TRANSACTIONS.remove(index);
            if (!transaction.beforeBlocks().isEmpty() || !transaction.afterBlocks().isEmpty()) {
                ServerLevel level = event.getServer().getLevel(transaction.dimension());
                if (level != null) {
                    push(transaction.playerId(), transaction.toEntry(level));
                }
            }
        }
    }

    private static final class Snapshot {
        private Snapshot() {
        }

        private static BiomeSnapshot captureBiomes(ServerLevel level, BoundingBox area) {
            Map<QuartPosKey, Holder<Biome>> biomes = new LinkedHashMap<>();
            for (int x = QuartPos.fromBlock(area.minX()); x <= QuartPos.fromBlock(area.maxX()); x++) {
                for (int y = QuartPos.fromBlock(area.minY()); y <= QuartPos.fromBlock(area.maxY()); y++) {
                    for (int z = QuartPos.fromBlock(area.minZ()); z <= QuartPos.fromBlock(area.maxZ()); z++) {
                        biomes.put(new QuartPosKey(x, y, z), level.getNoiseBiome(x, y, z));
                    }
                }
            }
            return new BiomeSnapshot(area, biomes);
        }

        private static Map<UUID, EntityRecord> captureEntities(ServerLevel level, BoundingBox area) {
            AABB bounds = new AABB(area.minX(), area.minY(), area.minZ(), area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1);
            Map<UUID, EntityRecord> entities = new LinkedHashMap<>();
            for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, candidate -> !(candidate instanceof Player))) {
                CompoundTag tag = new CompoundTag();
                if (entity.save(tag)) {
                    entities.put(entity.getUUID(), new EntityRecord(tag));
                }
            }
            return entities;
        }
    }

    private record BlockRecord(BlockState state, CompoundTag blockEntityTag) {
    }

    private record EntityRecord(CompoundTag tag) {
    }

    private record QuartPosKey(int x, int y, int z) {
    }

    public static final class BlockTransaction {
        private final UUID playerId;
        private final ResourceKey<net.minecraft.world.level.Level> dimension;
        private final BoundingBox area;
        private final Map<BlockPos, BlockRecord> beforeBlocks = new LinkedHashMap<>();
        private final Map<BlockPos, BlockRecord> afterBlocks = new LinkedHashMap<>();
        private final Map<UUID, EntityRecord> beforeEntities;
        private int ticksRemaining;

        private BlockTransaction(
                UUID playerId,
                ResourceKey<net.minecraft.world.level.Level> dimension,
                BoundingBox area,
                Map<UUID, EntityRecord> beforeEntities,
                int ticksRemaining
        ) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.area = area;
            this.beforeEntities = beforeEntities;
            this.ticksRemaining = ticksRemaining;
        }

        private UUID playerId() {
            return this.playerId;
        }

        private ResourceKey<net.minecraft.world.level.Level> dimension() {
            return this.dimension;
        }

        private Map<BlockPos, BlockRecord> beforeBlocks() {
            return this.beforeBlocks;
        }

        private Map<BlockPos, BlockRecord> afterBlocks() {
            return this.afterBlocks;
        }

        private void captureBefore(ServerLevel level, BlockPos pos) {
            BlockPos immutable = pos.immutable();
            this.beforeBlocks.computeIfAbsent(immutable, ignored -> captureBlock(level, immutable));
        }

        private void captureInitialNeighborhood(ServerLevel level) {
            for (BlockPos pos : BlockPos.betweenClosed(
                    this.area.minX() - BLOCK_NEIGHBOR_PADDING,
                    this.area.minY() - BLOCK_NEIGHBOR_PADDING,
                    this.area.minZ() - BLOCK_NEIGHBOR_PADDING,
                    this.area.maxX() + BLOCK_NEIGHBOR_PADDING,
                    this.area.maxY() + BLOCK_NEIGHBOR_PADDING,
                    this.area.maxZ() + BLOCK_NEIGHBOR_PADDING
            )) {
                BlockPos immutable = pos.immutable();
                this.beforeBlocks.putIfAbsent(immutable, captureBlock(level, immutable));
            }
        }

        private void captureAfter(ServerLevel level, BlockPos pos) {
            BlockPos immutable = pos.immutable();
            this.afterBlocks.put(immutable, captureBlock(level, immutable));
        }

        private boolean tick() {
            this.ticksRemaining--;
            return this.ticksRemaining <= 0;
        }

        private Entry toEntry(ServerLevel level) {
            for (BlockPos pos : this.beforeBlocks.keySet()) {
                this.afterBlocks.putIfAbsent(pos, captureBlock(level, pos));
            }
            return new Entry(
                    this.dimension,
                    this.area,
                    new BlockSnapshot(this.area, Map.copyOf(this.beforeBlocks), Map.copyOf(this.beforeEntities)),
                    new BlockSnapshot(this.area, Map.copyOf(this.afterBlocks), Snapshot.captureEntities(level, this.area)),
                    null,
                    null
            );
        }
    }

    public record BlockSnapshot(BoundingBox area, Map<BlockPos, BlockRecord> blocks, Map<UUID, EntityRecord> entities) {
        private void restore(ServerLevel level, MinecraftServer server, Set<UUID> entitiesToRemove) {
            for (Map.Entry<BlockPos, BlockRecord> entry : this.blocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockRecord record = entry.getValue();
                level.setBlock(pos, record.state(), Block.UPDATE_ALL);
                if (record.blockEntityTag() != null) {
                    BlockEntity restored = BlockEntity.loadStatic(pos, record.state(), record.blockEntityTag(), level.registryAccess());
                    if (restored != null) {
                        level.setBlockEntity(restored);
                    }
                }
            }

            for (BlockPos pos : this.blocks.keySet()) {
                level.blockUpdated(pos, level.getBlockState(pos).getBlock());
            }

            for (UUID uuid : entitiesToRemove) {
                Entity entity = findEntity(server, uuid);
                if (entity != null) {
                    entity.discard();
                }
            }

            for (Map.Entry<UUID, EntityRecord> entry : this.entities.entrySet()) {
                restoreEntity(server, level, entry.getKey(), entry.getValue());
            }
        }

        private static void restoreEntity(MinecraftServer server, ServerLevel targetLevel, UUID uuid, EntityRecord record) {
            Entity current = findEntity(server, uuid);
            CompoundTag tag = record.tag().copy();
            if (current != null) {
                if (current.level() != targetLevel) {
                    current.discard();
                    current = null;
                } else {
                    current.load(tag);
                    return;
                }
            }

            Optional<Entity> restored = EntityType.create(tag, targetLevel);
            restored.ifPresent(targetLevel::addFreshEntityWithPassengers);
        }

        private static Entity findEntity(MinecraftServer server, UUID uuid) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(uuid);
                if (entity != null) {
                    return entity;
                }
            }
            return null;
        }
    }

    private static BlockRecord captureBlock(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return new BlockRecord(
                level.getBlockState(pos),
                blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess())
        );
    }

    public record BiomeSnapshot(BoundingBox area, Map<QuartPosKey, Holder<Biome>> biomes) {
        private void restore(ServerLevel level) {
            List<ChunkAccess> chunks = new ArrayList<>();
            for (int z = SectionPos.blockToSectionCoord(this.area.minZ()); z <= SectionPos.blockToSectionCoord(this.area.maxZ()); z++) {
                for (int x = SectionPos.blockToSectionCoord(this.area.minX()); x <= SectionPos.blockToSectionCoord(this.area.maxX()); x++) {
                    ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.FULL, false);
                    if (chunk != null) {
                        chunks.add(chunk);
                    }
                }
            }

            BiomeResolver resolver = (x, y, z, sampler) -> this.biomes.getOrDefault(new QuartPosKey(x, y, z), level.getNoiseBiome(x, y, z));
            for (ChunkAccess chunk : chunks) {
                chunk.fillBiomesFromNoise(resolver, level.getChunkSource().randomState().sampler());
                chunk.setUnsaved(true);
            }
            level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
        }
    }
}
