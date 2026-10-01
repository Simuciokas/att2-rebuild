package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.hud.ModToast;
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
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

public class RuneCodexScreen extends ShopPanelScreen {
    private static final SoundEvent OPEN_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private static final ShopType TYPE = ShopType.RUNES;
    private static final int PANEL_W = 640;
    private static final int PANEL_H = 392;
    private static final int CARD_ORIGIN_X = 164;
    private static final int CARD_ORIGIN_Y = 90;
    private static final int CARD_AREA_W = 430;
    private static final int CARD_AREA_H = 238;
    private static final int CARD_GAP = 6;
    private int offsetX;
    private int offsetY;
    private RuneCatalog.Tab tab;
    private int page;
    private int pageCount = 1;
    private int lastStockHash = Integer.MIN_VALUE;
    private final List<RuneRecipeRow> rows = new ArrayList<>();
    private final List<RuneCardButton> cards = new ArrayList<>();
    private ShopActionButton prevNav;
    private ShopActionButton nextNav;
    private final CraftQuantityMenu qtyMenu = new CraftQuantityMenu(640, 392);
    private int lastClickX;
    private int lastClickY;

    private RuneCodexScreen(RuneCatalog.Tab tab) {
        super(ModLanguageManager.get("rune_codex.title"), 640, 392);
        this.tab = tab;
    }

    public static void open(RuneCatalog.Tab tab) {
        // Menus re-opened by replayed chat would never close, as closing them is never recorded
        if (FlashbackCompat.isInReplay()) {
            return;
        }

        if (HUDConfig.get().runeMenuEnabled) {
            Minecraft client = Minecraft.getInstance();
            if (!(client.screen instanceof RuneCodexScreen)) {
                client.setScreen(new RuneCodexScreen(tab));
                client.getSoundManager().play(SimpleSoundInstance.forUI(OPEN_SOUND, 1.0F));
            }
        }
    }

    public void switchTab(RuneCatalog.Tab tab) {
        this.tab = tab;
        this.page = 0;
        this.rebuild(true);
    }

    public RuneCatalog.Tab tab() {
        return this.tab;
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
        RuneCodexModel.get().onScreenOpened();
        this.rebuild(true);
    }

    @Override
    public void removed() {
        RuneCodexModel.get().onScreenClosed();
        super.removed();
    }

    private boolean usesCards() {
        return switch (this.tab) {
            case SYNTH_LOW, SYNTH_MID, SYNTH_HIGH, WORDS, ARROWS -> true;
            case OTHER, BONUS -> false;
        };
    }

    private void rebuild(boolean full) {
        this.qtyMenu.close();
        if (full) {
            this.clearWidgets();
            this.rows.clear();
            this.cards.clear();
            this.prevNav = null;
            this.nextNav = null;
            this.buildTabs();
            this.chrome(603, 22, 18, 18, ModLanguageManager.get("ui.close"), ShopActionButton.Chrome.CLOSE, TYPE, this::onClose);
        } else {
            for (RuneRecipeRow row : this.rows) {
                this.removeWidget(row);
            }

            this.rows.clear();

            for (RuneCardButton card : this.cards) {
                this.removeWidget(card);
            }

            this.cards.clear();
            this.removeNavButtons();
        }

        if (this.usesCards()) {
            this.buildCardsAndNav();
        } else {
            this.buildRowsAndNav();
        }

        this.lastStockHash = this.stockHash();
    }

    private void buildTabs() {
        int tabY = 88;
        int tabX = 21;

        for (RuneCatalog.Tab t : RuneCatalog.Tab.values()) {
            ShopAction action = new ShopAction(-1, ModLanguageManager.get(tabLangKey(t)), null, ShopAction.Kind.OTHER);
            int tw = 110;
            this.addRenderableWidget(
                new ShopActionButton(
                    tabX, tabY, tw, 24, action, -4675448, TYPE.titleColor, () -> this.switchTab(t), ShopActionButton.Chrome.TAB, () -> this.tab == t, TYPE
                )
            );
            tabY += 32;
        }
    }

