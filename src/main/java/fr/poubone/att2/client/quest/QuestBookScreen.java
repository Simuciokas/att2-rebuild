package fr.poubone.att2.client.quest;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.widget.JournalButton;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.quest.widget.QuestEntryButton;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class QuestBookScreen extends Screen {
    private static final SoundEvent BOOK_OPEN = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final SoundEvent BOOK_TURN_PAGE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.turn_page"));
    private QuestBookLayout layout;
    private int currentPage = 0;
    private int maxPage = 0;
    private final List<QuestInfo> elements = new ArrayList<>();
    private final List<QuestEntryButton> entryButtons = new ArrayList<>();
    private EditBox searchBox;
    private String searchText = "";
    private String selectedKey;
    private final List<Component> guideTooltip = new ArrayList<>();
    private QuestBookWidget hovered;
    private int leftScroll;
    private int leftContentHeight;
    private boolean draggingLeftScroll;
    private static final int SCROLLBAR_WIDTH = 5;

    private QuestBookScreen() {
        super(ModLanguageManager.get("quest_book.title"));
        this.selectedKey = QuestModel.get().getSelectedKey();
    }

    public static void open() {
        if (!HUDConfig.get().questMenuEnabled) {
            Att2Triggers.send(2315);
        } else {
            Minecraft client = Minecraft.getInstance();
            QuestModel.get().requestSideQuests();
            if (HUDConfig.get().questBookShowDaily) {
                QuestModel.get().requestDailyQuests();
            }

            client.setScreen(new QuestBookScreen());
            client.getSoundManager().play(SimpleSoundInstance.forUI(BOOK_OPEN, 1.0F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.layout = QuestBookLayout.fit(this.width, this.height);
        this.entryButtons.clear();
        this.draggingLeftScroll = false;
        this.searchBox = new EditBox(
            this.font, this.layout.rightX() + 7, this.layout.searchY() + 6, this.layout.contentWidth() - 14, 12, ModLanguageManager.get("quest_book.search")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setTextColor(-13096159);
        this.searchBox.setTextColorUneditable(-9413049);
        this.searchBox.setHint(ModLanguageManager.get("quest_book.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setValue(this.searchText);
        this.searchBox.setResponder(text -> {
            this.searchText = text;
            this.currentPage = 0;
            this.reloadElements();
        });
        this.addRenderableWidget(this.searchBox);
        HUDConfig config = HUDConfig.get();
        this.addControl(
            this.layout.x() + this.layout.width() - 30,
            this.layout.y() + 9,
            22,
            22,
            "quest_book.close",
            ItemStack.EMPTY,
            "close",
            () -> true,
            () -> true,
            e -> this.onClose()
        );
        this.addControl(
            this.layout.x() + this.layout.width() - 56,
            this.layout.y() + 9,
            22,
            22,
            "quest_book.reload",
            new ItemStack(Items.COMPASS),
            null,
            () -> false,
            () -> true,
            e -> {
                QuestModel.get().requestSideQuests();
                if (config.questBookShowDaily) {
                    QuestModel.get().requestDailyQuests();
                }
            }
        );
        Component guideLabel = ModLanguageManager.get("quest_book.guide");
        int guideWidth = this.font.width(guideLabel) + 16;
        this.refreshGuideTooltip();
        this.addRenderableWidget(
            new JournalButton(
                this.layout.x() + this.layout.width() - 60 - guideWidth,
                this.layout.y() + 9,
                guideWidth,
                22,
                guideLabel,
                ItemStack.EMPTY,
                null,
                this.guideTooltip,
                () -> false,
                this::hasGuidePage,
                e -> this.openSelectedGuide()
            )
        );
        this.addFilter(
            0, new ItemStack(Items.WRITTEN_BOOK), "main", QuestBookScreen.QuestCategory.MAIN, () -> config.questBookShowMain, v -> config.questBookShowMain = v
        );
        this.addFilter(
            1, new ItemStack(Items.PAPER), "side", QuestBookScreen.QuestCategory.SIDE, () -> config.questBookShowSide, v -> config.questBookShowSide = v
        );
        this.addFilter(
            2, new ItemStack(Items.CLOCK), "daily", QuestBookScreen.QuestCategory.DAILY, () -> config.questBookShowDaily, v -> config.questBookShowDaily = v
        );
        this.addFilter(3, new ItemStack(Items.EMERALD), "completed", null, () -> config.questBookShowCompleted, v -> config.questBookShowCompleted = v);
        this.addControl(
            this.layout.rightX(),
            this.layout.footerY(),
            24,
            20,
            "quest_book.previous",
            ItemStack.EMPTY,
            "previous",
            () -> false,
            () -> this.currentPage > 0,
            e -> this.setCurrentPage(this.currentPage - 1)
        );
        this.addControl(
            this.layout.rightX() + this.layout.contentWidth() - 24,
            this.layout.footerY(),
            24,
            20,
            "quest_book.next",
            ItemStack.EMPTY,
            "next",
            () -> false,
            () -> this.currentPage < this.maxPage,
            e -> this.setCurrentPage(this.currentPage + 1)
        );
        this.reloadElements();
    }

    private void addControl(
        int x,
        int y,
        int w,
        int h,
        String key,
        ItemStack item,
        String icon,
        BooleanSupplier selected,
        BooleanSupplier available,
        Consumer<MouseButtonEvent> press
    ) {
        this.addRenderableWidget(
            new JournalButton(x, y, w, h, ModLanguageManager.get(key), item, icon, List.of(ModLanguageManager.get(key)), selected, available, press)
        );
    }

    private void addFilter(int index, ItemStack item, String key, QuestBookScreen.QuestCategory category, BooleanSupplier getter, Consumer<Boolean> setter) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ModLanguageManager.get("quest_book.filter." + key));
        tooltip.add(ModLanguageManager.get("quest_book.filter.toggle").withStyle(ChatFormatting.GRAY));
        if (category != null) {
            tooltip.add(ModLanguageManager.get("quest_book.filter.only").withStyle(ChatFormatting.GRAY));
        }

        if (category == QuestBookScreen.QuestCategory.DAILY) {
            tooltip.add(ModLanguageManager.get("quest_book.daily.description"));
        }

        int columns = this.layout.filterColumns();
        int x = this.layout.rightX() + index % columns * (this.layout.filterWidth() + 4);
        int y = this.layout.filterY() + index / columns * 22;
        this.addRenderableWidget(
            new JournalButton(
                x, y, this.layout.filterWidth(), 20, ModLanguageManager.get("quest_book.tab." + key), item, null, tooltip, getter, () -> true, event -> {
                    HUDConfig config = HUDConfig.get();
                    if (event.hasShiftDown() && category != null) {
                        config.questBookShowMain = category == QuestBookScreen.QuestCategory.MAIN;
                        config.questBookShowSide = category == QuestBookScreen.QuestCategory.SIDE;
                        config.questBookShowDaily = category == QuestBookScreen.QuestCategory.DAILY;
                    } else {
                        setter.accept(!getter.getAsBoolean());
                    }

                    if (config.questBookShowDaily && !QuestModel.get().hasDailyQuestData()) {
                        QuestModel.get().requestDailyQuests();
                    }

                    HUDConfig.save();
                    this.currentPage = 0;
                    this.reloadElements();
                }
            )
        );
    }

    private List<QuestInfo> collectQuests() {
        QuestModel model = QuestModel.get();
        HUDConfig config = HUDConfig.get();
        List<QuestInfo> quests = new ArrayList<>();
        if (config.questBookShowMain) {
            quests.add(model.getMainQuest());
        }

        if (config.questBookShowSide) {
            for (QuestInfo quest : model.getSideQuests()) {
                if (config.questBookShowCompleted || quest.status() != QuestStatus.COMPLETED) {
                    quests.add(quest);
                }
            }
        }

        if (config.questBookShowDaily) {
            for (QuestInfo quest : model.getDailyQuests()) {
                if (config.questBookShowCompleted || quest.status() != QuestStatus.COMPLETED) {
                    quests.add(quest);
                }
            }
        }

        String search = this.searchText.toLowerCase(Locale.ROOT).trim();
        if (!search.isEmpty()) {
            quests.removeIf(q -> !q.plainName().toLowerCase(Locale.ROOT).contains(search));
        }

        return quests;
    }

    private void reloadElements() {
        this.elements.clear();
        this.elements.addAll(this.collectQuests());
        this.maxPage = Math.max(0, (this.elements.size() + this.layout.rowsPerPage() - 1) / this.layout.rowsPerPage() - 1);
        this.currentPage = Mth.clamp(this.currentPage, 0, this.maxPage);

        for (QuestEntryButton button : this.entryButtons) {
            this.removeWidget(button);
        }

        this.entryButtons.clear();
        int start = this.currentPage * this.layout.rowsPerPage();

        for (int i = start; i < Math.min(this.elements.size(), start + this.layout.rowsPerPage()); i++) {
            int slot = i - start;
            QuestEntryButton button = new QuestEntryButton(
                this.layout.rightX(),
                this.layout.listTop() + slot * this.layout.rowHeight(),
                this.layout.contentWidth(),
                this.layout.rowHeight() - 3,
                this.elements.get(i),
                this
            );
            this.entryButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.collectQuests().equals(this.elements)) {
            this.reloadElements();
        }
    }

    private void setCurrentPage(int page) {
        int clamped = Mth.clamp(page, 0, this.maxPage);
        if (clamped != this.currentPage) {
            this.currentPage = clamped;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BOOK_TURN_PAGE, 1.0F));
            this.reloadElements();
        }
    }

    public String getSelectedKey() {
        return this.selectedKey;
    }

    private boolean hasGuidePage() {
        return this.guidePage() != null;
    }

    private void refreshGuideTooltip() {
        this.guideTooltip.clear();
        this.guideTooltip.add(ModLanguageManager.get("quest_book.guide.open"));
        QuestInfo selected = QuestModel.get().findByKey(this.selectedKey);
        if (selected != null) {
            this.guideTooltip.add(selected.name().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    private void openSelectedGuide() {
        URI page = this.guidePage();
        if (page != null) {
            try {
                Util.getPlatform().openUri(page);
            } catch (RuntimeException e) {
                System.err.println("[ATT2] Could not open the quest guide: " + e.getMessage());
            }
        }
    }

    private URI guidePage() {
        QuestInfo selected = QuestModel.get().findByKey(this.selectedKey);
        if (selected == null) {
            return null;
        }

        int number = selected.isMain() ? QuestModel.get().getMainStep() : selected.number();
        return GuideLinks.page(selected.type(), number, selected.city(), ModLanguageManager.getCurrentLanguage());
    }

    public void selectQuest(QuestInfo quest) {
        if (!quest.key().equals(this.selectedKey)) {
            this.leftScroll = 0;
        }

        this.selectedKey = quest.key();
        QuestModel.get().setSelected(quest);
        QuestModel.get().requestDetails(quest);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.refreshGuideTooltip();
        super.render(graphics, mouseX, mouseY, partialTick);
        this.hovered = null;

        for (GuiEventListener child : this.children()) {
            if (child instanceof QuestBookWidget widget && widget.isMouseOver(mouseX, mouseY)) {
                this.hovered = widget;
            }
        }

        this.renderLeftPage(graphics);
        this.renderRightPageInfo(graphics);
        if (this.hovered != null) {
            List<Component> lines = this.hovered.getTooltipLines();
            if (!lines.isEmpty()) {
                graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (this.layout != null) {
            QuestBookSkin.book(graphics, this.layout);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.layout.x() + 18, this.layout.y() + 9);
            graphics.pose().scale(1.35F, 1.35F);
            graphics.drawString(this.font, ModLanguageManager.get("quest_book.journal"), 0, 0, -660784, false);
            graphics.pose().popMatrix();
            graphics.drawString(this.font, "Across The Time II", this.layout.x() + 19, this.layout.y() + 24, -660784, false);
            graphics.drawString(this.font, ModLanguageManager.get("quest_book.objective"), this.layout.leftX(), this.layout.y() + 46, -9413049, false);
            graphics.fill(this.layout.leftX(), this.layout.y() + 61, this.layout.leftX() + this.layout.contentWidth(), this.layout.y() + 62, -4216453);
            int sx = this.layout.rightX();
            int sy = this.layout.searchY();
            int sw = this.layout.contentWidth();
            graphics.fill(sx, sy, sx + sw, sy + 20, -4216453);
            graphics.fill(sx + 1, sy + 1, sx + sw - 1, sy + 19, -396834);
        }
    }

    public float textScale() {
        return this.layout.textScale();
    }

    private int leftViewX() {
        return this.layout.leftX();
    }

    private int leftViewWidth() {
        return this.layout.contentWidth() - 8;
    }

    private int leftViewTop() {
        return this.layout.detailTop();
    }

    private int leftViewBottom() {
        return this.layout.detailBottom();
    }

    private int lineSpacing() {
        return (int)Math.ceil(9.0F * this.textScale()) + 3;
    }

    private int leftMaxScroll() {
        return Math.max(0, this.leftContentHeight - (this.leftViewBottom() - this.leftViewTop()));
    }

    private void renderLeftPage(GuiGraphics graphics) {
        int textX = this.leftViewX();
        int textWidth = this.leftViewWidth();
        int top = this.leftViewTop();
        int bottom = this.leftViewBottom();
        int viewport = bottom - top;
        List<QuestBookScreen.LeftBlock> blocks = this.buildLeftBlocks();
        this.leftContentHeight = this.measureBlocks(blocks, textWidth);
        this.leftScroll = Mth.clamp(this.leftScroll, 0, this.leftMaxScroll());
        graphics.enableScissor(textX, top, textX + textWidth, bottom);
        int y = top - this.leftScroll;

        for (QuestBookScreen.LeftBlock block : blocks) {
            y = this.drawWrapped(graphics, block.text(), textX, y, textWidth, block.color()) + block.gapAfter();
        }

        graphics.disableScissor();
        if (this.leftMaxScroll() > 0) {
            this.drawLeftScrollbar(graphics, top, bottom, viewport);
        }
    }

    private List<QuestBookScreen.LeftBlock> buildLeftBlocks() {
        QuestModel model = QuestModel.get();
        QuestInfo selected = model.findByKey(this.selectedKey);
        HUDConfig config = HUDConfig.get();
        int textColor = HUDConfig.color(config.questBookTextColor, -16777216);
        int secondary = HUDConfig.color(config.questBookSecondaryTextColor, -12566464);
        List<QuestBookScreen.LeftBlock> blocks = new ArrayList<>();
        if (selected == null) {
            blocks.add(new QuestBookScreen.LeftBlock(ModLanguageManager.get("quest_book.description1"), textColor, 6));
            blocks.add(new QuestBookScreen.LeftBlock(ModLanguageManager.get("quest_book.description2"), textColor, 0));
            return blocks;
        }

        Component header = Component.literal(selected.plainName()).withStyle(ChatFormatting.BOLD);
        blocks.add(new QuestBookScreen.LeftBlock(header, -13096159, 5));
        blocks.add(
            new QuestBookScreen.LeftBlock(
                ModLanguageManager.get("quest_book.status." + selected.status().name().toLowerCase(Locale.ROOT)),
                QuestBookSkin.statusColor(selected.status()),
                8
            )
        );
        if (selected.isMain()) {
            int step = model.getMainStep();
            String progress = ModLanguageManager.format("quest_book.main_progress", "step", step < 0 ? "?" : step, "max", model.getMainMaxStep());
            blocks.add(new QuestBookScreen.LeftBlock(Component.literal(progress), secondary, 0));
        } else if (selected.isDaily()) {
            Component city = Component.literal(ModLanguageManager.getString("quest_book.daily_city") + " ").append(selected.cityName());
            blocks.add(new QuestBookScreen.LeftBlock(city, secondary, 0));
            if (selected.remainingSeconds() >= 0) {
                String time = ModLanguageManager.format("quest_book.daily_remaining", "time", selected.formatRemainingTime());
                blocks.add(new QuestBookScreen.LeftBlock(Component.literal(time), secondary, 0));
            }
        }

        List<Component> description = model.getDescription(selected);
        if (description.isEmpty()) {
            Component hint = model.isCapturing() ? ModLanguageManager.get("quest_book.loading") : ModLanguageManager.get("quest_book.no_description");
            blocks.add(new QuestBookScreen.LeftBlock(hint, secondary, 0));
            return blocks;
        }

        if (!blocks.isEmpty()) {
            QuestBookScreen.LeftBlock last = blocks.remove(blocks.size() - 1);
            blocks.add(new QuestBookScreen.LeftBlock(last.text(), last.color(), last.gapAfter() + 4));
        }

        for (int i = 0; i < description.size(); i++) {
            int gap = i == description.size() - 1 ? 0 : 2;
            blocks.add(new QuestBookScreen.LeftBlock(description.get(i), textColor, gap));
        }

        return blocks;
    }

    private int measureBlocks(List<QuestBookScreen.LeftBlock> blocks, int width) {
        int height = 0;

        for (QuestBookScreen.LeftBlock block : blocks) {
            height += this.font.split(block.text(), (int)(width / this.textScale())).size() * this.lineSpacing() + block.gapAfter();
        }

        return height;
    }

    private void drawLeftScrollbar(GuiGraphics graphics, int top, int bottom, int viewport) {
        int barX = this.leftViewX() + this.leftViewWidth() + 1;
        int trackH = bottom - top;
        int thumbH = Math.max(8, viewport * viewport / this.leftContentHeight);
        int maxThumbTravel = trackH - thumbH;
        int thumbY = top + (this.leftMaxScroll() == 0 ? 0 : this.leftScroll * maxThumbTravel / this.leftMaxScroll());
        graphics.fill(barX, top, barX + 5, bottom, 855638016);
        graphics.fill(barX, thumbY, barX + 5, thumbY + thumbH, -1728053248);
    }

    private boolean isOverLeftText(double mouseX, double mouseY) {
        return mouseX >= this.leftViewX()
            && mouseX < this.leftViewX() + this.leftViewWidth() + 5 + 2
            && mouseY >= this.leftViewTop()
            && mouseY < this.leftViewBottom();
    }

    private boolean isOverLeftScrollbar(double mouseX, double mouseY) {
        if (this.leftMaxScroll() <= 0) {
            return false;
        }

        int barX = this.leftViewX() + this.leftViewWidth() + 1;
        return mouseX >= barX - 1 && mouseX < barX + 5 + 2 && mouseY >= this.leftViewTop() && mouseY < this.leftViewBottom();
    }

    private void scrollLeftTo(double mouseY) {
        int top = this.leftViewTop();
        int bottom = this.leftViewBottom();
        int viewport = bottom - top;
        int thumbH = Math.max(8, viewport * viewport / Math.max(this.leftContentHeight, 1));
        int maxThumbTravel = Math.max(1, viewport - thumbH);
        double rel = (mouseY - top - thumbH / 2.0) / maxThumbTravel;
        this.leftScroll = Mth.clamp((int)Math.round(rel * this.leftMaxScroll()), 0, this.leftMaxScroll());
    }

    private int drawWrapped(GuiGraphics graphics, Component text, int x, int y, int width, int defaultColor) {
        for (FormattedCharSequence line : this.font.split(text, (int)(width / this.textScale()))) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(this.textScale(), this.textScale());
            graphics.drawString(this.font, line, 0, 0, defaultColor, false);
            graphics.pose().popMatrix();
            y += this.lineSpacing();
        }

        return y;
    }

    private void renderRightPageInfo(GuiGraphics graphics) {
        String pageInfo = this.currentPage + 1 + " / " + (this.maxPage + 1);
        graphics.drawString(
            this.font,
            pageInfo,
            this.layout.rightX() + (this.layout.contentWidth() - this.font.width(pageInfo)) / 2,
            this.layout.footerY() + 6,
            -13096159,
            false
        );
        QuestModel model = QuestModel.get();
        String counter = ModLanguageManager.format("quest_book.progress_short", "done", model.getSideCompleted(), "total", model.getSideTotal());
        graphics.drawString(this.font, counter, this.layout.leftX(), this.layout.footerY() + 6, -9413049, false);
        if (this.elements.isEmpty()) {
            Component hint = !model.isWaitingForSideQuests() && !model.isWaitingForDailyQuests()
                ? ModLanguageManager.get("quest_book.empty_short")
                : ModLanguageManager.get("quest_book.loading");
            graphics.enableScissor(this.layout.rightX(), this.layout.listTop(), this.layout.rightX() + this.layout.contentWidth(), this.layout.footerY() - 6);
            this.drawWrapped(graphics, hint, this.layout.rightX() + 4, this.layout.listTop() + 5, this.layout.contentWidth() - 8, -9413049);
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0.0 && this.isOverLeftText(mouseX, mouseY) && this.leftMaxScroll() > 0) {
            int step = Math.max(9, (int)Math.round(Math.abs(deltaY) * 9.0));
            this.leftScroll = Mth.clamp(this.leftScroll - (int)Math.signum(deltaY) * step, 0, this.leftMaxScroll());
            return true;
        } else if (deltaY != 0.0
            && mouseX >= this.layout.rightX()
            && mouseX <= this.layout.rightX() + this.layout.contentWidth()
            && mouseY >= this.layout.listTop()
            && mouseY <= this.layout.footerY()) {
            this.setCurrentPage(this.currentPage - (int)Math.signum(deltaY));
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.isOverLeftScrollbar(event.x(), event.y())) {
            this.draggingLeftScroll = true;
            this.scrollLeftTo(event.y());
            return true;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingLeftScroll && event.button() == 0) {
            this.scrollLeftTo(event.y());
            return true;
        } else {
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingLeftScroll) {
            this.draggingLeftScroll = false;
            return true;
        } else {
            return super.mouseReleased(event);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        } else if (this.searchBox != null && this.searchBox.isFocused()) {
            return this.searchBox.keyPressed(event) || super.keyPressed(event);
        } else if (event.key() == 267) {
            this.setCurrentPage(this.currentPage + 1);
            return true;
        } else if (event.key() == 266) {
            this.setCurrentPage(this.currentPage - 1);
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    private record LeftBlock(Component text, int color, int gapAfter) {
    }

    private enum QuestCategory {
        MAIN,
        SIDE,
        DAILY;
    }
}
