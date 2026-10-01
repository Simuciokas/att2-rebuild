package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;

public class ShopScreen extends Screen {
    private static final SoundEvent OPEN_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private int offsetX;
    private int offsetY;
    private int page;
    private int pages = 1;
    private int lastVersion = -1;
    private float scale;
    private ShopType type = ShopType.GENERAL;
    private String categoryFilter = "";
    private List<String> categories = List.of();
    private List<String> chromeCategories = List.of();
    private ShopType chromeType;
    private boolean chromeRepair;
    private ShopActionButton prevNav;
    private ShopActionButton nextNav;

    private boolean comparisonHeld() {
        return ShopComparison.isHeld(this.minecraft.getWindow().handle(), KeybindManager.compareKeyCode());
    }

    private ShopScreen() {
        super(ModLanguageManager.get("shop.title"));
    }

    public static void open() {
        if (HUDConfig.get().isShopMenuEnabled(ShopModel.get().menuTypeId())) {
            Minecraft client = Minecraft.getInstance();
            if (client.screen instanceof ShopScreen open) {
                open.rebuild();
            } else if (client.screen == null || client.screen instanceof ChatScreen) {
                client.setScreen(new ShopScreen());
                client.getSoundManager().play(SimpleSoundInstance.forUI(OPEN_SOUND, 1.0F));
            }
        }
    }

    public static boolean isOpen() {
        return Minecraft.getInstance().screen instanceof ShopScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.chromeType = null;
        this.scale = ShopViewport.fit(this.width, this.height, 640, 392).scale() * 0.44444445F;
        this.offsetX = (this.width - this.size(1440)) / 2;
        this.offsetY = (this.height - this.size(880)) / 2;
        this.rebuild();
    }

    private int size(int value) {
        return Math.max(1, Math.round(value * this.scale));
    }

    private int x(int value) {
        return this.offsetX + Math.round(value * this.scale);
    }

    private int y(int value) {
        return this.offsetY + Math.round(value * this.scale);
    }

    private float textScale() {
        return this.scale * 2.7F;
    }

    @Override
    public void tick() {
        if (ShopModel.get().version() != this.lastVersion) {
            this.rebuild();
        }
    }

    @Override
    public void removed() {
        CurrencyModel.close();
        super.removed();
    }

    private void rebuild() {
        ShopModel model = ShopModel.get();
        ShopCatalog catalog = model.catalog();
        catalog.inferType();
        if (this.type != catalog.type()) {
            this.categoryFilter = "";
            this.page = 0;
        }

        this.type = catalog.type();
        this.lastVersion = model.version();
        CurrencyModel.request();
        if (catalog.offers().isEmpty() && model.isCollecting() && !catalog.categories().isEmpty()) {
            this.categories = List.copyOf(catalog.categories());
        } else {
            this.categories = catalog.categories().stream().filter(c -> catalog.offers().stream().anyMatch(o -> c.equals(o.category()))).toList();
        }

        if (!this.categories.contains(this.categoryFilter)) {
            this.categoryFilter = "";
        }

        boolean keepChrome = this.chromeType == this.type
            && this.chromeRepair == model.isRepairView()
            && this.chromeCategories.equals(this.categories)
            && !this.children().isEmpty();
        if (keepChrome) {
            for (GuiEventListener child : List.copyOf(this.children())) {
                if (child instanceof ShopSlotButton slot) {
                    this.removeWidget(slot);
                }
            }
        } else {
            this.clearWidgets();
            this.chromeCategories = List.copyOf(this.categories);
            this.chromeType = this.type;
            this.chromeRepair = model.isRepairView();
        }

        List<ShopOffer> offers = model.isRepairView()
            ? model.repairOffers()
            : catalog.offers().stream().filter(o -> this.categoryFilter.isEmpty() || this.categoryFilter.equals(o.category())).toList();
        this.pages = Math.max(1, (offers.size() + 5) / 6);
        this.page = Math.max(0, Math.min(this.page, this.pages - 1));

        for (int i = this.page * 6; i < Math.min(offers.size(), this.page * 6 + 6); i++) {
            int local = i - this.page * 6;
            ShopOffer offer = offers.get(i);
            this.addRenderableWidget(
                new ShopSlotButton(
                    this.x(364 + local % 3 * 330),
                    this.y(211 + local / 3 * 270),
                    this.size(312),
                    this.size(252),
                    offer,
                    () -> ShopModel.get().buy(offer),
                    this.type
                )
            );
        }

        if (!keepChrome) {
            this.addChrome(model, catalog);
        } else if (this.prevNav != null && this.nextNav != null) {
            this.prevNav.active = this.page > 0;
            this.nextNav.active = this.page + 1 < this.pages;
        }
    }

