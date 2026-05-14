package de.maax.tweaked.client;

import com.mojang.authlib.GameProfile;
import de.maax.tweaked.Tweaked;
import de.maax.tweaked.network.GameRuleSync;
import de.maax.tweaked.world.TweakedGameRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class TweakedAdminScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 0;
    private static final int OUTER_GAP = 4;
    private static final int CATEGORY_SEARCH_GAP = 4;
    private static final int SEARCH_TEXT_PADDING_X = 8;
    private static final int SEARCH_TEXT_OFFSET_Y = 2;
    private static final int GAME_RULE_ROW_HEIGHT = 28;
    private static final int GAME_RULE_ROW_CONTENT_HEIGHT = 22;
    private static final int MOB_SPAWNING_TILE_SIZE = 52;
    private static final int MOB_SPAWNING_TILE_GAP = 6;
    private static final int MOB_SPAWNING_HEADER_HEIGHT = 18;
    private static final int MOB_SPAWNING_ROW_HEIGHT = MOB_SPAWNING_TILE_SIZE + MOB_SPAWNING_TILE_GAP;
    private static final int MOB_SPAWNING_MODEL_PADDING = 7;
    private static final int PLAYER_ROW_HEIGHT = 96;
    private static final int PLAYER_ROW_GAP = 8;
    private static final int PLAYER_MODEL_WIDTH = 94;
    private static final int PLAYER_ACTION_BUTTON_SIZE = 20;
    private static final int PLAYER_ACTION_BUTTON_GAP = 4;
    private static final ResourceLocation BUTTON_SPRITE = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
    private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
    private static final ResourceLocation DROPDOWN_CLOSED_SPRITE = ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "dropdown_closed");
    private static final ResourceLocation DROPDOWN_OPENED_SPRITE = ResourceLocation.fromNamespaceAndPath(Tweaked.MOD_ID, "dropdown_opened");
    private static final ResourceLocation TP_TO_ICON = ResourceLocation.withDefaultNamespace("textures/item/ender_pearl.png");
    private static final ResourceLocation TP_HERE_ICON = ResourceLocation.withDefaultNamespace("textures/item/ender_eye.png");
    private static final ResourceLocation HEAL_ICON = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final ResourceLocation FEED_ICON = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final ResourceLocation SET_XP_ICON = ResourceLocation.withDefaultNamespace("textures/item/experience_bottle.png");
    private static final String[] CATEGORIES = {"Gamerules", "Mob Spawning", "Players"};
    private static final String MOB_GRIEFING_CATEGORY = "gamerule.category.tweaked.mob_griefing";
    private static final List<GameRuleDefinition> GAME_RULE_DEFINITIONS = createGameRuleDefinitions();
    private static final List<String> BOOLEAN_GAMERULES = List.of(
            "doFireTick",
            "mobGriefing",
            "keepInventory",
            "doMobSpawning",
            "doMobLoot",
            "doTileDrops",
            "doEntityDrops",
            "commandBlockOutput",
            "naturalRegeneration",
            "doDaylightCycle",
            "logAdminCommands",
            "showDeathMessages",
            "sendCommandFeedback",
            "reducedDebugInfo",
            "spectatorsGenerateChunks",
            "disableElytraMovementCheck",
            "doWeatherCycle",
            "doLimitedCrafting",
            "announceAdvancements",
            "disableRaids",
            "doInsomnia",
            "doImmediateRespawn",
            "drowningDamage",
            "fallDamage",
            "fireDamage",
            "freezeDamage",
            "doPatrolSpawning",
            "doTraderSpawning",
            "doWardenSpawning",
            "forgiveDeadPlayers",
            "universalAnger",
            "blockExplosionDropDecay",
            "mobExplosionDropDecay",
            "tntExplosionDropDecay",
            "waterSourceConversion",
            "lavaSourceConversion",
            "globalSoundEvents",
            "doVinesSpread",
            "creeperGriefing",
            "witherGriefing",
            "ghastGriefing",
            "endermanGriefing",
            "villagerFarming",
            "zombieDoorBreaking",
            "ravagerGriefing",
            "sheepEatGrass",
            "snifferDigging",
            "mobItemPickup",
            "villagerItemPickup",
            "piglinGoldPickup",
            "allayItemPickup"
    );
    private static final List<String> INTEGER_GAMERULES = List.of(
            "randomTickSpeed",
            "spawnRadius",
            "maxEntityCramming",
            "maxCommandChainLength",
            "commandModificationBlockLimit",
            "playersSleepingPercentage",
            "snowAccumulationHeight"
    );

    private final Screen parent;
    private final GameRules displayedGameRules = new GameRules();
    private final Map<String, String> integerRuleDrafts = new HashMap<>();
    private Tab tab = Tab.GAMERULES;
    private int page;
    private int gameRuleScroll;
    private int mobSpawningScroll;
    private int playerScroll;
    private int gameRuleSyncTicks;
    private int playerListSyncTicks;
    private boolean gameRuleScrollbarDragging;
    private int gameRuleScrollbarDragOffset;
    private String search = "";
    private boolean searchFocused;
    private boolean categoryDropdownOpen;
    private String selectedCategory = CATEGORIES[0];
    private String selectedPlayer;
    private UUID focusedXpPlayer;
    private String xpDraft = "0";
    private GameRules.Key<?> focusedIntegerRule;
    private final Map<ResourceLocation, LivingEntity> mobPreviewEntities = new HashMap<>();
    private final Map<UUID, LivingEntity> playerPreviewEntities = new HashMap<>();
    private final Set<ResourceLocation> disabledMobSpawningTypes = new HashSet<>();
    private List<GameProfile> playerProfiles = List.of();
    private EditBox categorySearchBox;
    private int categorySearchLeft;
    private int categorySearchTop;
    private int categorySearchRight;
    private int categorySearchBottom;
    private EditBox playerLevel;

    public TweakedAdminScreen(Screen parent) {
        super(Component.translatable("menu.tweaked.admin"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.categorySearchBox = new EditBox(this.font, 0, 0, 0, BUTTON_HEIGHT, Component.translatable("menu.tweaked.search"));
        this.categorySearchBox.setHint(Component.literal("Search..."));
        this.categorySearchBox.setBordered(false);
        this.categorySearchBox.setMaxLength(64);
        this.categorySearchBox.setValue(this.search);
        this.categorySearchBox.setResponder(value -> {
            this.search = value;
            this.page = 0;
            this.gameRuleScroll = 0;
        });
        updateCategorySearchBoxBounds();
        this.addRenderableWidget(this.categorySearchBox);
        requestGameRuleValues();
        requestSpawnControlValues();
        requestPlayerProfiles();
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.gameRuleSyncTicks >= 20) {
            this.gameRuleSyncTicks = 0;
            requestGameRuleValues();
        }
        if (++this.playerListSyncTicks >= 20) {
            this.playerListSyncTicks = 0;
            requestPlayerProfiles();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    public void applyGameRuleValues(Map<String, String> values) {
        for (GameRuleDefinition definition : GAME_RULE_DEFINITIONS) {
            String value = values.get(definition.rule().getId());
            if (value == null) {
                continue;
            }

            if (definition.kind() == GameRuleKind.BOOLEAN) {
                setBooleanRuleValue(definition.rule(), Boolean.parseBoolean(value));
            } else {
                try {
                    int parsed = Integer.parseInt(value);
                    if (this.focusedIntegerRule == definition.rule()) {
                        setIntegerRuleCurrentValue(definition.rule(), parsed);
                    } else {
                        setIntegerRuleValue(definition.rule(), parsed);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void applyDisabledMobSpawningTypes(Set<ResourceLocation> disabledTypes) {
        this.disabledMobSpawningTypes.clear();
        this.disabledMobSpawningTypes.addAll(disabledTypes);
    }

    public void applyPlayerProfiles(List<GameProfile> profiles) {
        this.playerProfiles = profiles;
        this.playerPreviewEntities.keySet().removeIf(uuid -> profiles.stream().noneMatch(profile -> profile.getId().equals(uuid)));
        this.playerScroll = Math.min(this.playerScroll, maxPlayerScroll());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateCategorySearchBoxBounds();
        this.categorySearchBox.visible = false;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.categorySearchBox.visible = true;
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int panelTop = marginY;
        int panelBottom = this.height - marginY;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int left = marginX;
        int middle = left + leftWidth + gap;
        int right = this.width - marginX;

        drawPanel(guiGraphics, left, panelTop, left + leftWidth, panelBottom);
        drawPanel(guiGraphics, middle, panelTop, right, panelBottom);
        if (!this.categoryDropdownOpen) {
            renderCategorySearchBox(guiGraphics, mouseX, mouseY, partialTick);
        }
        drawCategories(guiGraphics, left, panelTop, left + leftWidth, mouseX, mouseY);
        drawSelectedCategoryContent(guiGraphics, middle, panelTop, right, panelBottom, mouseX, mouseY);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleCategoryDropdownClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0) {
            if (isCategorySearchClicked(mouseX, mouseY)) {
                this.focusedIntegerRule = null;
                this.categorySearchBox.setFocused(true);
                this.setFocused(this.categorySearchBox);
                this.categorySearchBox.onClick(mouseX, mouseY);
                return true;
            }
            clearCategorySearchFocusOnly();
        }
        if (button == 0 && handleGameRuleScrollbarClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handleMobSpawningClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handlePlayerClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && handleGameRuleClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.gameRuleScrollbarDragging) {
            this.gameRuleScrollbarDragging = false;
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.gameRuleScrollbarDragging) {
            updateGameRuleScrollFromScrollbar(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.focusedIntegerRule != null) {
            String value = integerDraft(this.focusedIntegerRule);
            if (value.length() < 16 && (Character.isDigit(codePoint) || codePoint == '-' && value.isEmpty())) {
                this.integerRuleDrafts.put(this.focusedIntegerRule.getId(), value + codePoint);
                return true;
            }

            return true;
        }

        if (this.focusedXpPlayer != null) {
            if (this.xpDraft.length() < 8 && Character.isDigit(codePoint)) {
                this.xpDraft += codePoint;
                return true;
            }

            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.focusedIntegerRule != null) {
            if (keyCode == 257 || keyCode == 335) {
                commitFocusedIntegerRule();
                return true;
            }
            if (keyCode == 256) {
                this.focusedIntegerRule = null;
                return true;
            }
            if (keyCode == 259) {
                String value = integerDraft(this.focusedIntegerRule);
                if (!value.isEmpty()) {
                    this.integerRuleDrafts.put(this.focusedIntegerRule.getId(), value.substring(0, value.length() - 1));
                }
                return true;
            }
            if (keyCode == 261) {
                this.integerRuleDrafts.put(this.focusedIntegerRule.getId(), "");
                return true;
            }

            return true;
        }

        if (this.focusedXpPlayer != null) {
            if (keyCode == 257 || keyCode == 335) {
                commitFocusedPlayerXp();
                return true;
            }
            if (keyCode == 256) {
                this.focusedXpPlayer = null;
                return true;
            }
            if (keyCode == 259) {
                if (!this.xpDraft.isEmpty()) {
                    this.xpDraft = this.xpDraft.substring(0, this.xpDraft.length() - 1);
                }
                return true;
            }
            if (keyCode == 261) {
                this.xpDraft = "";
                return true;
            }

            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ("Gamerules".equals(this.selectedCategory) && isGameRulePanelHovered(mouseX, mouseY)) {
            int maxScroll = Math.max(0, visibleGameRuleRows().size() - gameRuleVisibleRows());
            int direction = scrollY > 0.0D ? -1 : 1;
            int nextScroll = Math.max(0, Math.min(maxScroll, this.gameRuleScroll + direction * 3));
            if (nextScroll != this.gameRuleScroll) {
                this.gameRuleScroll = nextScroll;
                return true;
            }
        }

        if ("Mob Spawning".equals(this.selectedCategory) && isGameRulePanelHovered(mouseX, mouseY)) {
            int maxScroll = maxMobSpawningScroll();
            int direction = scrollY > 0.0D ? -1 : 1;
            int nextScroll = Math.max(0, Math.min(maxScroll, this.mobSpawningScroll + direction * 36));
            if (nextScroll != this.mobSpawningScroll) {
                this.mobSpawningScroll = nextScroll;
                return true;
            }
        }

        if ("Players".equals(this.selectedCategory) && isGameRulePanelHovered(mouseX, mouseY)) {
            int maxScroll = maxPlayerScroll();
            int direction = scrollY > 0.0D ? -1 : 1;
            int nextScroll = Math.max(0, Math.min(maxScroll, this.playerScroll + direction * (PLAYER_ROW_HEIGHT / 2)));
            if (nextScroll != this.playerScroll) {
                this.playerScroll = nextScroll;
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, 0x40000000);
        guiGraphics.fill(left, top, right, top + 1, 0xFF000000);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF000000);
        guiGraphics.fill(left, top, left + 1, bottom, 0xFF000000);
        guiGraphics.fill(right - 1, top, right, bottom, 0xFF000000);
    }

    private void drawCategories(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelRight, int mouseX, int mouseY) {
        int dropdownLeft = panelLeft + 20;
        int dropdownRight = panelRight - 20;
        int dropdownTop = panelTop + 46;
        int headerHeight = 24;
        int listTop = dropdownTop + headerHeight;
        int rowHeight = 13;
        int listHeight = rowHeight * CATEGORIES.length + 8;
        int titleY = panelTop + (dropdownTop - panelTop - this.font.lineHeight) / 2;

        guiGraphics.drawCenteredString(this.font, Component.literal("Categories"), (panelLeft + panelRight) / 2, titleY, 0xFFFFFF);

        drawMinecraftButton(guiGraphics, dropdownLeft, dropdownTop, dropdownRight, dropdownTop + headerHeight);
        guiGraphics.drawString(this.font, Component.literal(this.selectedCategory), dropdownLeft + 8, dropdownTop + 8, 0xFFFFFF, true);
        drawDropdownIndicator(guiGraphics, dropdownRight - 29, dropdownTop + 7);

        if (this.categoryDropdownOpen) {
            guiGraphics.fill(dropdownLeft, listTop, dropdownRight, listTop + listHeight, 0xFF000000);
            drawBorder(guiGraphics, dropdownLeft, listTop, dropdownRight, listTop + listHeight, 0xFF80FFFF);

            for (int index = 0; index < CATEGORIES.length; index++) {
                int rowTop = listTop + 4 + index * rowHeight;
                if (contains(mouseX, mouseY, dropdownLeft + 1, rowTop, dropdownRight - 1, rowTop + rowHeight)) {
                    guiGraphics.fill(dropdownLeft + 1, rowTop, dropdownRight - 1, rowTop + rowHeight, 0x80404040);
                }
                guiGraphics.drawString(this.font, Component.literal(CATEGORIES[index]), dropdownLeft + 8, listTop + 6 + index * rowHeight, 0xFFFFFF, true);
            }
        }
    }

    private void drawSelectedCategoryContent(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelRight, int panelBottom, int mouseX, int mouseY) {
        if ("Gamerules".equals(this.selectedCategory)) {
            drawGameRulesMenu(guiGraphics, panelLeft, panelTop, panelRight, panelBottom, mouseX, mouseY);
        } else if ("Mob Spawning".equals(this.selectedCategory)) {
            drawMobSpawningMenu(guiGraphics, panelLeft, panelTop, panelRight, panelBottom, mouseX, mouseY);
        } else if ("Players".equals(this.selectedCategory)) {
            drawPlayersMenu(guiGraphics, panelLeft, panelTop, panelRight, panelBottom, mouseX, mouseY);
        }
    }

    private void drawGameRulesMenu(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelRight, int panelBottom, int mouseX, int mouseY) {
        int titleY = panelTop + 13;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = panelTop + 34;
        int listBottom = panelBottom - 16;
        int labelLeft = listLeft + 10;
        int controlWidth = 68;
        int controlLeft = listRight - controlWidth - 8;
        int labelWidth = Math.max(80, controlLeft - labelLeft - 12);

        guiGraphics.drawCenteredString(this.font, Component.translatable("editGamerule.title"), (panelLeft + panelRight) / 2, titleY, 0xFFFFFF);

        List<GameRuleMenuRow> rows = visibleGameRuleRows();
        int visibleRows = Math.max(1, (listBottom - listTop) / GAME_RULE_ROW_HEIGHT);
        int maxScroll = Math.max(0, rows.size() - visibleRows);
        this.gameRuleScroll = Math.max(0, Math.min(maxScroll, this.gameRuleScroll));

        int end = Math.min(rows.size(), this.gameRuleScroll + visibleRows);
        for (int rowIndex = this.gameRuleScroll; rowIndex < end; rowIndex++) {
            GameRuleMenuRow row = rows.get(rowIndex);
            int y = listTop + (rowIndex - this.gameRuleScroll) * GAME_RULE_ROW_HEIGHT;

            if (row.categoryKey() != null) {
                Component category = Component.translatable(row.categoryKey());
                drawGameRuleLabel(guiGraphics, category, listLeft, y + 8, labelWidth, 0xFFFF55);
                continue;
            }

            int rowRight = controlLeft - 8;
            int rowBottom = y + GAME_RULE_ROW_CONTENT_HEIGHT;
            boolean hovering = contains(mouseX, mouseY, listLeft, y, listRight, rowBottom);
            guiGraphics.fill(listLeft, y, rowRight, rowBottom, 0x70000000);
            if (contains(mouseX, mouseY, listLeft, y, rowRight, rowBottom)) {
                drawBorder(guiGraphics, listLeft, y, rowRight, rowBottom, 0xFF80FFFF);
            }

            drawGameRuleLabel(guiGraphics, Component.translatable(row.rule().getDescriptionId()), labelLeft, y, labelWidth, 0xFFFFFF);

            drawGameRuleControl(guiGraphics, row, controlLeft, y + 1, listRight - 8, y + 21);
            if (hovering) {
                this.setTooltipForNextRenderPass(createGameRuleTooltip(row.definition()));
            }
        }

        if (maxScroll > 0) {
            drawGameRuleScrollbar(guiGraphics, rows.size(), visibleRows, maxScroll, listRight + 4, listTop, listBottom);
        }
    }

    private void drawMobSpawningMenu(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelRight, int panelBottom, int mouseX, int mouseY) {
        int titleY = panelTop + 13;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = panelTop + 34;
        int listBottom = panelBottom - 16;
        int columns = mobSpawningColumns(listLeft, listRight);
        List<MobSpawningRow> rows = visibleMobSpawningRows(columns);
        int contentHeight = mobSpawningContentHeight(rows);
        int viewportHeight = listBottom - listTop;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        this.mobSpawningScroll = Math.max(0, Math.min(maxScroll, this.mobSpawningScroll));

        guiGraphics.drawCenteredString(this.font, Component.literal("Mob Spawning"), (panelLeft + panelRight) / 2, titleY, 0xFFFFFF);

        guiGraphics.enableScissor(listLeft, listTop, listRight, listBottom);
        int y = listTop - this.mobSpawningScroll;
        for (MobSpawningRow row : rows) {
            int rowHeight = mobSpawningRowHeight(row);
            if (y + rowHeight >= listTop && y <= listBottom) {
                if (row.namespace() != null) {
                    guiGraphics.drawString(this.font, Component.literal(namespaceTitle(row.namespace())), listLeft, y + 4, 0xFFFF55, false);
                } else {
                    drawMobSpawningRow(guiGraphics, row.mobs(), listLeft, listRight, y, mouseX, mouseY);
                }
            }
            y += rowHeight;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            drawPixelScrollbar(guiGraphics, this.mobSpawningScroll, maxScroll, listRight + 4, listTop, listBottom, contentHeight);
        }
    }

    private void drawMobSpawningRow(GuiGraphics guiGraphics, List<MobSpawningEntry> mobs, int listLeft, int listRight, int y, int mouseX, int mouseY) {
        for (int index = 0; index < mobs.size(); index++) {
            MobSpawningEntry mob = mobs.get(index);
            int tileLeft = listLeft + index * (MOB_SPAWNING_TILE_SIZE + MOB_SPAWNING_TILE_GAP);
            int tileRight = Math.min(tileLeft + MOB_SPAWNING_TILE_SIZE, listRight);
            int tileBottom = y + MOB_SPAWNING_TILE_SIZE;
            boolean hovering = contains(mouseX, mouseY, tileLeft, y, tileRight, tileBottom);
            boolean disabled = this.disabledMobSpawningTypes.contains(mob.id());

            guiGraphics.fill(tileLeft, y, tileRight, tileBottom, 0x70000000);
            if (disabled) {
                guiGraphics.fill(tileLeft, y, tileRight, tileBottom, 0x80400000);
            }
            if (hovering) {
                drawBorder(guiGraphics, tileLeft, y, tileRight, tileBottom, 0xFF80FFFF);
                this.setTooltipForNextRenderPass(createMobSpawningTooltip(mob));
            }

            LivingEntity entity = mobPreviewEntity(mob);
            if (entity != null) {
                int inset = MOB_SPAWNING_MODEL_PADDING;
                int scale = mobPreviewScale(entity);
                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics,
                        tileLeft + inset,
                        y + inset,
                        tileRight - inset,
                        tileBottom - inset,
                        scale,
                        0.0625F,
                        mouseX,
                        mouseY,
                        entity
                );
            } else {
                drawCenteredTruncated(guiGraphics, Component.translatable(mob.type().getDescriptionId()), tileLeft + 4, y + 21, tileRight - tileLeft - 8, 0xFFFFFF);
            }
        }
    }

    private void drawPlayersMenu(GuiGraphics guiGraphics, int panelLeft, int panelTop, int panelRight, int panelBottom, int mouseX, int mouseY) {
        int titleY = panelTop + 13;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = panelTop + 34;
        int listBottom = panelBottom - 16;
        List<GameProfile> players = visiblePlayerProfiles();
        int contentHeight = playerContentHeight(players.size());
        int viewportHeight = listBottom - listTop;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        this.playerScroll = Math.max(0, Math.min(maxScroll, this.playerScroll));

        guiGraphics.drawCenteredString(this.font, Component.literal("Players"), (panelLeft + panelRight) / 2, titleY, 0xFFFFFF);

        if (players.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("menu.tweaked.no_players"), (listLeft + listRight) / 2, listTop + 20, 0xA0A0A0);
            return;
        }

        guiGraphics.enableScissor(listLeft, listTop, listRight, listBottom);
        int y = listTop - this.playerScroll;
        for (GameProfile profile : players) {
            if (y + PLAYER_ROW_HEIGHT >= listTop && y <= listBottom) {
                drawPlayerRow(guiGraphics, profile, listLeft, listRight, y, mouseX, mouseY);
            }
            y += PLAYER_ROW_HEIGHT + PLAYER_ROW_GAP;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            drawPixelScrollbar(guiGraphics, this.playerScroll, maxScroll, listRight + 4, listTop, listBottom, contentHeight);
        }
    }

    private void drawPlayerRow(GuiGraphics guiGraphics, GameProfile profile, int left, int right, int top, int mouseX, int mouseY) {
        int bottom = top + PLAYER_ROW_HEIGHT;
        boolean hovering = contains(mouseX, mouseY, left, top, right, bottom);
        guiGraphics.fill(left, top, right, bottom, 0x70000000);
        if (hovering) {
            drawBorder(guiGraphics, left, top, right, bottom, 0xFF80FFFF);
        }

        LivingEntity player = playerEntity(profile);
        int modelLeft = left + 8;
        int modelRight = Math.min(modelLeft + PLAYER_MODEL_WIDTH, right - 160);
        if (player != null && modelRight > modelLeft + 24) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    modelLeft,
                    top + 6,
                    modelRight,
                    bottom - 8,
                    34,
                    0.0625F,
                    mouseX,
                    mouseY,
                    player
            );
        } else {
            guiGraphics.fill(modelLeft, top + 10, modelLeft + 54, bottom - 10, 0x404A638F);
        }

        int actionLeft = Math.max(left + PLAYER_MODEL_WIDTH + 230, right - playerActionAreaWidth() - 10);
        int textLeft = left + PLAYER_MODEL_WIDTH + 24;
        int textWidth = Math.max(60, actionLeft - textLeft - 10);
        guiGraphics.drawString(this.font, Component.literal("Name"), textLeft, top + 16, 0xFFFF00, false);
        drawTruncated(guiGraphics, Component.literal(profile.getName()), textLeft, top + 29, textWidth, 0xFFFFFF);

        guiGraphics.drawString(this.font, Component.literal("UUID"), textLeft, top + 48, 0xFFFF00, false);
        drawTruncated(guiGraphics, Component.literal(profile.getId().toString()), textLeft, top + 61, textWidth, 0xFFFFFF);
        drawPlayerActions(guiGraphics, profile, actionLeft, top + 10, right - 8, mouseX, mouseY);
    }

    private void drawPlayerActions(GuiGraphics guiGraphics, GameProfile profile, int left, int top, int right, int mouseX, int mouseY) {
        drawPlayerActionButton(guiGraphics, TP_TO_ICON, Component.literal("TP To"), left, top, mouseX, mouseY);
        drawPlayerActionButton(guiGraphics, TP_HERE_ICON, Component.literal("TP Here"), left + 24, top, mouseX, mouseY);
        drawPlayerActionButton(guiGraphics, HEAL_ICON, Component.literal("Heal"), left + 48, top, mouseX, mouseY);
        drawPlayerActionButton(guiGraphics, FEED_ICON, Component.literal("Feed"), left + 72, top, mouseX, mouseY);

        int xpY = top + PLAYER_ACTION_BUTTON_SIZE + PLAYER_ACTION_BUTTON_GAP;
        int inputRight = left + playerActionAreaWidth() - PLAYER_ACTION_BUTTON_SIZE - PLAYER_ACTION_BUTTON_GAP;
        guiGraphics.fill(left, xpY, inputRight, xpY + PLAYER_ACTION_BUTTON_SIZE, 0xFF000000);
        drawBorder(guiGraphics, left, xpY, inputRight, xpY + PLAYER_ACTION_BUTTON_SIZE, profile.getId().equals(this.focusedXpPlayer) ? 0xFFFFFFFF : 0xFF808080);
        String xpText = profile.getId().equals(this.focusedXpPlayer) ? this.xpDraft : "0";
        drawTruncated(guiGraphics, Component.literal(xpText), left + 4, xpY + 6, inputRight - left - 8, 0xFFFFFF);
        drawPlayerActionButton(guiGraphics, SET_XP_ICON, Component.literal("Set XP"), inputRight + PLAYER_ACTION_BUTTON_GAP, xpY, mouseX, mouseY);
    }

    private void drawPlayerActionButton(GuiGraphics guiGraphics, ResourceLocation icon, Component label, int x, int y, int mouseX, int mouseY) {
        drawMinecraftButton(guiGraphics, x, y, x + PLAYER_ACTION_BUTTON_SIZE, y + PLAYER_ACTION_BUTTON_SIZE);
        if (contains(mouseX, mouseY, x, y, x + PLAYER_ACTION_BUTTON_SIZE, y + PLAYER_ACTION_BUTTON_SIZE)) {
            drawBorder(guiGraphics, x, y, x + PLAYER_ACTION_BUTTON_SIZE, y + PLAYER_ACTION_BUTTON_SIZE, 0xFF80FFFF);
            this.setTooltipForNextRenderPass(label);
        }
        guiGraphics.blit(icon, x + 2, y + 2, 0, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    private void drawPixelScrollbar(GuiGraphics guiGraphics, int scroll, int maxScroll, int x, int top, int bottom, int contentHeight) {
        int height = bottom - top;
        int scrollerHeight = Math.max(32, Math.min(height * height / Math.max(1, contentHeight), height - 8));
        int scrollerTop = top + (height - scrollerHeight) * scroll / maxScroll;

        guiGraphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, x, top, 6, height);
        guiGraphics.blitSprite(SCROLLER_SPRITE, x, scrollerTop, 6, scrollerHeight);
    }

    private void drawGameRuleScrollbar(GuiGraphics guiGraphics, int totalRows, int visibleRows, int maxScroll, int x, int top, int bottom) {
        int height = bottom - top;
        int scrollerHeight = gameRuleScrollerHeight(totalRows, height);
        int scrollerTop = top + (height - scrollerHeight) * this.gameRuleScroll / maxScroll;

        guiGraphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, x, top, 6, height);
        guiGraphics.blitSprite(SCROLLER_SPRITE, x, scrollerTop, 6, scrollerHeight);
    }

    private int gameRuleScrollerHeight(int totalRows, int scrollbarHeight) {
        int contentHeight = Math.max(1, totalRows * GAME_RULE_ROW_HEIGHT);
        int scrollerHeight = scrollbarHeight * scrollbarHeight / contentHeight;
        return Math.max(32, Math.min(scrollerHeight, scrollbarHeight - 8));
    }

    private void drawGameRuleControl(GuiGraphics guiGraphics, GameRuleMenuRow row, int left, int top, int right, int bottom) {
        if (row.definition().kind() == GameRuleKind.BOOLEAN) {
            drawMinecraftButton(guiGraphics, left, top, right, bottom);
            boolean enabled = booleanRuleValue(row.rule());
            guiGraphics.drawCenteredString(this.font, enabled ? Component.literal("ON") : Component.literal("OFF"), (left + right) / 2, top + 6, 0xFFFFFF);
            return;
        }

        guiGraphics.fill(left, top, right, bottom, 0xFF000000);
        drawBorder(guiGraphics, left, top, right, bottom, row.rule() == this.focusedIntegerRule ? 0xFFFFFFFF : 0xFF808080);
        guiGraphics.drawString(this.font, Component.literal(integerDraft(row.rule())), left + 6, top + 6, 0xFFFFFF, false);
    }

    private void drawGameRuleLabel(GuiGraphics guiGraphics, Component label, int x, int y, int width, int color) {
        List<FormattedCharSequence> labelLines = this.font.split(label, width);
        if (!labelLines.isEmpty()) {
            int textY = labelLines.size() > 1 ? y + 1 : y + (GAME_RULE_ROW_CONTENT_HEIGHT - this.font.lineHeight) / 2;
            guiGraphics.drawString(this.font, labelLines.get(0), x, textY, color, false);
        }
        if (labelLines.size() > 1) {
            guiGraphics.drawString(this.font, labelLines.get(1), x, y + 11, color, false);
        }
    }

    private void drawCenteredTruncated(GuiGraphics guiGraphics, Component label, int x, int y, int width, int color) {
        String text = label.getString();
        if (this.font.width(text) > width) {
            text = this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
        }

        guiGraphics.drawString(this.font, Component.literal(text), x + (width - this.font.width(text)) / 2, y, color, false);
    }

    private void drawTruncated(GuiGraphics guiGraphics, Component label, int x, int y, int width, int color) {
        String text = label.getString();
        if (this.font.width(text) > width) {
            text = this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width("..."))) + "...";
        }

        guiGraphics.drawString(this.font, Component.literal(text), x, y, color, false);
    }

    private void updateCategorySearchBoxBounds() {
        if (this.categorySearchBox == null) {
            return;
        }

        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX;
        int panelRight = panelLeft + leftWidth;
        int dropdownLeft = panelLeft + 20;
        int dropdownRight = panelRight - 20;
        int dropdownTop = marginY + 46;
        int headerHeight = 24;
        int searchTop = dropdownTop + headerHeight + CATEGORY_SEARCH_GAP;

        this.categorySearchLeft = dropdownLeft;
        this.categorySearchTop = searchTop;
        this.categorySearchRight = dropdownRight;
        this.categorySearchBottom = searchTop + BUTTON_HEIGHT;

        this.categorySearchBox.setX(this.categorySearchLeft + SEARCH_TEXT_PADDING_X);
        this.categorySearchBox.setY(this.categorySearchTop + (BUTTON_HEIGHT - this.font.lineHeight) / 2 + SEARCH_TEXT_OFFSET_Y);
        this.categorySearchBox.setWidth(this.categorySearchRight - this.categorySearchLeft - SEARCH_TEXT_PADDING_X * 2);
        this.categorySearchBox.setHeight(this.font.lineHeight);
    }

    private void renderCategorySearchBox(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.categorySearchBox == null) {
            return;
        }

        int borderColor = this.categorySearchBox.isFocused() ? 0xFFFFFFFF : 0xFF505050;
        int textColor = this.categorySearchBox.getValue().isEmpty() ? 0xFF505050 : 0xFFFFFFFF;
        guiGraphics.fill(this.categorySearchLeft, this.categorySearchTop, this.categorySearchRight, this.categorySearchBottom, 0xFF000000);
        drawBorder(guiGraphics, this.categorySearchLeft, this.categorySearchTop, this.categorySearchRight, this.categorySearchBottom, borderColor);
        this.categorySearchBox.setTextColor(textColor);
        this.categorySearchBox.setTextColorUneditable(textColor);

        boolean focused = this.categorySearchBox.isFocused();
        if (focused) {
            this.categorySearchBox.setFocused(false);
        }

        this.categorySearchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        if (focused) {
            this.categorySearchBox.setFocused(true);
        }
    }

    private boolean isCategorySearchClicked(double mouseX, double mouseY) {
        return this.categorySearchBox != null
                && contains(mouseX, mouseY, this.categorySearchLeft, this.categorySearchTop, this.categorySearchRight, this.categorySearchBottom);
    }

    private void clearCategorySearchFocusOnly() {
        this.searchFocused = false;
        if (this.categorySearchBox != null) {
            this.categorySearchBox.setFocused(false);
            if (this.getFocused() == this.categorySearchBox) {
                this.setFocused(null);
            }
        }
    }

    private void clearCategorySearchFocus() {
        this.search = "";
        this.page = 0;
        clearCategorySearchFocusOnly();
        if (this.categorySearchBox != null) {
            this.categorySearchBox.setValue("");
        }
    }

    private boolean handleCategoryDropdownClick(double mouseX, double mouseY) {
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX;
        int panelRight = panelLeft + leftWidth;
        int dropdownLeft = panelLeft + 20;
        int dropdownRight = panelRight - 20;
        int dropdownTop = marginY + 46;
        int headerHeight = 24;
        int listTop = dropdownTop + headerHeight;
        int rowHeight = 13;
        int listHeight = rowHeight * CATEGORIES.length + 8;

        if (contains(mouseX, mouseY, dropdownLeft, dropdownTop, dropdownRight, dropdownTop + headerHeight)) {
            this.categoryDropdownOpen = !this.categoryDropdownOpen;
            playClickSound();
            return true;
        }

        if (this.categoryDropdownOpen && contains(mouseX, mouseY, dropdownLeft, listTop, dropdownRight, listTop + listHeight)) {
            int index = (int)((mouseY - listTop - 6) / rowHeight);
            if (index >= 0 && index < CATEGORIES.length) {
                this.selectedCategory = CATEGORIES[index];
                this.gameRuleScroll = 0;
                this.mobSpawningScroll = 0;
                this.playerScroll = 0;
                clearCategorySearchFocus();
                playClickSound();
            }
            this.categoryDropdownOpen = false;
            return true;
        }

        if (this.categoryDropdownOpen) {
            this.categoryDropdownOpen = false;
            return true;
        }

        return false;
    }

    private boolean handleGameRuleScrollbarClick(double mouseX, double mouseY) {
        if (!"Gamerules".equals(this.selectedCategory)) {
            return false;
        }

        GameRuleScrollbar scrollbar = gameRuleScrollbar();
        if (scrollbar.maxScroll() <= 0
                || !contains(mouseX, mouseY, scrollbar.left(), scrollbar.top(), scrollbar.right(), scrollbar.bottom())) {
            return false;
        }

        this.gameRuleScrollbarDragging = true;
        this.gameRuleScrollbarDragOffset = (int)Math.round(mouseY) - scrollbar.scrollerTop();
        if (this.gameRuleScrollbarDragOffset < 0 || this.gameRuleScrollbarDragOffset > scrollbar.scrollerHeight()) {
            this.gameRuleScrollbarDragOffset = scrollbar.scrollerHeight() / 2;
            updateGameRuleScrollFromScrollbar(mouseY);
        }
        return true;
    }

    private void updateGameRuleScrollFromScrollbar(double mouseY) {
        GameRuleScrollbar scrollbar = gameRuleScrollbar();
        if (scrollbar.maxScroll() <= 0) {
            this.gameRuleScroll = 0;
            return;
        }

        int available = Math.max(1, scrollbar.bottom() - scrollbar.top() - scrollbar.scrollerHeight());
        int scrollerTop = (int)Math.round(mouseY) - this.gameRuleScrollbarDragOffset;
        int offset = Math.max(0, Math.min(available, scrollerTop - scrollbar.top()));
        this.gameRuleScroll = Math.max(0, Math.min(scrollbar.maxScroll(), Math.round((float)offset * scrollbar.maxScroll() / available)));
    }

    private GameRuleScrollbar gameRuleScrollbar() {
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelRight = this.width - marginX;
        int listRight = panelRight - 18;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        int scrollbarLeft = listRight + 4;
        int scrollbarHeight = listBottom - listTop;
        int totalRows = visibleGameRuleRows().size();
        int maxScroll = Math.max(0, totalRows - gameRuleVisibleRows());
        int scrollerHeight = gameRuleScrollerHeight(totalRows, scrollbarHeight);
        int scrollerTop = maxScroll == 0 ? listTop : listTop + (scrollbarHeight - scrollerHeight) * this.gameRuleScroll / maxScroll;
        return new GameRuleScrollbar(scrollbarLeft, listTop, scrollbarLeft + 6, listBottom, scrollerTop, scrollerHeight, maxScroll);
    }

    private boolean handleGameRuleClick(double mouseX, double mouseY) {
        if (!"Gamerules".equals(this.selectedCategory) || !isGameRulePanelHovered(mouseX, mouseY)) {
            return false;
        }

        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = marginY + 34;
        int controlLeft = listRight - 76;
        int controlRight = listRight - 8;

        if (!contains(mouseX, mouseY, listLeft, listTop, listRight, this.height - marginY - 16)) {
            return false;
        }

        int visibleIndex = (int)((mouseY - listTop) / GAME_RULE_ROW_HEIGHT);
        int rowIndex = this.gameRuleScroll + visibleIndex;
        List<GameRuleMenuRow> rows = visibleGameRuleRows();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return false;
        }

        GameRuleMenuRow row = rows.get(rowIndex);
        if (row.categoryKey() != null) {
            return false;
        }

        if (!contains(mouseX, mouseY, controlLeft, listTop + visibleIndex * GAME_RULE_ROW_HEIGHT + 2, controlRight, listTop + visibleIndex * GAME_RULE_ROW_HEIGHT + 22)) {
            return false;
        }

        if (row.definition().kind() == GameRuleKind.INTEGER) {
            this.focusedIntegerRule = row.rule();
            clearCategorySearchFocusOnly();
            playClickSound();
            return true;
        }

        boolean next = !booleanRuleValue(row.rule());
        setBooleanRuleValue(row.rule(), next);
        sendCommand("gamerule " + row.rule().getId() + " " + next);
        playClickSound();
        return true;
    }

    private boolean handleMobSpawningClick(double mouseX, double mouseY) {
        if (!"Mob Spawning".equals(this.selectedCategory) || !isGameRulePanelHovered(mouseX, mouseY)) {
            return false;
        }

        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        if (!contains(mouseX, mouseY, listLeft, listTop, listRight, listBottom)) {
            return false;
        }

        int columns = mobSpawningColumns(listLeft, listRight);
        List<MobSpawningRow> rows = visibleMobSpawningRows(columns);
        int y = listTop - this.mobSpawningScroll;
        for (MobSpawningRow row : rows) {
            int rowHeight = mobSpawningRowHeight(row);
            if (row.namespace() == null && y + rowHeight >= listTop && y <= listBottom) {
                for (int index = 0; index < row.mobs().size(); index++) {
                    int tileLeft = listLeft + index * (MOB_SPAWNING_TILE_SIZE + MOB_SPAWNING_TILE_GAP);
                    int tileRight = Math.min(tileLeft + MOB_SPAWNING_TILE_SIZE, listRight);
                    int tileBottom = y + MOB_SPAWNING_TILE_SIZE;
                    if (contains(mouseX, mouseY, tileLeft, y, tileRight, tileBottom)) {
                        MobSpawningEntry mob = row.mobs().get(index);
                        boolean enabled = this.disabledMobSpawningTypes.remove(mob.id());
                        if (!enabled) {
                            this.disabledMobSpawningTypes.add(mob.id());
                        }
                        sendCommand("tweaked spawning set " + mob.id() + " " + enabled);
                        playClickSound();
                        return true;
                    }
                }
            }
            y += rowHeight;
        }

        return false;
    }

    private boolean handlePlayerClick(double mouseX, double mouseY) {
        if (!"Players".equals(this.selectedCategory) || !isGameRulePanelHovered(mouseX, mouseY)) {
            return false;
        }

        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        if (!contains(mouseX, mouseY, listLeft, listTop, listRight, listBottom)) {
            return false;
        }

        List<GameProfile> players = visiblePlayerProfiles();
        int y = listTop - this.playerScroll;
        for (GameProfile profile : players) {
            if (y + PLAYER_ROW_HEIGHT >= listTop && y <= listBottom && contains(mouseX, mouseY, listLeft, y, listRight, y + PLAYER_ROW_HEIGHT)) {
                int actionLeft = Math.max(listLeft + PLAYER_MODEL_WIDTH + 230, listRight - playerActionAreaWidth() - 10);
                if (handlePlayerActionClick(profile, actionLeft, y + 10, mouseX, mouseY)) {
                    playClickSound();
                    return true;
                }
            }
            y += PLAYER_ROW_HEIGHT + PLAYER_ROW_GAP;
        }

        this.focusedXpPlayer = null;
        return false;
    }

    private boolean handlePlayerActionClick(GameProfile profile, int left, int top, double mouseX, double mouseY) {
        String[] commands = {
                "tp " + selfName() + " " + profile.getName(),
                "tp " + profile.getName() + " " + selfName(),
                "heal " + profile.getName(),
                "feed " + profile.getName()
        };

        for (int index = 0; index < commands.length; index++) {
            int x = left + index * (PLAYER_ACTION_BUTTON_SIZE + PLAYER_ACTION_BUTTON_GAP);
            if (contains(mouseX, mouseY, x, top, x + PLAYER_ACTION_BUTTON_SIZE, top + PLAYER_ACTION_BUTTON_SIZE)) {
                sendCommand(commands[index]);
                return true;
            }
        }

        int xpY = top + PLAYER_ACTION_BUTTON_SIZE + PLAYER_ACTION_BUTTON_GAP;
        int inputRight = left + playerActionAreaWidth() - PLAYER_ACTION_BUTTON_SIZE - PLAYER_ACTION_BUTTON_GAP;
        if (contains(mouseX, mouseY, left, xpY, inputRight, xpY + PLAYER_ACTION_BUTTON_SIZE)) {
            this.focusedXpPlayer = profile.getId();
            this.xpDraft = "0";
            return true;
        }
        int setLeft = inputRight + PLAYER_ACTION_BUTTON_GAP;
        int setRight = setLeft + PLAYER_ACTION_BUTTON_SIZE;
        if (contains(mouseX, mouseY, setLeft, xpY, setRight, xpY + PLAYER_ACTION_BUTTON_SIZE)) {
            this.focusedXpPlayer = profile.getId();
            commitPlayerXp(profile.getName());
            return true;
        }

        return false;
    }

    private boolean isGameRulePanelHovered(double mouseX, double mouseY) {
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        return contains(mouseX, mouseY, panelLeft, marginY, panelRight, this.height - marginY);
    }

    private int gameRuleVisibleRows() {
        int marginY = 20;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        return Math.max(1, (listBottom - listTop) / GAME_RULE_ROW_HEIGHT);
    }

    private List<GameRuleMenuRow> visibleGameRuleRows() {
        List<GameRuleMenuRow> rows = new ArrayList<>();
        for (GameRules.Category category : GameRules.Category.values()) {
            if (category == GameRules.Category.SPAWNING) {
                List<GameRuleDefinition> mobGriefingRules = GAME_RULE_DEFINITIONS.stream()
                        .filter(TweakedAdminScreen::isMobGriefingDefinition)
                        .filter(definition -> matchesGameRuleSearch(definition, MOB_GRIEFING_CATEGORY))
                        .sorted(Comparator.comparingInt(TweakedAdminScreen::mobGriefingOrder))
                        .toList();
                addGameRuleRows(rows, MOB_GRIEFING_CATEGORY, mobGriefingRules);
            }

            List<GameRuleDefinition> definitions = GAME_RULE_DEFINITIONS.stream()
                    .filter(definition -> definition.category() == category)
                    .filter(definition -> !isMobGriefingDefinition(definition))
                    .filter(definition -> matchesGameRuleSearch(definition, category.getDescriptionId()))
                    .sorted(Comparator.comparing(definition -> definition.rule().getId()))
                    .toList();
            addGameRuleRows(rows, category.getDescriptionId(), definitions);
        }
        return rows;
    }

    private boolean matchesGameRuleSearch(GameRuleDefinition definition) {
        return matchesGameRuleSearch(definition, definition.category().getDescriptionId());
    }

    private boolean matchesGameRuleSearch(GameRuleDefinition definition, String categoryKey) {
        String needle = this.search.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }

        String label = Component.translatable(definition.rule().getDescriptionId()).getString();
        return definition.rule().getId().toLowerCase(Locale.ROOT).contains(needle)
                || label.toLowerCase(Locale.ROOT).contains(needle)
                || Component.translatable(categoryKey).getString().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static void addGameRuleRows(List<GameRuleMenuRow> rows, String categoryKey, List<GameRuleDefinition> definitions) {
        if (!definitions.isEmpty()) {
            rows.add(new GameRuleMenuRow(categoryKey, null, null));
            definitions.forEach(definition -> rows.add(new GameRuleMenuRow(null, definition, definition.rule())));
        }
    }

    private static boolean isMobGriefingDefinition(GameRuleDefinition definition) {
        return definition.rule() == GameRules.RULE_MOBGRIEFING || TweakedGameRules.isMobGriefingDetail(definition.rule());
    }

    private static int mobGriefingOrder(GameRuleDefinition definition) {
        if (definition.rule() == GameRules.RULE_MOBGRIEFING) {
            return 0;
        }

        int detailIndex = TweakedGameRules.ALL.indexOf(definition.rule());
        return detailIndex >= 0 ? detailIndex + 1 : Integer.MAX_VALUE;
    }

    private int maxMobSpawningScroll() {
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        int listLeft = panelLeft + 18;
        int listRight = panelRight - 18;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        int columns = mobSpawningColumns(listLeft, listRight);
        int contentHeight = mobSpawningContentHeight(visibleMobSpawningRows(columns));
        return Math.max(0, contentHeight - (listBottom - listTop));
    }

    private int maxPlayerScroll() {
        int marginX = 56;
        int marginY = 20;
        int gap = 6;
        int availableWidth = this.width - marginX * 2 - gap;
        int leftWidth = availableWidth / 3;
        int panelLeft = marginX + leftWidth + gap;
        int panelRight = this.width - marginX;
        int listTop = marginY + 34;
        int listBottom = this.height - marginY - 16;
        int contentHeight = playerContentHeight(visiblePlayerProfiles().size());
        return Math.max(0, contentHeight - (listBottom - listTop));
    }

    private int playerActionAreaWidth() {
        return PLAYER_ACTION_BUTTON_SIZE * 4 + PLAYER_ACTION_BUTTON_GAP * 3;
    }

    private int playerContentHeight(int playerCount) {
        if (playerCount == 0) {
            return 0;
        }

        return playerCount * PLAYER_ROW_HEIGHT + (playerCount - 1) * PLAYER_ROW_GAP;
    }

    private int mobSpawningColumns(int listLeft, int listRight) {
        int width = listRight - listLeft;
        return Math.max(1, (width + MOB_SPAWNING_TILE_GAP) / (MOB_SPAWNING_TILE_SIZE + MOB_SPAWNING_TILE_GAP));
    }

    private List<MobSpawningRow> visibleMobSpawningRows(int columns) {
        Map<String, List<MobSpawningEntry>> grouped = new HashMap<>();
        BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type != EntityType.PLAYER)
                .filter(type -> type != EntityType.ENDER_DRAGON)
                .filter(type -> type.getCategory() != MobCategory.MISC)
                .map(type -> new MobSpawningEntry(BuiltInRegistries.ENTITY_TYPE.getKey(type), type))
                .filter(this::matchesMobSpawningSearch)
                .forEach(entry -> grouped.computeIfAbsent(entry.id().getNamespace(), ignored -> new ArrayList<>()).add(entry));

        List<String> namespaces = new ArrayList<>(grouped.keySet());
        namespaces.sort((left, right) -> {
            if ("minecraft".equals(left) && !"minecraft".equals(right)) {
                return -1;
            }
            if (!"minecraft".equals(left) && "minecraft".equals(right)) {
                return 1;
            }
            return left.compareTo(right);
        });

        List<MobSpawningRow> rows = new ArrayList<>();
        for (String namespace : namespaces) {
            List<MobSpawningEntry> mobs = grouped.get(namespace);
            mobs.sort(Comparator.comparing(entry -> Component.translatable(entry.type().getDescriptionId()).getString().toLowerCase(Locale.ROOT)));
            rows.add(new MobSpawningRow(namespace, null));
            for (int index = 0; index < mobs.size(); index += columns) {
                rows.add(new MobSpawningRow(null, mobs.subList(index, Math.min(index + columns, mobs.size()))));
            }
        }
        return rows;
    }

    private boolean matchesMobSpawningSearch(MobSpawningEntry entry) {
        String needle = this.search.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }

        String label = Component.translatable(entry.type().getDescriptionId()).getString().toLowerCase(Locale.ROOT);
        return entry.id().toString().toLowerCase(Locale.ROOT).contains(needle)
                || entry.id().getNamespace().toLowerCase(Locale.ROOT).contains(needle)
                || namespaceTitle(entry.id().getNamespace()).toLowerCase(Locale.ROOT).contains(needle)
                || label.contains(needle);
    }

    private int mobSpawningContentHeight(List<MobSpawningRow> rows) {
        int height = 0;
        for (MobSpawningRow row : rows) {
            height += mobSpawningRowHeight(row);
        }
        return height;
    }

    private int mobSpawningRowHeight(MobSpawningRow row) {
        return row.namespace() != null ? MOB_SPAWNING_HEADER_HEIGHT : MOB_SPAWNING_ROW_HEIGHT;
    }

    private LivingEntity mobPreviewEntity(MobSpawningEntry mob) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        return this.mobPreviewEntities.computeIfAbsent(mob.id(), ignored -> {
            Entity entity = mob.type().create(this.minecraft.level);
            return entity instanceof LivingEntity livingEntity ? livingEntity : null;
        });
    }

    private int mobPreviewScale(LivingEntity entity) {
        float width = Math.max(0.1F, entity.getBbWidth());
        float height = Math.max(0.1F, entity.getBbHeight());
        int available = MOB_SPAWNING_TILE_SIZE - MOB_SPAWNING_MODEL_PADDING * 2;
        if (available <= 0) {
            return 24;
        }

        int scaleByHeight = Math.round(available * 0.9F / height);
        int scaleByWidth = Math.round(available * 0.58F / width);
        return Math.max(6, Math.min(28, Math.min(scaleByHeight, scaleByWidth)));
    }

    private List<FormattedCharSequence> createMobSpawningTooltip(MobSpawningEntry mob) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(mob.type().getDescriptionId()).withStyle(ChatFormatting.YELLOW).getVisualOrderText());
        tooltip.add(Component.literal(mob.id().toString()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
        return tooltip;
    }

    private String namespaceTitle(String namespace) {
        if ("minecraft".equals(namespace)) {
            return "Minecraft";
        }

        String[] words = namespace.replace('-', '_').split("_");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.isEmpty() ? namespace : title.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean booleanRuleValue(GameRules.Key<?> rule) {
        return this.displayedGameRules.getRule((GameRules.Key<GameRules.BooleanValue>)rule).get();
    }

    @SuppressWarnings("unchecked")
    private void setBooleanRuleValue(GameRules.Key<?> rule, boolean value) {
        this.displayedGameRules.getRule((GameRules.Key<GameRules.BooleanValue>)rule).set(value, null);
    }

    @SuppressWarnings("unchecked")
    private int integerRuleValue(GameRules.Key<?> rule) {
        return this.displayedGameRules.getRule((GameRules.Key<GameRules.IntegerValue>)rule).get();
    }

    private String integerDraft(GameRules.Key<?> rule) {
        return this.integerRuleDrafts.computeIfAbsent(rule.getId(), ignored -> Integer.toString(integerRuleValue(rule)));
    }

    private List<FormattedCharSequence> createGameRuleTooltip(GameRuleDefinition definition) {
        List<FormattedCharSequence> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(definition.rule().getId()).withStyle(ChatFormatting.YELLOW).getVisualOrderText());

        String descriptionKey = definition.rule().getDescriptionId() + ".description";
        if (I18n.exists(descriptionKey)) {
            this.font.split(Component.translatable(descriptionKey), 150).forEach(tooltip::add);
        }

        tooltip.add(Component.translatable("editGamerule.default", Component.literal(definition.defaultValue())).withStyle(ChatFormatting.GRAY).getVisualOrderText());
        return tooltip;
    }

    private void commitFocusedIntegerRule() {
        String value = integerDraft(this.focusedIntegerRule);
        try {
            int parsed = Integer.parseInt(value);
            setIntegerRuleValue(this.focusedIntegerRule, parsed);
            sendCommand("gamerule " + this.focusedIntegerRule.getId() + " " + parsed);
            playClickSound();
            this.focusedIntegerRule = null;
        } catch (NumberFormatException ignored) {
        }
    }

    private void commitFocusedPlayerXp() {
        if (this.focusedXpPlayer == null) {
            return;
        }

        visiblePlayerProfiles().stream()
                .filter(profile -> profile.getId().equals(this.focusedXpPlayer))
                .findFirst()
                .ifPresent(profile -> commitPlayerXp(profile.getName()));
    }

    private void commitPlayerXp(String playerName) {
        String value = this.xpDraft.isBlank() ? "0" : this.xpDraft;
        sendCommand("xp set " + playerName + " " + value + " levels");
        this.focusedXpPlayer = null;
    }

    private void requestGameRuleValues() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(GameRuleSync.GameRuleSyncRequest.INSTANCE);
        }
    }

    private void requestSpawnControlValues() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(GameRuleSync.SpawnControlSyncRequest.INSTANCE);
        }
    }

    private void requestPlayerProfiles() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(GameRuleSync.PlayerListSyncRequest.INSTANCE);
        }
    }

    @SuppressWarnings("unchecked")
    private void setIntegerRuleValue(GameRules.Key<?> rule, int value) {
        setIntegerRuleCurrentValue(rule, value);
        this.integerRuleDrafts.put(rule.getId(), Integer.toString(value));
    }

    @SuppressWarnings("unchecked")
    private void setIntegerRuleCurrentValue(GameRules.Key<?> rule, int value) {
        this.displayedGameRules.getRule((GameRules.Key<GameRules.IntegerValue>)rule).set(value, null);
    }

    private static List<GameRuleDefinition> createGameRuleDefinitions() {
        Map<GameRules.Category, List<GameRuleDefinition>> definitionsByCategory = new EnumMap<>(GameRules.Category.class);
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                addDefinition(key, GameRuleKind.BOOLEAN);
            }

            @Override
            public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                addDefinition(key, GameRuleKind.INTEGER);
            }

            private void addDefinition(GameRules.Key<?> key, GameRuleKind kind) {
                definitionsByCategory.computeIfAbsent(key.getCategory(), ignored -> new ArrayList<>())
                        .add(new GameRuleDefinition(key.getCategory(), key, kind, defaultValue(key)));
            }
        });

        List<GameRuleDefinition> definitions = new ArrayList<>();
        for (GameRules.Category category : GameRules.Category.values()) {
            definitionsByCategory.getOrDefault(category, List.of()).stream()
                    .sorted(Comparator.comparing(definition -> definition.rule().getId()))
                    .forEach(definitions::add);
        }
        return List.copyOf(definitions);
    }

    @SuppressWarnings("unchecked")
    private static <T extends GameRules.Value<T>> String defaultValue(GameRules.Key<?> key) {
        return new GameRules().getRule((GameRules.Key<T>)key).serialize();
    }

    private boolean contains(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseY >= top && mouseX < right && mouseY < bottom;
    }

    private void playClickSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void drawBorder(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    private void drawMinecraftButton(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.blitSprite(BUTTON_SPRITE, left, top, right - left, bottom - top);
    }

    private void drawDropdownIndicator(GuiGraphics guiGraphics, int x, int y) {
        ResourceLocation sprite = this.categoryDropdownOpen ? DROPDOWN_OPENED_SPRITE : DROPDOWN_CLOSED_SPRITE;
        guiGraphics.blitSprite(sprite, x, y, 20, 10);
    }

    private void rebuild() {
        this.clearWidgets();
        Layout layout = layout();

        int filterX = layout.filterLeft + 20;
        int filterWidth = layout.filterRight - layout.filterLeft - 40;
        addCategoryDropdown(filterX, layout.filterTop + 78, filterWidth);

        int searchY = this.categoryDropdownOpen ? layout.filterTop + 140 : layout.filterTop + 108;
        EditBox searchBox = new EditBox(this.font, filterX, searchY, filterWidth, BUTTON_HEIGHT, Component.translatable("menu.tweaked.search"));
        searchBox.setHint(Component.translatable("menu.tweaked.search"));
        searchBox.setMaxLength(64);
        searchBox.setValue(this.search);
        searchBox.setResponder(value -> {
            this.search = value;
            this.page = 0;
            this.searchFocused = true;
            rebuild();
        });
        this.addRenderableWidget(searchBox);

        if (this.categoryDropdownOpen) {
            addCategoryOptionButtons(filterX, layout.filterTop + 100, filterWidth);
        }

        int extraY = searchY + 28;
        addFilterExtras(filterX, extraY, filterWidth);

        if (this.searchFocused) {
            searchBox.setFocused(true);
            this.setFocused(searchBox);
        }

        switch (this.tab) {
            case GAMERULES -> buildGameRules(layout);
            case SPAWNING -> buildSpawning(layout);
            case PLAYERS -> buildPlayers(layout);
        }
    }

    private void addCategoryDropdown(int x, int y, int width) {
        Component label = Component.literal(Component.translatable(this.tab.translationKey).getString() + " v");
        this.addRenderableWidget(Button.builder(label, clicked -> {
            this.categoryDropdownOpen = !this.categoryDropdownOpen;
            this.searchFocused = false;
            rebuild();
        }).bounds(x, y, width, BUTTON_HEIGHT).build());
    }

    private void addCategoryOptionButtons(int x, int y, int width) {
        Tab[] tabs = Tab.values();
        for (int index = 0; index < tabs.length; index++) {
            Tab target = tabs[index];
            Button button = Button.builder(Component.translatable(target.translationKey), clicked -> {
                this.tab = target;
                this.page = 0;
                this.search = "";
                this.categoryDropdownOpen = false;
                this.searchFocused = false;
                rebuild();
            }).bounds(x, y + index * BUTTON_HEIGHT, width, BUTTON_HEIGHT).build();
            button.active = this.tab != target;
            this.addRenderableWidget(button);
        }
    }

    private void addFilterExtras(int x, int y, int width) {
        Button showEmpty = Button.builder(Component.literal(""), clicked -> {
        }).bounds(x, y, 20, 20).build();
        showEmpty.active = false;
        this.addRenderableWidget(showEmpty);
        addLabel(Component.translatable("menu.tweaked.show_empty"), x + 28, y + 6, 0xFFFFFF);

        addFilterSelect(Component.literal("*"), Component.translatable("menu.tweaked.default_filter"), x, y + 28, width);
        addFilterSelect(Component.literal("^"), Component.translatable("menu.tweaked.default_filter"), x, y + 52, width);
        addFilterSelect(Component.literal("o"), Component.literal("-"), x, y + 76, width);
        addFilterSelect(Component.literal("="), Component.literal("-"), x, y + 100, width);
    }

    private void addFilterSelect(Component icon, Component label, int x, int y, int width) {
        addLabel(icon, x + 3, y + 6, 0xFFFFFF);
        Button button = Button.builder(Component.literal(label.getString() + " v"), clicked -> {
        }).bounds(x + 28, y, width - 28, BUTTON_HEIGHT).build();
        button.active = false;
        this.addRenderableWidget(button);
    }

    private void buildGameRules(Layout layout) {
        List<String> rules = new ArrayList<>();
        rules.addAll(BOOLEAN_GAMERULES);
        rules.addAll(INTEGER_GAMERULES);
        rules = rules.stream()
                .filter(rule -> matchesSearch(rule, Component.translatable("gamerule." + rule).getString()))
                .toList();

        int pageSize = pageSize(layout);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, rules.size());

        addSectionHeader(Component.translatable(this.tab.translationKey), layout);
        for (int index = start; index < end; index++) {
            String rule = rules.get(index);
            int y = rowY(layout, index - start);
            addRowBackground(layout, y);
            addLabel(Component.translatable("gamerule." + rule), layout.statsLeft + 10, y + 6, 0xFFFFFF);

            if (BOOLEAN_GAMERULES.contains(rule)) {
                this.addRenderableWidget(Button.builder(Component.literal("true"), button -> sendCommand("gamerule " + rule + " true"))
                        .bounds(layout.contentRight - 144, y, 68, BUTTON_HEIGHT)
                        .build());
                this.addRenderableWidget(Button.builder(Component.literal("false"), button -> sendCommand("gamerule " + rule + " false"))
                        .bounds(layout.contentRight - 72, y, 68, BUTTON_HEIGHT)
                        .build());
            } else {
                EditBox value = new EditBox(this.font, layout.contentRight - 144, y, 68, BUTTON_HEIGHT, Component.translatable("menu.tweaked.value"));
                value.setValue("0");
                this.addRenderableWidget(value);
                this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.set"), button -> sendCommand("gamerule " + rule + " " + value.getValue()))
                        .bounds(layout.contentRight - 72, y, 68, BUTTON_HEIGHT)
                        .build());
            }
        }

        addPager(layout, rules.size(), pageSize);
    }

    private void buildSpawning(Layout layout) {
        List<String> entities = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> type != EntityType.PLAYER)
                .filter(type -> type.getCategory() != MobCategory.MISC)
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())
                .filter(entity -> matchesSearch(entity))
                .sorted()
                .toList();

        int pageSize = pageSize(layout);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, entities.size());

        addSectionHeader(Component.translatable(this.tab.translationKey), layout);
        for (int index = start; index < end; index++) {
            String entity = entities.get(index);
            int y = rowY(layout, index - start);
            addRowBackground(layout, y);
            addLabel(Component.literal(entity), layout.statsLeft + 10, y + 6, 0xFFFFFF);
            this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.disable"), button -> sendCommand("tweaked spawning set " + entity + " false"))
                    .bounds(layout.contentRight - 144, y, 68, BUTTON_HEIGHT)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.enable"), button -> sendCommand("tweaked spawning set " + entity + " true"))
                    .bounds(layout.contentRight - 72, y, 68, BUTTON_HEIGHT)
                    .build());
        }

        addPager(layout, entities.size(), pageSize);
    }

    private void buildPlayers(Layout layout) {
        List<String> players = onlinePlayers().stream()
                .filter(this::matchesSearch)
                .toList();
        int actionWidth = Math.min(208, layout.contentWidth() / 2);
        int listWidth = Math.max(120, layout.contentWidth() - actionWidth - 34);
        int actionX = layout.contentRight - actionWidth - 18;
        int pageSize = pageSize(layout);
        int start = page * pageSize;
        int end = Math.min(start + pageSize, players.size());

        addSectionHeader(Component.translatable(this.tab.translationKey), layout);
        for (int index = start; index < end; index++) {
            String player = players.get(index);
            int y = rowY(layout, index - start);
            addRowBackground(layout.statsLeft, actionX - 12, y);
            Button button = Button.builder(Component.literal(player), clicked -> {
                this.selectedPlayer = player;
                rebuild();
            }).bounds(layout.statsLeft + 10, y + 2, listWidth - 12, BUTTON_HEIGHT).build();
            button.active = !player.equals(this.selectedPlayer);
            this.addRenderableWidget(button);
        }

        addPager(layout, players.size(), pageSize);

        int actionY = layout.rowsTop;
        boolean hasPlayer = this.selectedPlayer != null;
        addPlayerAction(Component.translatable("menu.tweaked.teleport_to"), actionX, actionY, hasPlayer, () -> sendCommand("tp " + selfName() + " " + this.selectedPlayer));
        addPlayerAction(Component.translatable("menu.tweaked.teleport_here"), actionX + 104, actionY, hasPlayer, () -> sendCommand("tp " + this.selectedPlayer + " " + selfName()));
        addPlayerAction(Component.literal("Heal"), actionX, actionY + 24, hasPlayer, () -> sendCommand("heal " + this.selectedPlayer));
        addPlayerAction(Component.literal("Feed"), actionX + 104, actionY + 24, hasPlayer, () -> sendCommand("feed " + this.selectedPlayer));
        addPlayerAction(Component.literal("Fly"), actionX, actionY + 48, hasPlayer, () -> sendCommand("fly " + this.selectedPlayer));
        addPlayerAction(Component.literal("God"), actionX + 104, actionY + 48, hasPlayer, () -> sendCommand("god " + this.selectedPlayer));
        addPlayerAction(Component.literal("InvSee"), actionX, actionY + 72, hasPlayer, () -> sendCommand("invsee " + this.selectedPlayer));
        addPlayerAction(Component.literal("EnderSee"), actionX + 104, actionY + 72, hasPlayer, () -> sendCommand("endersee " + this.selectedPlayer));
        addPlayerAction(Component.translatable("menu.tweaked.clear_inventory"), actionX, actionY + 96, hasPlayer, this::confirmClearInventory);

        this.playerLevel = new EditBox(this.font, actionX, actionY + 124, 72, BUTTON_HEIGHT, Component.translatable("menu.tweaked.level"));
        this.playerLevel.setValue("0");
        this.playerLevel.active = hasPlayer;
        this.addRenderableWidget(this.playerLevel);
        Button levelButton = Button.builder(Component.translatable("menu.tweaked.set_level"), button -> sendCommand("experience set " + this.selectedPlayer + " " + this.playerLevel.getValue() + " levels"))
                .bounds(actionX + 76, actionY + 124, 132, BUTTON_HEIGHT)
                .build();
        levelButton.active = hasPlayer;
        this.addRenderableWidget(levelButton);

        if (!hasPlayer) {
            this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.select_player"), button -> {
            }).bounds(actionX, actionY + 154, 208, BUTTON_HEIGHT).build()).active = false;
        }
    }

    private void addPlayerAction(Component label, int x, int y, boolean active, Runnable action) {
        Button button = Button.builder(label, clicked -> action.run())
                .bounds(x, y, 100, BUTTON_HEIGHT)
                .build();
        button.active = active;
        this.addRenderableWidget(button);
    }

    private void addPager(Layout layout, int itemCount, int pageSize) {
        int maxPage = Math.max(0, (itemCount - 1) / pageSize);
        this.page = Math.min(this.page, maxPage);
        int y = layout.contentBottom - 28;
        Button previous = Button.builder(Component.literal("<"), button -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(layout.statsLeft, y, 40, BUTTON_HEIGHT).build();
        previous.active = this.page > 0;
        this.addRenderableWidget(previous);

        this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.page", this.page + 1, maxPage + 1), button -> {
        }).bounds(layout.statsLeft + 44, y, 96, BUTTON_HEIGHT).build()).active = false;

        Button next = Button.builder(Component.literal(">"), button -> {
            this.page = Math.min(maxPage, this.page + 1);
            rebuild();
        }).bounds(layout.statsLeft + 144, y, 40, BUTTON_HEIGHT).build();
        next.active = this.page < maxPage;
        this.addRenderableWidget(next);

        this.addRenderableWidget(Button.builder(Component.translatable("menu.tweaked.results", itemCount), button -> {
        }).bounds(layout.statsLeft + 190, y, Math.min(128, layout.contentRight - layout.statsLeft - 190), BUTTON_HEIGHT).build()).active = false;
    }

    private void renderSummary(GuiGraphics guiGraphics, Layout layout) {
        int x = layout.contentLeft + 170;
        int y = layout.contentTop + 38;
        guiGraphics.drawString(this.font, Component.translatable("menu.tweaked.summary.category"), x, y, 0xFFFF00, false);
        guiGraphics.drawString(this.font, Component.translatable(this.tab.translationKey), x, y + 11, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("menu.tweaked.summary.search"), x, y + 38, 0xFFFF00, false);
        guiGraphics.drawString(this.font, this.search.isBlank() ? Component.literal("-") : Component.literal(this.search), x, y + 49, 0xFFFFFF, false);

        int iconLeft = layout.contentLeft + 70;
        int iconTop = layout.contentTop + 44;
        guiGraphics.fill(iconLeft, iconTop, iconLeft + 52, iconTop + 52, 0x404A638F);
        guiGraphics.fill(iconLeft + 16, iconTop + 8, iconLeft + 36, iconTop + 28, 0xFFFFFF00);
        guiGraphics.fill(iconLeft + 12, iconTop + 28, iconLeft + 40, iconTop + 44, 0xFFEFEFEF);
    }

    private void addSectionHeader(Component label, Layout layout) {
        this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> guiGraphics.drawString(this.font, label, layout.statsLeft, layout.rowsTop - 17, 0xFFFF00, false));
    }

    private void addLabel(Component label, int x, int y, int color) {
        this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> guiGraphics.drawString(this.font, label, x, y, color, false));
    }

    private void addRowBackground(Layout layout, int y) {
        addRowBackground(layout.statsLeft, layout.contentRight - 18, y);
    }

    private void addRowBackground(int left, int right, int y) {
        this.addRenderableOnly((guiGraphics, mouseX, mouseY, partialTick) -> {
            int rowIndex = (y / Math.max(1, ROW_HEIGHT)) & 1;
            guiGraphics.fill(left, y, right, y + ROW_HEIGHT, rowIndex == 0 ? 0x603A4F78 : 0x4010201A);
        });
    }

    private boolean matchesSearch(String value) {
        return matchesSearch(value, "");
    }

    private boolean matchesSearch(String value, String label) {
        String needle = this.search.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        return value.toLowerCase(Locale.ROOT).contains(needle) || label.toLowerCase(Locale.ROOT).contains(needle);
    }

    private int rowY(Layout layout, int row) {
        return layout.rowsTop + row * (ROW_HEIGHT + ROW_GAP);
    }

    private int pageSize(Layout layout) {
        return Math.max(4, (layout.contentBottom - layout.rowsTop - 42) / (ROW_HEIGHT + ROW_GAP));
    }

    private List<String> onlinePlayers() {
        if (this.minecraft == null || this.minecraft.getConnection() == null) {
            return List.of();
        }

        return this.minecraft.getConnection().getOnlinePlayers().stream()
                .map(PlayerInfo::getProfile)
                .map(GameProfile::getName)
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    private List<GameProfile> visiblePlayerProfiles() {
        Set<UUID> onlinePlayerIds = onlinePlayerIds();
        return this.playerProfiles.stream()
                .filter(profile -> matchesSearch(profile.getName(), profile.getId().toString()))
                .sorted(Comparator
                        .comparing((GameProfile profile) -> !onlinePlayerIds.contains(profile.getId()))
                        .thenComparing(profile -> profile.getName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private Set<UUID> onlinePlayerIds() {
        if (this.minecraft == null || this.minecraft.getConnection() == null) {
            return Set.of();
        }

        return this.minecraft.getConnection().getOnlinePlayers().stream()
                .map(PlayerInfo::getProfile)
                .map(GameProfile::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private LivingEntity playerEntity(GameProfile profile) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        return this.playerPreviewEntities.computeIfAbsent(profile.getId(), ignored -> new OfflinePlayerPreview(this.minecraft.level, profile));
    }

    private static final class OfflinePlayerPreview extends RemotePlayer {
        private final Supplier<PlayerSkin> skin;

        private OfflinePlayerPreview(net.minecraft.client.multiplayer.ClientLevel level, GameProfile profile) {
            super(level, profile);
            this.skin = net.minecraft.client.Minecraft.getInstance().getSkinManager().lookupInsecure(profile);
        }

        @Override
        public PlayerSkin getSkin() {
            return this.skin.get();
        }

        @Override
        public boolean shouldShowName() {
            return false;
        }

        @Override
        public boolean isInvisibleTo(net.minecraft.world.entity.player.Player player) {
            return true;
        }
    }

    private String selfName() {
        return this.minecraft.player.getGameProfile().getName();
    }

    private void confirmClearInventory() {
        this.minecraft.setScreen(new ConfirmScreen(accepted -> {
            if (accepted) {
                sendCommand("clear " + this.selectedPlayer);
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("menu.tweaked.clear_inventory"), Component.translatable("menu.tweaked.clear_inventory.confirm", this.selectedPlayer)));
    }

    private void confirmPlayerCommand(String playerName, String command) {
        this.minecraft.setScreen(new ConfirmScreen(accepted -> {
            if (accepted) {
                sendCommand(command);
            }
            this.minecraft.setScreen(this);
        }, Component.literal("Submit"), Component.literal(command + " ?")));
    }

    private void sendCommand(String command) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundChatCommandPacket(command));
        }
    }

    private Layout layout() {
        int panelWidth = Math.max(500, Math.min(this.width - 32, this.width - (this.width / 9)));
        int panelHeight = Math.max(240, this.height - 40);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int bottom = top + panelHeight;
        int filterLeft = left;
        int filterTop = top;
        int filterRight = left + panelWidth / 3 - OUTER_GAP;
        int filterBottom = bottom;
        int contentLeft = filterRight + OUTER_GAP;
        int contentRight = left + panelWidth;
        int contentTop = top;
        int contentBottom = bottom;
        int statsLeft = contentLeft + 18;
        int rowsTop = contentTop + 180;
        return new Layout(left, top, left + panelWidth, bottom, filterLeft, filterTop, filterRight, filterBottom, contentLeft, contentTop, contentRight, contentBottom, statsLeft, rowsTop);
    }

    private void fillPanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, bottom, color);
        guiGraphics.fill(left, top, right, top + 2, 0xFF000000);
        guiGraphics.fill(left, bottom - 2, right, bottom, 0xFF000000);
        guiGraphics.fill(left, top, left + 2, bottom, 0xFF000000);
        guiGraphics.fill(right - 2, top, right, bottom, 0xFF000000);
        guiGraphics.fill(left + 2, top + 2, right - 2, top + 3, 0x805A6F99);
        guiGraphics.fill(left + 2, top + 2, left + 3, bottom - 2, 0x805A6F99);
    }

    private enum Tab {
        GAMERULES("menu.tweaked.gamerules", "menu.tweaked.gamerules.description"),
        SPAWNING("menu.tweaked.spawning", "menu.tweaked.spawning.description"),
        PLAYERS("menu.tweaked.players", "menu.tweaked.players.description");

        private final String translationKey;
        private final String descriptionKey;

        Tab(String translationKey, String descriptionKey) {
            this.translationKey = translationKey;
            this.descriptionKey = descriptionKey;
        }
    }

    private enum GameRuleKind {
        BOOLEAN,
        INTEGER
    }

    private record GameRuleDefinition(GameRules.Category category, GameRules.Key<?> rule, GameRuleKind kind, String defaultValue) {
    }

    private record GameRuleMenuRow(String categoryKey, GameRuleDefinition definition, GameRules.Key<?> rule) {
    }

    private record MobSpawningEntry(ResourceLocation id, EntityType<?> type) {
    }

    private record MobSpawningRow(String namespace, List<MobSpawningEntry> mobs) {
    }

    private record GameRuleScrollbar(
            int left,
            int top,
            int right,
            int bottom,
            int scrollerTop,
            int scrollerHeight,
            int maxScroll
    ) {
    }

    private record Layout(
            int left,
            int top,
            int right,
            int bottom,
            int filterLeft,
            int filterTop,
            int filterRight,
            int filterBottom,
            int contentLeft,
            int contentTop,
            int contentRight,
            int contentBottom,
            int statsLeft,
            int rowsTop
    ) {
        int contentWidth() {
            return contentRight - statsLeft - 18;
        }
    }
}
