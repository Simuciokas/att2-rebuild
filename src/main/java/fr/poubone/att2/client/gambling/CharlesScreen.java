package fr.poubone.att2.client.gambling;

import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.gambling.widget.ListEntryButton;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.shop.ShopAction;
import fr.poubone.att2.client.shop.ShopActionButton;
import fr.poubone.att2.client.shop.ShopHover;
import fr.poubone.att2.client.shop.ShopPaging;
import fr.poubone.att2.client.shop.ShopPanelScreen;
import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopType;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;

public class CharlesScreen extends ShopPanelScreen {
    private static final SoundEvent BOOK_OPEN = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final int BG_WIDTH = 760;
    private static final int BG_HEIGHT = 464;
    private static final int ROW_HEIGHT = 30;
    private static final int RIGHT_X = 198;
    private static final int RIGHT_WIDTH = 510;
    private static final int GRID_FOOTER = 56;
    private static CharlesScreen.Tab lastTab = CharlesScreen.Tab.BETS;
    private int offsetX;
    private int offsetY;
    private CharlesScreen.Tab tab;
    private final List<QuestBookWidget> tabWidgets = new ArrayList<>();
    private QuestBookWidget hovered;
    private int gridLeft;
    private int gridTop;
    private int cellSize;
    private int lastRenderedState = -1;
    private int page;
    private int pages = 1;
    private ShopActionButton previousPage;
    private ShopActionButton nextPage;
    private ShopActionButton resetBoard;
    private ShopActionButton backToList;

    private CharlesScreen(CharlesScreen.Tab tab) {
        super(ModLanguageManager.get("charles.title"), 760, 464);
        this.tab = tab;
    }

    public static void open(CharlesScreen.Tab tab) {
        if (HUDConfig.get().charlesAutoOpen) {
            Minecraft client = Minecraft.getInstance();
            boolean alreadyOpen = client.screen instanceof CharlesScreen;
            if (alreadyOpen) {
                ((CharlesScreen)client.screen).switchTab(tab, false);
            } else {
                client.setScreen(new CharlesScreen(tab));
                client.getSoundManager().play(SimpleSoundInstance.forUI(BOOK_OPEN, 1.0F));
            }
        }
    }