    private void addChrome(ShopModel model, ShopCatalog catalog) {
        if (!model.isRepairView() && this.categories.size() > 1) {
            List<String> filters = new ArrayList<>();
            filters.add("");
            filters.addAll(this.categories);
            int step = Math.min(72, 500 / filters.size());

            for (int i = 0; i < filters.size(); i++) {
                String filter = filters.get(i);
                Component label = filter.isEmpty() ? ModLanguageManager.get("shop.category.all") : Component.literal(prettyCategory(filter));
                this.button(
                    48,
                    202 + i * step,
                    250,
                    Math.min(58, step - 6),
                    new ShopAction(-1, label, null, ShopAction.Kind.OTHER),
                    ShopActionButton.Chrome.TAB,
                    () -> filter.equals(this.categoryFilter),
                    () -> {
                        this.categoryFilter = filter;
                        this.page = 0;
                        this.rebuild();
                    }
                );
            }
        }

        List<ShopAction> canonical = new ArrayList<>();
        if (model.hasRepair()) {
            addIfMissing(canonical, 413, "shop.action.mending_price", ShopAction.Kind.MENDING_PRICE);
            addIfMissing(canonical, 414, "shop.action.mending_repair", ShopAction.Kind.MENDING_REPAIR);
            addIfMissing(canonical, 415, "shop.action.mending_tools", ShopAction.Kind.MENDING_TOOLS);
        }

        if (model.hasReset()) {
            addIfMissing(canonical, 423, "shop.action.reset", ShopAction.Kind.RESET);
        }

        if (model.isRepairView()) {
            addIfMissing(canonical, -2, "shop.action.back", ShopAction.Kind.BACK);
        }

        List<ShopAction> actions = ShopFooterActions.merge(canonical, catalog.actions());
        int columns = Math.min(3, Math.max(1, actions.size()));
        int rows = Math.max(1, (actions.size() + columns - 1) / columns);

        for (int i = 0; i < actions.size(); i++) {
            ShopAction action = actions.get(i);
            this.button(
                48 + i % columns * 438,
                774 + i / columns * (82 / rows),
                420,
                Math.min(50, 76 / rows),
                action,
                ShopActionButton.Chrome.ACTION,
                () -> false,
                () -> ShopModel.get().run(action)
            );
        }

        this.prevNav = this.button(1122, 158, 42, 38, navigation("‹"), ShopActionButton.Chrome.NAV, () -> false, () -> {
            this.page--;
            this.rebuild();
        });
        this.prevNav.active = this.page > 0;
        this.nextNav = this.button(1294, 158, 42, 38, navigation("›"), ShopActionButton.Chrome.NAV, () -> false, () -> {
            this.page++;
            this.rebuild();
        });
        this.nextNav.active = this.page + 1 < this.pages;
        this.button(
            1350,
            51,
            42,
            42,
            new ShopAction(-1, ModLanguageManager.get("ui.close"), null, ShopAction.Kind.OTHER),
            ShopActionButton.Chrome.CLOSE,
            () -> false,
            this::onClose
        );
    }

    private static ShopAction navigation(String label) {
        return new ShopAction(-1, Component.literal(label), null, ShopAction.Kind.OTHER);
    }

    private ShopActionButton button(int x, int y, int w, int h, ShopAction action, ShopActionButton.Chrome chrome, BooleanSupplier selected, Runnable callback) {
        return this.addRenderableWidget(
            new ShopActionButton(this.x(x), this.y(y), this.size(w), this.size(h), action, -13096159, -660784, callback, chrome, selected, this.type)
        );
    }

    private static void addIfMissing(List<ShopAction> actions, int trigger, String key, ShopAction.Kind kind) {
        if (!actions.stream().anyMatch(a -> a.trigger() == trigger)) {
            actions.add(new ShopAction(trigger, ModLanguageManager.get(key), trigger == 423 ? ModLanguageManager.get("shop.action.reset.tip") : null, kind));
        }
    }

    private static String prettyCategory(String category) {
        String cleaned = category.replace('<', ' ').replace('>', ' ').replace('-', ' ').strip();
        return cleaned.isEmpty() ? category : cleaned;
    }