    private void buildCardsAndNav() {
        List<RuneCodexScreen.CardDraft> all = this.buildAllCards();
        this.pageCount = RuneCardLogic.pageCount(all.size());
        this.page = RuneCardLogic.clampPage(this.page, this.pageCount);
        int cardW = 139;
        int cardH = Math.min(cardW * 252 / 312, 116);
        int from = this.page * 6;
        int to = Math.min(all.size(), from + 6);

        for (int i = from; i < to; i++) {
            RuneCodexScreen.CardDraft d = all.get(i);
            int local = i - from;
            int x = 164 + RuneCardLogic.gridCol(local) * (cardW + 6);
            int y = 90 + RuneCardLogic.gridRow(local) * (cardH + 6);
            RuneCardButton card = new RuneCardButton(
                x, y, cardW, cardH, d.result(), d.name(), d.price(), d.ingredients(), d.extraTooltip(), d.clickable(), d.onCraft(), TYPE
            );
            this.cards.add(card);
            this.addRenderableWidget(card);
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
                if (this.page < this.pageCount - 1) {
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

    private void buildRowsAndNav() {
        int bodyTop = 90;
        int bodyH = 238;
        int rowW = 430;
        List<RuneCodexScreen.Draft> all = this.buildAllRows(rowW);
        List<int[]> ranges = this.pageRanges(all, bodyH);
        this.clampPage(ranges);
        this.pageCount = ranges.size();
        int from = ranges.get(this.page)[0];
        int to = ranges.get(this.page)[1];
        int rowY = bodyTop;

        for (int i = from; i < to; i++) {
            RuneCodexScreen.Draft draft = all.get(i);
            RuneRecipeRow live = new RuneRecipeRow(
                164, rowY, rowW, draft.height(), draft.result(), draft.ingredients(), draft.caption(), draft.stock(), draft.clickable(), draft.onCraft(), TYPE
            );
            this.rows.add(live);
            this.addRenderableWidget(live);
            rowY += draft.height() + 2;
        }

        if (ranges.size() > 1) {
            ShopAction prev = new ShopAction(-1, Component.literal("‹"), null, ShopAction.Kind.OTHER);
            ShopAction next = new ShopAction(-1, Component.literal("›"), null, ShopAction.Kind.OTHER);
            this.prevNav = new ShopActionButton(490, 350, 18, 18, prev, TYPE.titleColor, -3896, () -> {
                if (this.page > 0) {
                    this.page--;
                    this.rebuild(false);
                }
            }, ShopActionButton.Chrome.NAV, () -> false, TYPE);
            this.nextNav = new ShopActionButton(576, 350, 18, 18, next, TYPE.titleColor, -3896, () -> {
                if (this.page < ranges.size() - 1) {
                    this.page++;
                    this.rebuild(false);
                }
            }, ShopActionButton.Chrome.NAV, () -> false, TYPE);
            this.prevNav.active = this.page > 0;
            this.nextNav.active = this.page + 1 < ranges.size();
            this.addRenderableWidget(this.prevNav);
            this.addRenderableWidget(this.nextNav);
        }
    }

    private List<int[]> pageRanges(List<RuneCodexScreen.Draft> all, int bodyH) {
        List<int[]> ranges = new ArrayList<>();
        int y = 0;
        int start = 0;

        for (int i = 0; i < all.size(); i++) {
            int h = all.get(i).height();
            if (y > 0 && y + h > bodyH) {
                ranges.add(new int[]{start, i});
                start = i;
                y = 0;
            }

            y += h + 2;
        }

        ranges.add(new int[]{start, all.size()});
        return ranges;
    }

    private void clampPage(List<int[]> ranges) {
        if (ranges.isEmpty()) {
            this.page = 0;
        } else {
            if (this.page >= ranges.size()) {
                this.page = ranges.size() - 1;
            }

            if (this.page < 0) {
                this.page = 0;
            }
        }
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

    private List<RuneCodexScreen.CardDraft> buildAllCards() {
        LocalPlayer player = Minecraft.getInstance().player;

        return switch (this.tab) {
            case SYNTH_LOW, SYNTH_MID, SYNTH_HIGH -> this.synthCards(player);
            case WORDS -> this.wordCards(player);
            case ARROWS -> this.arrowCards(player);
            case OTHER, BONUS -> List.of();
        };
    }

    private List<RuneCodexScreen.Draft> buildAllRows(int rowW) {
        if (this.tab == RuneCatalog.Tab.BONUS) {
            return List.of();
        }

        return switch (this.tab) {
            case OTHER -> this.otherRows(rowW);
            default -> List.of();
        };
    }

    private List<RuneCodexScreen.CardDraft> synthCards(LocalPlayer player) {
        List<RuneCodexScreen.CardDraft> drafts = new ArrayList<>();

        for (RuneCatalog.RuneDef rune : RuneCatalog.runes(this.tab)) {
            Component name = Component.translatable(rune.translationKey());
            if (!RuneStock.unlocked(rune)) {
                List<Component> lockedTip = List.of(ModLanguageManager.get("rune_codex.locked"));
                drafts.add(
                    new RuneCodexScreen.CardDraft(
                        RuneItems.rune(rune), name, ModLanguageManager.get("rune_codex.locked"), List.of(), lockedTip, false, (button, shift) -> {}
                    )
                );
            } else {
                RuneStock.Counts counts = RuneStock.counts(player, rune);
                OptionalInt powderNeed = RuneStock.price(rune);
                OptionalInt escNeed = RuneStock.escPrice(rune);
                int powderHave = RuneStock.powder().orElse(0);
                int escHave = RuneStock.esc(player);
                Component price = this.labeledCost("rune_codex.powder", powderHave, powderNeed);
                if (escNeed.orElse(0) > 0) {
                    price = Component.empty().append(price).append(" ").append(this.labeledCost("rune_codex.esc", escHave, escNeed));
                }

                List<Component> extra = new ArrayList<>();
                extra.add(name.copy().withStyle(ChatFormatting.BOLD));
                extra.add(this.labeledCost("rune_codex.powder", powderHave, powderNeed));
                if (escNeed.orElse(0) > 0) {
                    extra.add(this.labeledCost("rune_codex.esc", escHave, escNeed));
                }

                extra.add(Component.literal("inv " + counts.inventory() + (counts.pouch().isPresent() ? "  sac " + counts.pouch().getAsInt() : "  sac ?")));
                extra.add(ModLanguageManager.get("rune_codex.qty.hint").withStyle(ChatFormatting.GRAY));
                RuneCodexScreen.CraftTarget target = new RuneCodexScreen.CraftTarget(RuneCodexScreen.CraftKind.SYNTH, rune, null, null);
                drafts.add(new RuneCodexScreen.CardDraft(RuneItems.rune(rune), name, price, List.of(), extra, true, this.craftClickHandler(target)));
            }
        }

        return drafts;
    }

    private List<RuneCodexScreen.CardDraft> wordCards(LocalPlayer player) {
        List<RuneCodexScreen.CardDraft> drafts = new ArrayList<>();

        for (RuneCatalog.WordDef word : RuneCatalog.words()) {
            Component name = Component.translatable(word.nameKey());
            List<ItemStack> ingredients = new ArrayList<>();

            for (RuneCatalog.Ingredient ingredient : word.ingredients()) {
                RuneCatalog.byId(ingredient.runeId()).ifPresent(rune -> {
                    ItemStack stack = RuneItems.rune(rune);
                    stack.setCount(ingredient.count());
                    ingredients.add(stack);
                });
            }

            List<Component> extra = new ArrayList<>();
            extra.add(name.copy().withStyle(ChatFormatting.BOLD));
            this.addIngredientLines(extra, player, word.ingredients());
            extra.add(ModLanguageManager.get("rune_codex.word.confirm").withStyle(ChatFormatting.GRAY));
            extra.add(ModLanguageManager.get("rune_codex.qty.hint").withStyle(ChatFormatting.GRAY));
            RuneCodexScreen.CraftTarget target = new RuneCodexScreen.CraftTarget(RuneCodexScreen.CraftKind.WORD, null, word, null);
            drafts.add(new RuneCodexScreen.CardDraft(RuneItems.word(word), name, Component.empty(), ingredients, extra, true, this.craftClickHandler(target)));
        }

        return drafts;
    }

    private List<RuneCodexScreen.CardDraft> arrowCards(LocalPlayer player) {
        List<RuneCodexScreen.CardDraft> drafts = new ArrayList<>();

        for (RuneCatalog.ArrowDef arrow : RuneCatalog.arrows()) {
            MutableComponent name = Component.translatable(arrow.nameKey());
            List<ItemStack> ingredients = new ArrayList<>();
            ingredients.add(RuneItems.craftBaseArrow(arrow));

            for (RuneCatalog.Ingredient ingredient : arrow.runes()) {
                RuneCatalog.byId(ingredient.runeId()).ifPresent(rune -> {
                    ItemStack stack = RuneItems.rune(rune);
                    stack.setCount(ingredient.count());
                    ingredients.add(stack);
                });
            }

            List<Component> extra = new ArrayList<>();
            extra.add(name.copy().withStyle(ChatFormatting.BOLD));
            ItemStack baseArrow = RuneItems.craftBaseArrow(arrow);
            int baseHave = RuneItems.countCraftBaseArrows(player, arrow);
            extra.add(baseArrow.getHoverName().copy().append(" ").append(this.stockLabel(baseHave, arrow.baseArrowCount(), OptionalInt.empty())));
            this.addIngredientLines(extra, player, arrow.runes());
            if (arrow.priorArrowKey() != null) {
                extra.add(Component.translatable(arrow.priorArrowKey()).withStyle(ChatFormatting.GRAY));
            }

            extra.add(ModLanguageManager.get("rune_codex.qty.hint").withStyle(ChatFormatting.GRAY));
            RuneCodexScreen.CraftTarget target = new RuneCodexScreen.CraftTarget(RuneCodexScreen.CraftKind.ARROW, null, null, arrow);
            drafts.add(new RuneCodexScreen.CardDraft(RuneItems.arrow(arrow), name, Component.empty(), ingredients, extra, true, this.craftClickHandler(target)));
        }

        return drafts;
    }

    private List<RuneCodexScreen.Draft> otherRows(int rowW) {
        List<RuneCodexScreen.Draft> drafts = new ArrayList<>();

        for (RuneCatalog.InfoDef info : RuneCatalog.others()) {
            MutableComponent name = Component.translatable(info.nameKey());
            if (info.id().startsWith("esc_")) {
                name.append(" " + info.id().substring(4));
            }

            List<Component> caption = new ArrayList<>();
            caption.add(name);
            Component recipe = pouchRecipe(info.id());
            if (recipe != null) {
                caption.add(recipe);
            }

            if (info.tipKey() != null) {
                caption.add(Component.translatable(info.tipKey()).withStyle(ChatFormatting.GRAY));
            }

            List<Component> extra = new ArrayList<>();
            extra.add(name.copy().withStyle(ChatFormatting.BOLD));
            if (recipe != null) {
                extra.add(recipe);
            }

            if (info.tipKey() != null) {
                extra.add(Component.translatable(info.tipKey()).withStyle(ChatFormatting.GRAY));
            }

            extra.add(ModLanguageManager.get("rune_codex.hopper_only").withStyle(ChatFormatting.GRAY));
            extra.add(ModLanguageManager.get("rune_codex.auto_arrows_only").withStyle(ChatFormatting.DARK_GRAY));
            int height = RuneRecipeRow.preferredHeight(rowW, 0, List.of());
            RuneRecipeRow.Slot result = new RuneRecipeRow.Slot(RuneItems.other(info), extra, true);
            drafts.add(new RuneCodexScreen.Draft(result, List.of(), caption, List.of(), false, () -> {}, height));
        }

        return drafts;
    }

    private static Component pouchRecipe(String id) {
        return switch (id) {
            case "rune_bundle" -> Component.literal("Ra + Ust + ").append(Component.translatable("att2.misc.bundle")).append(" + Nym + Ehl");
            case "spell_bundle_1" -> Component.literal("Tha + Org + ").append(Component.translatable("att2.misc.bundle")).append(" + Ra + Inu");
            case "spell_bundle_2" -> Component.literal("Von + Ave + ").append(Component.translatable("att2.misc.spell_bundle_1.name")).append(" + For + Wej");
            case "spell_bundle_3" -> Component.literal("Da + Ave + ").append(Component.translatable("att2.misc.spell_bundle_2.name")).append(" + Ra + Bex");
            default -> null;
        };
    }

    private void addIngredientLines(List<Component> tooltip, LocalPlayer player, List<RuneCatalog.Ingredient> ingredients) {
        for (RuneCatalog.Ingredient ingredient : ingredients) {
            RuneCatalog.byId(ingredient.runeId())
                .ifPresent(
                    rune -> {
                        RuneStock.Counts counts = RuneStock.counts(player, rune);
                        tooltip.add(
                            Component.translatable(rune.translationKey())
                                .append(" ")
                                .append(this.stockLabel(counts.inventory(), ingredient.count(), counts.pouch()))
                        );
                    }
                );
        }
    }

    private Component stockLabel(int have, int need, OptionalInt pouch) {
        String inv = ModLanguageManager.format("rune_codex.inv", "have", have, "need", need);
        String bag = pouch.isPresent()
            ? ModLanguageManager.format("rune_codex.pouch", "n", pouch.getAsInt())
            : ModLanguageManager.getString("rune_codex.pouch.unknown");
        return Component.literal(inv + " · " + bag).withStyle(this.stockColor(have, need, pouch));
    }

    private ChatFormatting stockColor(int have, int need, OptionalInt pouch) {
        return RuneCardLogic.stockColor(have, need, pouch);
    }

    private CraftClickHandler craftClickHandler(RuneCodexScreen.CraftTarget target) {
        return (button, shift) -> this.onCraftCardClick(target, button, shift);
    }

    private void onCraftCardClick(RuneCodexScreen.CraftTarget target, int button, boolean shift) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            int max = this.maxForCraft(player, target);
            if (max == 0) {
                this.tipCantCraft(target);
            } else if (button == 0) {
                this.runCraft(target, 1);
            } else {
                if (button == 1) {
                    this.qtyMenu.open(max, this.lastClickX, this.lastClickY, q -> this.runCraft(target, q));
                }
            }
        }
    }

    private void tipCantCraft(RuneCodexScreen.CraftTarget target) {
        String tipKey = target.kind() == RuneCodexScreen.CraftKind.SYNTH ? "rune_codex.qty.cant" : "rune_codex.auto.missing";
        ModToast.showError(ModLanguageManager.get(tipKey));
    }

    private void runCraft(RuneCodexScreen.CraftTarget target, int qty) {
        if (qty >= 1) {
            LocalPlayer player = Minecraft.getInstance().player;
            switch (target.kind()) {
                case SYNTH:
                    RuneCatalog.RuneDef rune = target.rune();
                    RuneCodexModel.get().craft(rune.craftTrigger(), qty, false, player == null ? null : () -> this.maxForSynth(player, rune) >= 1);
                    break;
                case WORD:
                    RuneCatalog.WordDef word = target.word();
                    RuneCodexModel.get().craft(word.trigger(), qty, true, player == null ? null : () -> this.maxForWord(player, word) >= 1);
                    break;
                case ARROW:
                    WorkshopHopperCraft.craftArrow(target.arrow(), qty);
            }
        }
    }

    private int maxForCraft(LocalPlayer player, RuneCodexScreen.CraftTarget target) {
        return switch (target.kind()) {
            case SYNTH -> this.maxForSynth(player, target.rune());
            case WORD -> this.maxForWord(player, target.word());
            case ARROW -> this.maxForArrow(player, target.arrow());
        };
    }

    private int maxForSynth(LocalPlayer player, RuneCatalog.RuneDef rune) {
        int powderNeed = RuneStock.price(rune).orElse(0);
        int escNeed = RuneStock.escPrice(rune).orElse(0);
        return CraftQuantity.maxSynth(RuneStock.powder().orElse(0), powderNeed, RuneStock.esc(player), escNeed);
    }

    private int maxForWord(LocalPlayer player, RuneCatalog.WordDef word) {
        int n = word.ingredients().size();
        int[] have = new int[n];
        int[] need = new int[n];

        for (int i = 0; i < n; i++) {
            RuneCatalog.Ingredient ing = word.ingredients().get(i);
            need[i] = ing.count();
            have[i] = RuneCatalog.byId(ing.runeId()).map(r -> RuneStock.countInventory(player, r)).orElse(0);
        }

        return CraftQuantity.maxWords(have, need);
    }

    private int maxForArrow(LocalPlayer player, RuneCatalog.ArrowDef arrow) {
        int[] have = new int[arrow.runes().size()];
        int[] need = new int[arrow.runes().size()];

        for (int i = 0; i < arrow.runes().size(); i++) {
            RuneCatalog.Ingredient ing = arrow.runes().get(i);
            need[i] = ing.count();
            have[i] = RuneCatalog.byId(ing.runeId()).map(r -> RuneStock.countInventory(player, r)).orElse(0);
        }

        return CraftQuantity.maxArrows(RuneItems.countCraftBaseArrows(player, arrow), arrow.baseArrowCount(), have, need, 5, 99);
    }

    private Component labeledCost(String key, int have, OptionalInt need) {
        String shown = ModLanguageManager.format("rune_codex.inv", "have", have, "need", need.isPresent() ? need.getAsInt() : "?");
        ChatFormatting color = need.isPresent() ? this.stockColor(have, need.getAsInt(), OptionalInt.of(0)) : ChatFormatting.RED;
        return Component.literal(ModLanguageManager.format(key, "n", shown)).withStyle(color);
    }

    private int stockHash() {
        LocalPlayer player = Minecraft.getInstance().player;
        int hash = RuneStock.powder().orElse(Integer.MIN_VALUE);
        hash = 31 * hash + RuneStock.esc(player);

        for (RuneCatalog.RuneDef rune : RuneCatalog.allRunes()) {
            hash = 31 * hash + RuneStock.countInventory(player, rune);
            hash = 31 * hash + RuneStock.pouch(player, rune).orElse(-1);
            hash = 31 * hash + (RuneStock.unlocked(rune) ? 1 : 0);
            hash = 31 * hash + RuneStock.price(rune).orElse(-1);
            hash = 31 * hash + RuneStock.escPrice(rune).orElse(-1);
        }

        for (RuneCatalog.BonusDef bonus : RuneCatalog.bonuses()) {
            hash = 31 * hash + RuneStock.bonus(bonus.holder()).orElse(Integer.MIN_VALUE);
        }

        return hash;
    }

    private static String tabLangKey(RuneCatalog.Tab tab) {
        return switch (tab) {
            case SYNTH_LOW -> "rune_codex.tab.low";
            case SYNTH_MID -> "rune_codex.tab.mid";
            case SYNTH_HIGH -> "rune_codex.tab.high";
            case WORDS -> "rune_codex.tab.words";
            case ARROWS -> "rune_codex.tab.arrows";
            case OTHER -> "rune_codex.tab.other";
            case BONUS -> "rune_codex.tab.bonus";
        };
    }

    @Override
    public void tick() {
        super.tick();
        int hash = this.stockHash();
        if (hash != this.lastStockHash) {
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

            if (child instanceof RuneCardButton card && ShopHover.showsTooltip(card.isHovered(), card.isFocused())) {
                this.showTooltip(graphics, card.tooltipLines(), mouseX, mouseY);
                break;
            }
        }

        int innerX = 62;
        int innerW = 640 - TYPE.insetLeft - TYPE.insetRight;
        int titleY = 26;
        this.drawShopTitle(graphics, ModLanguageManager.getString("rune_codex.title"), innerX, titleY, TYPE.titleColor);
        this.drawHeaderBalances(graphics, innerX, innerW, titleY);
        if (this.tab == RuneCatalog.Tab.BONUS) {
            this.renderBonusLines(graphics);
        }

        for (RuneRecipeRow row : this.rows) {
            if (row.isMouseOver(mouseX, mouseY)) {
                List<Component> tip = row.tooltipAt(mouseX, mouseY);
                if (!tip.isEmpty()) {
                    this.showTooltip(graphics, tip, mouseX, mouseY);
                    break;
                }
            }
        }

        this.qtyMenu.render(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean clickContent(MouseButtonEvent event, boolean doubleClick) {
        this.lastClickX = (int)event.x();
        this.lastClickY = (int)event.y();
        if (this.qtyMenu.isOpen()) {
            if (this.qtyMenu.mouseClicked(event.x(), event.y(), event.button())) {
                return true;
            }

            this.qtyMenu.close();
            return true;
        } else {
            return super.clickContent(event, doubleClick);
        }
    }

    private void drawShopTitle(GuiGraphics graphics, String title, int x, int y, int color) {
        ShopTheme.text(graphics, Component.literal(title), x, y, 360, 1.5F, color, false);
    }

    private void drawHeaderBalances(GuiGraphics graphics, int innerX, int innerW, int titleY) {
        LocalPlayer player = Minecraft.getInstance().player;
        String powder = RuneStock.powder().isPresent()
            ? ModLanguageManager.format("rune_codex.powder", "n", RuneStock.powder().getAsInt())
            : ModLanguageManager.format("rune_codex.powder", "n", "?");
        String esc = ModLanguageManager.format("rune_codex.esc", "n", RuneStock.esc(player));
        ShopSkin.texture(graphics, TYPE, "wallet", 454, 17, 140, 37, 232, 70);
        ShopTheme.text(graphics, Component.literal(powder), 465, 23, 116, 0.85F, -3622752, false);
        ShopTheme.text(graphics, Component.literal(esc), 465, 38, 116, 0.85F, -1541639, false);
    }

    private void renderBonusLines(GuiGraphics graphics) {
        int rowY = 92;

        for (RuneCatalog.BonusDef bonus : RuneCatalog.bonuses()) {
            Component name = ModLanguageManager.get(bonus.langKey());
            OptionalInt value = RuneStock.bonus(bonus.holder());
            String amount = value.isPresent() ? value.getAsInt() + bonus.suffix() : "?";
            graphics.drawString(this.font, name.getString() + " " + amount, 164, rowY, -13096159, false);
            rowY += 10;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY != 0.0) {
            if (this.usesCards()) {
                int count = RuneCardLogic.pageCount(this.buildAllCards().size());
                if (count <= 1) {
                    return true;
                }

                int next = ShopPaging.afterScroll(this.page, count, deltaY);
                if (next != this.page) {
                    this.page = next;
                    this.rebuild(false);
                }

                return true;
            } else {
                int bodyH = 238;
                int rowW = 430;
                List<RuneCodexScreen.Draft> all = this.buildAllRows(rowW);
                List<int[]> ranges = this.pageRanges(all, bodyH);
                if (ranges.size() <= 1) {
                    return true;
                }

                int next = ShopPaging.afterScroll(this.page, ranges.size(), deltaY);
                if (next != this.page) {
                    this.page = next;
                    this.clampPage(ranges);
                    this.pageCount = ranges.size();
                    this.rebuild(false);
                }

                return true;
            }
        } else {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (this.qtyMenu.keyEscape()) {
                return true;
            }

            this.onClose();
            return true;
        } else {
            return super.keyPressed(event);
        }
    }

    private record CardDraft(
        ItemStack result,
        Component name,
        Component price,
        List<ItemStack> ingredients,
        List<Component> extraTooltip,
        boolean clickable,
        CraftClickHandler onCraft
    ) {
    }

    private enum CraftKind {
        SYNTH,
        WORD,
        ARROW;
    }

    private record CraftTarget(RuneCodexScreen.CraftKind kind, RuneCatalog.RuneDef rune, RuneCatalog.WordDef word, RuneCatalog.ArrowDef arrow) {
    }

    private record Draft(
        RuneRecipeRow.Slot result,
        List<RuneRecipeRow.Slot> ingredients,
        List<Component> caption,
        List<RuneRecipeRow.StockLine> stock,
        boolean clickable,
        Runnable onCraft,
        int height
    ) {
    }
}