    public static void toggle() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof CharlesScreen) {
            client.setScreen(null);
        } else if (client.screen == null) {
            open(lastTab);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.initViewport();
        this.offsetX = 0;
        this.offsetY = 0;
        this.chrome(714, 25, 22, 22, Component.translatable("gui.close"), ShopActionButton.Chrome.CLOSE, ShopType.CHARLES, this::onClose);
        this.chrome(520, 70, 80, 20, ModLanguageManager.get("charles.reload"), ShopActionButton.Chrome.ACTION, ShopType.CHARLES, this::requestCurrentTab);

        for (CharlesScreen.Tab value : CharlesScreen.Tab.values()) {
            this.addRenderableWidget(
                new ShopActionButton(
                    24,
                    92 + value.ordinal() * 34,
                    132,
                    27,
                    new ShopAction(
                        -1, ModLanguageManager.get(value == CharlesScreen.Tab.BETS ? "charles.tab.bets" : "charles.tab.grids"), null, ShopAction.Kind.OTHER
                    ),
                    ShopType.CHARLES.titleColor,
                    -6469,
                    () -> this.switchTab(value, true),
                    ShopActionButton.Chrome.TAB,
                    () -> this.tab == value,
                    ShopType.CHARLES
                )
            );
        }

        this.switchTab(this.tab, false);
        if (this.needsData()) {
            this.requestCurrentTab();
        }
    }

    private boolean needsData() {
        GamblingModel model = GamblingModel.get();
        return this.tab == CharlesScreen.Tab.BETS ? model.getBets().isEmpty() : !model.hasGrid() && model.getGridChoices().isEmpty();
    }

    private void requestCurrentTab() {
        GamblingModel model = GamblingModel.get();
        if (this.tab == CharlesScreen.Tab.BETS) {
            model.requestBets();
        } else {
            model.requestGridMenu();
        }
    }

    private void switchTab(CharlesScreen.Tab newTab, boolean requestIfEmpty) {
        if (this.tab != newTab) {
            this.page = 0;
        }

        this.tab = newTab;
        lastTab = newTab;
        GamblingModel.get().clearNotEnoughChronotons();
        this.rebuildTabWidgets();
        if (requestIfEmpty && this.needsData()) {
            this.requestCurrentTab();
        }
    }

    private void rebuildTabWidgets() {
        for (QuestBookWidget widget : this.tabWidgets) {
            this.removeWidget(widget);
        }

        this.tabWidgets.clear();
        if (this.previousPage != null) {
            this.removeWidget(this.previousPage);
        }

        if (this.nextPage != null) {
            this.removeWidget(this.nextPage);
        }

        if (this.resetBoard != null) {
            this.removeWidget(this.resetBoard);
            this.resetBoard = null;
        }

        if (this.backToList != null) {
            this.removeWidget(this.backToList);
            this.backToList = null;
        }

        GamblingModel model = GamblingModel.get();
        int x = this.offsetX + 198;
        int y = this.offsetY + 104;
        if (this.tab == CharlesScreen.Tab.BETS) {
            this.buildPageNavigation(model.getBets().size());

            for (GamblingModel.BetEntry bet : model.getBets().subList(this.page * 8, Math.min(model.getBets().size(), this.page * 8 + 8))) {
                List<Component> tooltip = new ArrayList<>();
                if (bet.locked()) {
                    tooltip.add(Component.translatable("att2.lock"));
                } else {
                    tooltip.add(bet.label().copy().withStyle(ChatFormatting.BOLD));
                    tooltip.add(Component.literal(ModLanguageManager.format("charles.price", "price", bet.price())).withStyle(ChatFormatting.YELLOW));
                    tooltip.add(ModLanguageManager.get("charles.bet.click").withStyle(ChatFormatting.GREEN));
                }

                Component value = bet.locked() ? null : Component.literal(bet.price() + " §6●");
                this.addTabWidget(
                    new ListEntryButton(x, y, 510, 28, bet.label(), value, tooltip, () -> !bet.locked() && !model.isBetPending(), () -> model.placeBet(bet))
                );
                y += 30;
            }
        } else if (model.hasGrid()) {
            this.pages = 1;
            this.page = 0;
            this.layoutGrid();
            int buttonsY = 408;
            int half = 249;
            this.resetBoard = this.chrome(
                198, buttonsY, half, 28, Component.translatable("matching_game.reset"), ShopActionButton.Chrome.ACTION, ShopType.CHARLES, model::resetGrid
            );
            this.backToList = this.chrome(
                198 + half + 12,
                buttonsY,
                half,
                28,
                Component.translatable("matching_game.back_menu"),
                ShopActionButton.Chrome.ACTION,
                ShopType.CHARLES,
                model::requestGridMenu
            );
        } else {
            this.buildPageNavigation(model.getGridChoices().size());

            for (GamblingModel.GridChoice choice : model.getGridChoices().subList(this.page * 8, Math.min(model.getGridChoices().size(), this.page * 8 + 8))) {
                List<Component> tooltip = new ArrayList<>();
                if (choice.locked()) {
                    tooltip.add(Component.translatable("matching_game.select.lock.hover_event"));
                } else {
                    tooltip.add(choice.label().copy().withStyle(ChatFormatting.BOLD));
                    if (choice.price() >= 0) {
                        tooltip.add(Component.literal(ModLanguageManager.format("charles.price", "price", choice.price())).withStyle(ChatFormatting.YELLOW));
                    }

                    tooltip.add(
                        ModLanguageManager.get(choice.isContinue() ? "charles.grid.continue.tip" : "charles.grid.buy.tip").withStyle(ChatFormatting.GREEN)
                    );
                }

                Component value = !choice.locked() && choice.price() >= 0 ? Component.literal(choice.price() + " §6●") : null;
                this.addTabWidget(new ListEntryButton(x, y, 510, 28, choice.label(), value, tooltip, () -> !choice.locked(), () -> model.chooseGrid(choice)));
                y += 30;
            }
        }
    }

    private void buildPageNavigation(int count) {
        this.pages = Math.max(1, (count + 7) / 8);
        this.page = Math.max(0, Math.min(this.page, this.pages - 1));
        this.previousPage = this.chrome(618, 70, 20, 20, Component.literal("‹"), ShopActionButton.Chrome.NAV, ShopType.CHARLES, () -> {
            this.page--;
            this.rebuildTabWidgets();
        });
        this.nextPage = this.chrome(690, 70, 20, 20, Component.literal("›"), ShopActionButton.Chrome.NAV, ShopType.CHARLES, () -> {
            this.page++;
            this.rebuildTabWidgets();
        });
        this.previousPage.active = this.page > 0;
        this.nextPage.active = this.page + 1 < this.pages;
    }

    private void addTabWidget(QuestBookWidget widget) {
        this.tabWidgets.add(widget);
        this.addRenderableWidget(widget);
    }

    private void layoutGrid() {
        List<List<GamblingModel.GridCell>> grid = GamblingModel.get().getGrid();
        int rows = Math.max(1, grid.size());
        int cols = grid.stream().mapToInt(List::size).max().orElse(1);
        int availableWidth = 510;
        int counterRows = Math.max(1, (GamblingModel.get().getCounts().size() + 13) / 14);
        int availableHeight = 304 - counterRows * 14;
        this.cellSize = Math.max(1, Math.min(42, Math.min(availableWidth / cols, availableHeight / rows)));
        this.gridLeft = this.offsetX + 198 + (availableWidth - cols * this.cellSize) / 2;
        this.gridTop = this.offsetY + 104;
    }

    @Override
    public void tick() {
        super.tick();
        GamblingModel model = GamblingModel.get();
        int state = model.getBets().size() * 31
            + model.getGridChoices().size() * 7
            + (model.hasGrid() ? 1 : 0)
            + model.getGrid().stream().flatMap(Collection::stream).mapToInt(c -> c.type().ordinal() + 1).sum() * 131
            + (model.isBetPending() ? 1000003 : 0);
        if (state != this.lastRenderedState) {
            this.lastRenderedState = state;
            this.rebuildTabWidgets();
        }
    }

    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.panel(graphics, ShopType.CHARLES);
        ShopTheme.text(graphics, ModLanguageManager.get("charles.title"), 62, 27, 410, 1.7F, -990260, false);
        ShopSkin.texture(graphics, ShopType.CHARLES, "wallet", 574, 19, 124, 37, 232, 70);
        String balance = ScoreCache.get("CHRONOTON").isPresent() ? String.valueOf(ScoreCache.getOrDefault("CHRONOTON", 0)) : "?";
        ShopTheme.text(graphics, Component.literal(ModLanguageManager.format("charles.balance", "n", balance)), 584, 32, 104, 0.85F, -1521558, false);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        this.hovered = null;

        for (GuiEventListener child : this.children()) {
            if (child instanceof QuestBookWidget widget && widget.isMouseOver(mouseX, mouseY)) {
                this.hovered = widget;
            }
        }

        for (GuiEventListener child : this.children()) {
            if (child instanceof ShopActionButton action && ShopHover.showsTooltip(action.isHovered(), action.isFocused())) {
                this.showTooltip(graphics, action.tooltipLines(), mouseX, mouseY);
            }
        }

        if (!GamblingModel.get().hasGrid() || this.tab == CharlesScreen.Tab.BETS) {
            ShopTheme.text(graphics, Component.literal(this.page + 1 + " / " + this.pages), 644, 74, 40, 1.0F, -13096159, true);
        }

        this.renderLeftPage(graphics);
        if (this.tab == CharlesScreen.Tab.BETS) {
            this.renderBetsPage(graphics);
        } else {
            this.renderGridsPage(graphics, mouseX, mouseY);
        }

        if (this.hovered != null && !this.hovered.getTooltipLines().isEmpty()) {
            this.showTooltip(graphics, this.hovered.getTooltipLines(), mouseX, mouseY);
        }
    }

    private void renderLeftPage(GuiGraphics graphics) {
        HUDConfig config = HUDConfig.get();
        int textColor = -13096159;
        int secondary = -9413049;
        GamblingModel model = GamblingModel.get();
        int x = this.offsetX + 24;
        int width = 132;
        int y = this.offsetY + 170;
        int bottom = this.offsetY + 400;
        String balance = ScoreCache.get("CHRONOTON").isPresent() ? String.valueOf(ScoreCache.getOrDefault("CHRONOTON", 0)) : "?";
        y = this.drawWrapped(graphics, Component.literal(ModLanguageManager.format("charles.balance", "n", balance)), x, y, width, textColor, bottom) + 3;
        if (model.isNotEnoughChronotons()) {
            y = this.drawWrapped(graphics, ModLanguageManager.get("charles.not_enough").withStyle(ChatFormatting.RED), x, y, width, textColor, bottom) + 2;
        }

        if (this.tab == CharlesScreen.Tab.GRIDS && !model.isNearTable()) {
            String distance = String.valueOf((int)model.distanceToTable());
            y = this.drawWrapped(
                    graphics,
                    Component.literal(ModLanguageManager.format("charles.too_far", "d", distance)).withStyle(ChatFormatting.RED),
                    x,
                    y,
                    width,
                    textColor,
                    bottom
                )
                + 2;
        }

        if (model.isWaiting()) {
            y = this.drawWrapped(graphics, ModLanguageManager.get("quest_book.loading"), x, y, width, secondary, bottom) + 2;
        }

        if (this.tab == CharlesScreen.Tab.BETS) {
            if (model.isBetPending()) {
                y = this.drawWrapped(graphics, ModLanguageManager.get("charles.bet.pending"), x, y, width, secondary, bottom) + 2;
            }

            if (model.getLastBetScore() >= 0 && System.currentTimeMillis() - model.getLastBetScoreTime() < 120000L) {
                y = this.drawWrapped(graphics, Component.translatable("att2.gambling.score", model.getLastBetScore()), x, y, width, textColor, bottom) + 2;
            }

            this.drawWrapped(graphics, ModLanguageManager.get("charles.bets.help"), x, y, width, secondary, bottom);
        } else {
            if (model.hasGrid()) {
                if (model.getNextFlipPrice() >= 0) {
                    ChatFormatting color = model.isNextFlipAffordable() ? ChatFormatting.GREEN : ChatFormatting.RED;
                    y = this.drawWrapped(
                            graphics,
                            Component.literal(ModLanguageManager.format("charles.grid.next_price", "price", model.getNextFlipPrice())).withStyle(color),
                            x,
                            y,
                            width,
                            textColor,
                            bottom
                        )
                        + 2;
                }

                if (model.isGridComplete()) {
                    y = this.drawWrapped(
                            graphics, ModLanguageManager.get("charles.grid.complete").withStyle(ChatFormatting.GREEN), x, y, width, textColor, bottom
                        )
                        + 2;
                }
            }

            this.drawWrapped(graphics, ModLanguageManager.get("charles.grids.help"), x, y, width, secondary, bottom);
        }
    }

    private void renderBetsPage(GuiGraphics graphics) {
        GamblingModel model = GamblingModel.get();
        if (model.getBets().isEmpty()) {
            Component hint = model.isWaiting() ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("charles.bets.empty");
            this.drawWrapped(
                graphics,
                hint,
                this.offsetX + 198,
                this.offsetY + 90,
                510,
                HUDConfig.color(HUDConfig.get().questBookSecondaryTextColor, -12566464),
                this.offsetY + 464
            );
        }
    }

    private void renderGridsPage(GuiGraphics graphics, int mouseX, int mouseY) {
        GamblingModel model = GamblingModel.get();
        int secondary = -9413049;
        if (!model.hasGrid()) {
            if (model.getGridChoices().isEmpty()) {
                Component hint = model.isWaiting() ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("charles.grids.empty");
                this.drawWrapped(graphics, hint, this.offsetX + 198, this.offsetY + 90, 510, secondary, this.offsetY + 464);
            }
        } else {
            List<List<GamblingModel.GridCell>> grid = model.getGrid();
            GamblingModel.GridCell hoveredCell = null;

            for (int row = 0; row < grid.size(); row++) {
                List<GamblingModel.GridCell> cells = grid.get(row);

                for (int col = 0; col < cells.size(); col++) {
                    GamblingModel.GridCell cell = cells.get(col);
                    int x = this.gridLeft + col * this.cellSize;
                    int y = this.gridTop + row * this.cellSize;
                    boolean over = mouseX >= x && mouseX < x + this.cellSize && mouseY >= y && mouseY < y + this.cellSize;
                    ShopSkin.texture(
                        graphics,
                        ShopType.CHARLES,
                        over && cell.isLocked() ? "card_hover" : "card_normal",
                        x + 1,
                        y + 1,
                        Math.max(1, this.cellSize - 2),
                        Math.max(1, this.cellSize - 2),
                        312,
                        252
                    );
                    if (over && cell.isLocked()) {
                        hoveredCell = cell;
                    }

                    int inset = Math.max(0, this.cellSize / 6);
                    int iconSize = this.cellSize - 2 * inset;
                    int textureSize = cell.type().textureSize;
                    graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        cell.type().texture,
                        x + inset,
                        y + inset,
                        0.0F,
                        0.0F,
                        iconSize,
                        iconSize,
                        textureSize,
                        textureSize,
                        textureSize,
                        textureSize
                    );
                }
            }

            int y = this.gridTop + grid.size() * this.cellSize + 4;
            int x = this.offsetX + 198;

            for (GamblingModel.CountEntry entry : model.getCounts()) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    entry.type().texture,
                    x,
                    y - 1,
                    0.0F,
                    0.0F,
                    10,
                    10,
                    entry.type().textureSize,
                    entry.type().textureSize,
                    entry.type().textureSize,
                    entry.type().textureSize
                );
                String count = String.valueOf(entry.count());
                int color = entry.color() == null ? -13096159 : 0xFF000000 | entry.color().getValue();
                graphics.drawString(this.font, count, x + 11, y, color, false);
                x += 36;
                if (x > this.offsetX + 198 + 510 - 20) {
                    x = this.offsetX + 198;
                    y += 14;
                }
            }

            if (hoveredCell != null && this.hovered == null) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("matching_game.hover_event"));
                if (model.getNextFlipPrice() >= 0) {
                    lines.add(
                        Component.literal(ModLanguageManager.format("charles.price", "price", model.getNextFlipPrice()))
                            .withStyle(model.isNextFlipAffordable() ? ChatFormatting.YELLOW : ChatFormatting.RED)
                    );
                }

                if (!model.isNearTable()) {
                    lines.add(ModLanguageManager.get("charles.too_far.short").withStyle(ChatFormatting.RED));
                }

                this.showTooltip(graphics, lines, mouseX, mouseY);
            }
        }
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int defaultColor, int bottom) {
        for (FormattedCharSequence line : this.font.split(text, width)) {
            if (y + 9 > bottom) {
                break;
            }

            graphics.drawString(this.font, line, x, y, defaultColor, false);
            y += 9;
        }

        return y;
    }

    @Override
    protected boolean clickContent(MouseButtonEvent event, boolean doubleClick) {
        if (super.clickContent(event, doubleClick)) {
            return true;
        }

        if (this.tab == CharlesScreen.Tab.GRIDS && event.button() == 0) {
            if (event.y() >= 408.0) {
                return false;
            }

            GamblingModel model = GamblingModel.get();
            List<List<GamblingModel.GridCell>> grid = model.getGrid();

            for (int row = 0; row < grid.size(); row++) {
                List<GamblingModel.GridCell> cells = grid.get(row);

                for (int col = 0; col < cells.size(); col++) {
                    int x = this.gridLeft + col * this.cellSize;
                    int y = this.gridTop + row * this.cellSize;
                    if (event.x() >= x && event.x() < x + this.cellSize && event.y() >= y && event.y() < y + this.cellSize) {
                        GamblingModel.GridCell cell = cells.get(col);
                        if (cell.isLocked()) {
                            model.flip(cell);
                            return true;
                        }

                        return false;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (GamblingModel.get().hasGrid()) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        } else {
            int next = ShopPaging.afterScroll(this.page, this.pages, deltaY);
            if (next != this.page) {
                this.page = next;
                this.rebuildTabWidgets();
                return true;
            } else {
                return deltaY != 0.0 ? true : super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    public enum Tab {
        BETS,
        GRIDS;
    }
}