    private void asset(GuiGraphics graphics, String asset, int x, int y, int w, int h, int sw, int sh) {
        ShopSkin.texture(graphics, this.type, asset, this.x(x), this.y(y), this.size(w), this.size(h), sw, sh);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fillGradient(0, 0, this.width, this.height, -1340596188, -620229616);
        this.asset(graphics, "panel_main", 0, 0, 1440, 880, 1440, 880);
        this.asset(graphics, "emblem", 48, 48, 67, 67, 128, 128);
        this.asset(graphics, "wallet", 1094, 38, 232, 70, 232, 70);
        int decorY = !ShopModel.get().isRepairView() && this.categories.size() > 1 ? 521 : 325;
        if (this.categories.size() <= 4 || ShopModel.get().isRepairView()) {
            this.asset(graphics, "separator", 48, decorY - 22, 250, 13, 250, 13);
            this.asset(graphics, "medallion", 48, decorY, 250, 218, 250, 218);
            this.asset(graphics, "emblem", 104, decorY + 46, 138, 138, 128, 128);
            this.asset(graphics, "ambiance", 48, decorY + 97, 250, 130, 250, 130);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ShopModel model = ShopModel.get();
        ShopCatalog catalog = model.catalog();
        Component title = model.isRepairView() && model.repairTitle() != null
            ? model.repairTitle()
            : (
                catalog.usesEsc()
                    ? (model.seller() != null ? Component.translatable("att2.npc.name." + model.seller().npcId()) : ModLanguageManager.get("shop.title"))
                    : ModLanguageManager.get(this.type.titleKey())
            );
        ShopTheme.text(graphics, title, this.x(137), this.y(61), this.size(900), this.scale * 4.5F, -990260, false);
        boolean esc = catalog.usesEsc();
        Component balance = Component.literal(String.valueOf(ScoreCache.getOrDefault(esc ? "ESC" : "CHRONOTON", 0)));
        ShopTheme.text(
            graphics, Component.literal(esc ? "ESC" : "Chronotons"), this.x(1110), this.y(47), this.size(200), this.scale * 1.8F, this.type.titleColor, false
        );
        int icon = this.size(26);
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.x(1110), this.y(72));
        graphics.pose().scale(icon / 16.0F, icon / 16.0F);
        graphics.renderItem(ShopIcons.currency(esc), 0, 0);
        graphics.pose().popMatrix();
        ShopTheme.text(graphics, balance, this.x(1144), this.y(73), this.size(166), this.textScale(), -995700, false);
        ShopTheme.text(
            graphics, Component.literal(this.page + 1 + " / " + this.pages), this.x(1171), this.y(166), this.size(116), this.textScale(), -9413049, true
        );
        Component meta = catalog.discount() != null ? catalog.discount() : catalog.powderStock();
        if (meta != null) {
            ShopTheme.text(graphics, meta, this.x(364), this.y(164), this.size(725), this.textScale(), -9413049, false);
        }

        if (catalog.remaining() != null && !ShopTellraws.isForceResetAction(catalog.remaining().getString())) {
            ShopTheme.text(graphics, catalog.remaining(), this.x(137), this.y(109), this.size(880), this.textScale(), -2505314, false);
        }

        if (model.isRepairView() && model.repairOffers().isEmpty()) {
            int lineY = 224;

            for (Component line : model.repairLines()) {
                for (FormattedCharSequence wrapped : this.font.split(line, Math.max(1, (int)(this.size(956) / this.textScale())))) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(this.x(364), this.y(lineY));
                    graphics.pose().scale(this.textScale(), this.textScale());
                    graphics.drawString(this.font, wrapped, 0, 0, -13096159, false);
                    graphics.pose().popMatrix();
                    lineY += Math.max(32, (int)(10.0F * this.textScale() / this.scale));
                }
            }
        } else if (catalog.offers().isEmpty()) {
            ShopTheme.text(
                graphics,
                ModLanguageManager.get(model.isCollecting() ? "shop.loading" : "shop.empty"),
                this.x(364),
                this.y(400),
                this.size(972),
                this.textScale(),
                -9413049,
                true
            );
        }

        ShopSlotButton hoveredSlot = null;
        ShopActionButton hoveredAction = null;

        for (GuiEventListener child : this.children()) {
            if (child instanceof ShopSlotButton slot && ShopHover.showsTooltip(slot.isHovered(), slot.isFocused())) {
                hoveredSlot = slot;
            } else if (child instanceof ShopActionButton action && ShopHover.showsTooltip(action.isHovered(), action.isFocused())) {
                hoveredAction = action;
            }
        }

        if (hoveredSlot != null) {
            EquipmentSlot equipmentSlot = ShopComparison.slot(hoveredSlot.offer());
            ArrayList<Component> lines = new ArrayList<>(hoveredSlot.tooltipLines());
            boolean held = this.comparisonHeld();
            if (equipmentSlot != null && this.minecraft.player != null && held) {
                ShopComparison.render(graphics, lines, this.minecraft.player.getItemBySlot(equipmentSlot), mouseX, mouseY, this.width, this.height);
            } else {
                if (ShopComparison.shouldShowHint(equipmentSlot == null ? null : equipmentSlot.name(), held)) {
                    lines.add(Component.translatable("att2.ui.compare", KeybindManager.compareKeyLabel()).withStyle(ChatFormatting.GRAY));
                }

                graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
        } else if (hoveredAction != null) {
            graphics.setTooltipForNextFrame(this.font, hoveredAction.tooltipLines(), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int next = ShopPaging.afterScroll(this.page, this.pages, deltaY);
        if (next != this.page) {
            this.page = next;
            this.rebuild();
            return true;
        } else {
            return deltaY != 0.0 ? true : super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
    }
}
