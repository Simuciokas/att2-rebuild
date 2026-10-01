package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.rune.RuneCatalog;
import fr.poubone.att2.client.rune.RuneItems;
import fr.poubone.att2.client.rune.RuneStock;
import fr.poubone.att2.client.shop.ShopAction;
import fr.poubone.att2.client.shop.ShopActionButton;
import fr.poubone.att2.client.shop.ShopHover;
import fr.poubone.att2.client.shop.ShopOffer;
import fr.poubone.att2.client.shop.ShopPaging;
import fr.poubone.att2.client.shop.ShopPanelScreen;
import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopSlotButton;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopType;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public class MinerShopScreen extends ShopPanelScreen {
    private static final SoundEvent OPEN_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final ShopType TYPE = ShopType.MINER;
    private static final int PANEL_W = 640;
    private static final int PANEL_H = 392;
    private static final int CARDS_PER_PAGE = 6;
    private static final int CARD_W = 138;
    private static final int CARD_H = 111;
    private static final int CARD_GAP = 6;
    private static final int GRID_X = 164;
    private static final int GRID_Y = 90;
    private MinerCatalog.Tab tab;
    private int page;
    private int pageCount = 1;
    private int lastHash = Integer.MIN_VALUE;
    private ShopActionButton prevNav;
    private ShopActionButton nextNav;

    private MinerShopScreen(MinerCatalog.Tab tab) {
        super(ModLanguageManager.get("miner_shop.title"), 640, 392);
        this.tab = tab;
    }

    public static void open(MinerCatalog.Tab tab) {
        if (HUDConfig.get().minerMenuEnabled) {
            Minecraft client = Minecraft.getInstance();
            if (!(client.screen instanceof MinerShopScreen)) {
                if (client.screen == null || client.screen instanceof ChatScreen) {
                    client.setScreen(new MinerShopScreen(tab));
                    client.getSoundManager().play(SimpleSoundInstance.forUI(OPEN_SOUND, 1.0F));
                }
            }
        }
    }

    public void switchTab(MinerCatalog.Tab tab) {
        MinerCatalog.Tab previous = this.tab;
        this.tab = tab;
        this.page = 0;
        if (tab == MinerCatalog.Tab.EXCHANGE && previous != tab && MinerShopModel.get().exchanges().isEmpty()) {
            MinerShopModel.get().requestExchangeRefresh();
        }

        this.rebuild(true);
    }

    public MinerCatalog.Tab tab() {
        return this.tab;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.initViewport();
        MinerShopModel.get().onScreenOpened();
        this.rebuild(true);
    }

    @Override
    public void removed() {
        MinerShopModel.get().onScreenClosed();
        super.removed();
    }

    private void rebuild(boolean full) {
        if (full) {
            this.clearWidgets();
            this.prevNav = null;
            this.nextNav = null;
            this.buildTabs();
            this.chrome(603, 22, 18, 18, Component.translatable("gui.close"), ShopActionButton.Chrome.CLOSE, TYPE, this::onClose);
        } else {
            for (GuiEventListener child : List.copyOf(this.children())) {
                if (child instanceof ShopSlotButton) {
                    this.removeWidget(child);
                }
            }

            this.removeNavButtons();
        }

        this.buildCardsAndNav();
        this.lastHash = this.stockHash();
    }

    private void buildTabs() {
        int tabY = 88;
        int tabX = 21;

        for (MinerCatalog.Tab t : MinerCatalog.Tab.values()) {
            ShopAction action = new ShopAction(-1, ModLanguageManager.get(tabLangKey(t)), null, ShopAction.Kind.OTHER);
            this.addRenderableWidget(
                new ShopActionButton(
                    tabX, tabY, 128, 24, action, -4675448, TYPE.titleColor, () -> this.switchTab(t), ShopActionButton.Chrome.TAB, () -> this.tab == t, TYPE
                )
            );
            tabY += 32;
        }
    }

    private void buildCardsAndNav() {
        List<ShopOffer> offers = this.buildOffers();
        this.pageCount = Math.max(1, (offers.size() + 6 - 1) / 6);
        this.page = Math.max(0, Math.min(this.page, this.pageCount - 1));
        int from = this.page * 6;
        int to = Math.min(offers.size(), from + 6);

        for (int i = from; i < to; i++) {
            int local = i - from;
            ShopOffer offer = offers.get(i);
            int cx = 164 + local % 3 * 144;
            int cy = 90 + local / 3 * 117;
            int trigger = offer.trigger();
            boolean locked = trigger <= 0;
            this.addRenderableWidget(new ShopSlotButton(cx, cy, 138, 111, offer, () -> {
                if (!locked) {
                    MinerShopModel.get().send(trigger);
                }
            }, TYPE));
        }

        if (this.pageCount > 1) {
            ShopAction prev = new ShopAction(-1, Component.literal("‹"), null, ShopAction.Kind.OTHER);
            ShopAction next = new ShopAction(-1, Component.literal("›"), null, ShopAction.Kind.OTHER);
            this.prevNav = new ShopActionButton(490, 350, 18, 18, prev, TYPE.titleColor, -3896, () -> {
                if (this.page > 0) {
                    this.page--;
                    this.rebuild(false);
                }
            }, ShopActionButton.Chrome.NAV, () -> false, TYPE);
            this.nextNav = new ShopActionButton(576, 350, 18, 18, next, TYPE.titleColor, -3896, () -> {
                if (this.page + 1 < this.pageCount) {
                    this.page++;
                    this.rebuild(false);
                }
            }, ShopActionButton.Chrome.NAV, () -> false, TYPE);
            this.prevNav.active = this.page > 0;
            this.nextNav.active = this.page + 1 < this.pageCount;
            this.addRenderableWidget(this.prevNav);
            this.addRenderableWidget(this.nextNav);
        }
    }

    private List<ShopOffer> buildOffers() {
        LocalPlayer player = Minecraft.getInstance().player;

        return switch (this.tab) {
            case BUY -> this.buyOffers();
            case POWDER -> this.powderOffers();
            case EXCHANGE -> this.exchangeOffers(player);
            case ACTIONS -> this.actionOffers();
        };
    }

    private List<ShopOffer> buyOffers() {
        List<ShopOffer> out = new ArrayList<>();

        for (MinerCatalog.BuyOffer offer : MinerCatalog.buys()) {
            OptionalInt price = MinerShopModel.get().price(offer.trigger());
            ItemStack rune = RuneItems.rune(offer.rune());
            Component name = Component.translatable(offer.rune().translationKey());
            Component priceText;
            int trigger;
            if (price.isEmpty()) {
                priceText = ModLanguageManager.get("miner_shop.locked").withStyle(ChatFormatting.GRAY);
                trigger = 0;
            } else {
                priceText = Component.literal(ModLanguageManager.format("miner_shop.chronoton", "n", price.getAsInt()));
                trigger = offer.trigger();
            }

            out.add(new ShopOffer(rune, name, priceText, trigger, "buy", offer.rune().translationKey(), null, null));
        }

        return out;
    }

    private List<ShopOffer> powderOffers() {
        List<ShopOffer> out = new ArrayList<>();

        for (MinerCatalog.PowderOffer offer : MinerCatalog.powders()) {
            ItemStack esc = RuneItems.esc();
            esc.setCount(offer.esc());
            Component name = Component.translatable("att2.runes.recipe.runepowder");
            Component price = Component.literal(
                ModLanguageManager.format("miner_shop.powder_amount", "n", offer.powder())
                    + " → "
                    + ModLanguageManager.format("rune_codex.esc", "n", offer.esc())
            );
            out.add(new ShopOffer(esc, name, price, offer.trigger(), "powder", "att2.runes.recipe.runepowder", null, null));
        }

        return out;
    }

    private List<ShopOffer> exchangeOffers(LocalPlayer player) {
        List<ShopOffer> out = new ArrayList<>();

        for (MinerCatalog.ExchangeOffer offer : MinerShopModel.get().exchanges()) {
            Optional<RuneCatalog.RuneDef> result = MinerCatalog.runeByName(offer.result());
            if (!result.isEmpty()) {
                ItemStack stack = RuneItems.rune(result.get());
                Component name = Component.translatable(result.get().translationKey());
                StringBuilder priceBuf = new StringBuilder();
                List<Component> loreParts = new ArrayList<>();

                for (String id : offer.ingredients()) {
                    MinerCatalog.runeByName(id).ifPresent(rune -> {
                        if (!priceBuf.isEmpty()) {
                            priceBuf.append(" + ");
                        }

                        String label = Component.translatable(rune.translationKey()).getString();
                        priceBuf.append(label);
                        if (player != null) {
                            RuneStock.Counts counts = RuneStock.counts(player, rune);
                            ChatFormatting color = counts.inventory() >= 1 ? ChatFormatting.GREEN : ChatFormatting.RED;
                            loreParts.add(Component.literal(label + " " + counts.inventory() + "/1").withStyle(color));
                        }
                    });
                }

                Component price = priceBuf.isEmpty() ? ModLanguageManager.get("miner_shop.exchange") : Component.literal(priceBuf.toString());
                Component lore = loreParts.isEmpty() ? null : loreParts.get(0);
                if (loreParts.size() > 1) {
                    Component combined = Component.empty();

                    for (int i = 0; i < loreParts.size(); i++) {
                        if (i > 0) {
                            combined.getSiblings().add(Component.literal(" · "));
                        }

                        combined.getSiblings().add(loreParts.get(i));
                    }

                    lore = combined;
                }

                out.add(new ShopOffer(stack, name, price, offer.trigger(), "exchange", result.get().translationKey(), null, lore));
            }
        }

        return out;
    }

    private List<ShopOffer> actionOffers() {
        List<ShopOffer> out = new ArrayList<>();
        out.add(
            new ShopOffer(
                RuneItems.darkResin(),
                ModLanguageManager.get("miner_shop.action.resin"),
                ModLanguageManager.get("miner_shop.action.resin.tip"),
                3490,
                "actions",
                "miner_shop.action.resin",
                null,
                ModLanguageManager.get("miner_shop.action.resin.tip")
            )
        );
        out.add(
            new ShopOffer(
                RuneItems.runicOre(),
                ModLanguageManager.get("miner_shop.action.ore"),
                ModLanguageManager.get("miner_shop.action.ore.tip"),
                3536,
                "actions",
                "miner_shop.action.ore",
                null,
                ModLanguageManager.get("miner_shop.action.ore.tip")
            )
        );
        return out;
    }

    private void removeNavButtons() {
        if (this.prevNav != null) {
            this.removeWidget(this.prevNav);
            this.prevNav = null;
        }

        if (this.nextNav != null) {
            this.removeWidget(this.nextNav);
            this.nextNav = null;
        }
    }

    private static int resin() {
        return MinerShopModel.get().resinCount().orElseGet(() -> ScoreCache.getHolder("DARK_RESIN", "Eldric").orElse(0));
    }

    private int stockHash() {
        LocalPlayer player = Minecraft.getInstance().player;
        int hash = resin();
        hash = 31 * hash + ScoreCache.getOrDefault("CHRONOTON", 0);
        hash = 31 * hash + RuneStock.powder().orElse(Integer.MIN_VALUE);
        hash = 31 * hash + MinerShopModel.get().version();
        hash = 31 * hash + RuneStock.esc(player);
        hash = 31 * hash + this.tab.ordinal();
        hash = 31 * hash + this.page;

        for (MinerCatalog.BuyOffer offer : MinerCatalog.buys()) {
            hash = 31 * hash + MinerShopModel.get().price(offer.trigger()).orElse(-1);
        }

        return hash;
    }

    private static String tabLangKey(MinerCatalog.Tab tab) {
        return switch (tab) {
            case BUY -> "miner_shop.tab.buy";
            case POWDER -> "miner_shop.tab.powder";
            case EXCHANGE -> "miner_shop.tab.exchange";
            case ACTIONS -> "miner_shop.tab.actions";
        };
    }

    @Override
    public void tick() {
        super.tick();
        int hash = this.stockHash();
        if (hash != this.lastHash) {
            this.rebuild(false);
        }
    }

    @Override
    protected void renderPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.panel(graphics, TYPE);
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        ShopTheme.text(graphics, Component.literal(this.page + 1 + " / " + this.pageCount), 515, 354, 54, 1.0F, -13096159, true);

        for (GuiEventListener child : this.children()) {
            if (child instanceof ShopActionButton action && ShopHover.showsTooltip(action.isHovered(), action.isFocused())) {
                this.showTooltip(graphics, action.tooltipLines(), mouseX, mouseY);
            }

            if (child instanceof ShopSlotButton slot && ShopHover.showsTooltip(slot.isHovered(), slot.isFocused())) {
                this.showTooltip(graphics, slot.tooltipLines(), mouseX, mouseY);
            }
        }

        int innerX = 62;
        int innerW = 640 - TYPE.insetLeft - TYPE.insetRight;
        int titleY = 26;
        ShopTheme.text(graphics, ModLanguageManager.get("miner_shop.title"), innerX, titleY, 360, 1.5F, TYPE.titleColor, false);
        this.drawHeader(graphics);
        if (this.tab == MinerCatalog.Tab.EXCHANGE && MinerShopModel.get().exchanges().isEmpty()) {
            graphics.drawWordWrap(this.font, ModLanguageManager.get("miner_shop.exchange.loading"), 164, 90, 430, -3622752);
        }
    }

    private void drawHeader(GuiGraphics graphics) {
        String resin = ModLanguageManager.format("miner_shop.resin", "n", resin());
        String chrono = String.valueOf(ScoreCache.getOrDefault("CHRONOTON", 0));
        int icon = 8;
        ShopSkin.texture(graphics, TYPE, "wallet", 454, 17, 140, 37, 232, 70);
        ShopTheme.text(graphics, Component.literal(resin), 465, 23, 116, 0.85F, -3622752, false);
        graphics.pose().pushMatrix();
        graphics.pose().translate(465.0F, 38.0F);
        graphics.pose().scale(icon / 16.0F, icon / 16.0F);
        graphics.renderItem(RuneItems.chronoton(), 0, 0);
        graphics.pose().popMatrix();
        ShopTheme.text(graphics, Component.literal(chrono), 478, 38, 103, 0.85F, -1521558, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0.0 && this.pageCount > 1) {
            int next = ShopPaging.afterScroll(this.page, this.pageCount, deltaY);
            if (next != this.page) {
                this.page = next;
                this.rebuild(false);
            }

            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
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
}
