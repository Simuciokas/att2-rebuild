package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyEnabler;
import fr.poubone.att2.client.gambling.CharlesScreen;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.miner.MinerCatalog;
import fr.poubone.att2.client.rune.RuneCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowItem;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShopModel {
    public static final int TRIGGER_RESET = 423;
    public static final int TRIGGER_MENDING_PRICE = 413;
    public static final int TRIGGER_MENDING_REPAIR = 414;
    public static final int TRIGGER_MENDING_TOOLS = 415;
    public static final int TRIGGER_TOOL_FIRST = 406;
    public static final int TRIGGER_TOOL_LAST = 412;
    private static final int COLLECT_TICKS = 18;
    private static final int OPEN_AFTER_TICKS = 7;
    private static final Pattern CATEGORY_HEADER = Pattern.compile("(?:<-°->|<=>|<->)\\s*(.+?)\\s*(?:<-°->|<=>|<->)");
    private static final Pattern MENDING_RARITY = Pattern.compile(
        "(?i)(commun|peu commun|uncommon|rare|souverain|sovereign|epique|épique|epic|legendaire|légendaire|legendary|supreme|suprême|common)\\s*:"
    );
    private static final ShopModel INSTANCE = new ShopModel();
    private final ShopCatalog catalog = new ShopCatalog();
    private final List<ShopOffer> repairOffers = new ArrayList<>();
    private final List<Component> repairLines = new ArrayList<>();
    private String currentCategory = "";
    private int collectTicks;
    private int openAfterTicks = -1;
    private int version;
    private boolean openedThisBatch;
    private ShopSeller seller;
    private int lastOpeningTrigger = -1;
    private boolean repairView;
    private Component repairTitle;
    private long lastNpcOpenMs;
    private int suppressShopTicks;

    private ShopModel() {
    }

    public static ShopModel get() {
        return INSTANCE;
    }

    public void reset() {
        this.catalog.clear();
        this.catalog.clearLock();
        this.currentCategory = "";
        this.collectTicks = 0;
        this.openAfterTicks = -1;
        this.version = 0;
        this.openedThisBatch = false;
        this.seller = null;
        this.lastOpeningTrigger = -1;
        this.suppressShopTicks = 0;
        this.exitRepairView();
    }

    public void tick(Minecraft client) {
        if (this.suppressShopTicks > 0) {
            this.suppressShopTicks--;
        }

        if (this.collectTicks > 0 && --this.collectTicks == 0) {
            this.catalog.inferType();
            this.version++;
        }

        if (this.openAfterTicks > 0 && --this.openAfterTicks == 0) {
            this.maybeOpen();
        }
    }

    public boolean onSystemMessage(Component message) {
        return this.onSystemMessage(message, true);
    }

    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) {
            return false;
        }

        try {
            return this.handleMessage(message);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean handleMessage(Component message) {
        int opening = extractShopOpeningTrigger(message);
        if (opening >= 0) {
            ShopSeller known = ShopSellers.byTrigger(opening);
            if (known != null && !known.gambling() && !menuEnabled(known.type())) {
                this.suppressShopTicks = 40;
                return false;
            }

            this.suppressShopTicks = 0;
            this.lockSeller(opening);
            if (opening != 730) {
                Att2Triggers.send(opening);
            }
        }

        if (this.suppressShopTicks <= 0
            || !this.looksLikeShop(message) && !isMendingProposal(message) && !this.isRepairOverlay(message) && !isMendingLine(message.getString())) {
            if (collectTriggers(message).stream().anyMatch(RuneCatalog::isWorkshopTrigger)) {
                return false;
            }

            if (this.collectTicks == 0 && !isScreenShowing() && isPowderStock(message.getString(), message)) {
                return false;
            }

            if (isGambling(message) || isQuestNoise(message) || isPrisonerCatalog(message) || isEnchantmentCatalog(message)) {
                return false;
            }

            if (isResetConfirmation(message)) {
                return false;
            }

            if (isMerchandiseInvitation(message, opening)) {
                return false;
            }

            if (isMendingProposal(message)) {
                this.ingestActions(message);
                this.catalog.inferType();
                this.version++;
                return isScreenShowing();
            }

            if (!this.isRepairOverlay(message) && (!this.repairView || !isMendingLine(message.getString()))) {
                boolean shopLike = this.looksLikeShop(message);
                if (!shopLike) {
                    return false;
                }

                if (this.collectTicks == 0) {
                    if (isScreenShowing() && this.seller != null) {
                        this.catalog.clearOffers();
                        this.currentCategory = "";
                        this.exitRepairView();
                    } else {
                        this.catalog.clear();
                        this.currentCategory = "";
                        this.openedThisBatch = false;
                        this.exitRepairView();
                    }
                }

                boolean accepted = this.ingest(message);
                if (!accepted) {
                    return false;
                }

                this.collectTicks = 18;
                this.catalog.inferType();
                this.version++;
                if (!this.catalog.isEmpty() && this.activeMenuEnabled() && canAutoOpen()) {
                    ShopScreen.open();
                    this.openedThisBatch = true;
                    this.openAfterTicks = -1;
                }

                return isScreenShowing();
            } else {
                this.ingestRepair(message);
                this.version++;
                if (this.activeMenuEnabled() && canAutoOpen() && !this.catalog.isEmpty()) {
                    ShopScreen.open();
                }

                return isScreenShowing();
            }
        } else {
            return false;
        }
    }

    private boolean ingest(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find()) {
            String title = header.group(1).trim();
            if (isRepairHeader(title)) {
                this.enterRepairView(Component.literal(title));
                return true;
            } else {
                this.currentCategory = title;
                this.catalog.addCategory(this.currentCategory);
                return true;
            }
        } else {
            if (isDiscount(raw, message)) {
                this.catalog.setDiscount(stripClicks(message));
                return true;
            }

            if (isPowderStock(raw, message)) {
                this.catalog.setPowderStock(stripClicks(message));
                return true;
            }

            if (isRemaining(raw, message)) {
                if (!ShopTellraws.isForceResetAction(raw)) {
                    this.catalog.setRemaining(stripClicks(message));
                }

                this.ingestActions(message);
                return true;
            } else {
                List<ItemStack> items = new ArrayList<>();
                List<Component> names = new ArrayList<>();
                List<Integer> itemOrder = new ArrayList<>();
                List<Integer> triggers = new ArrayList<>();
                List<Component> triggerLabels = new ArrayList<>();
                int[] index = new int[]{0};
                visit(message, part -> {
                    if (part.getStyle().getHoverEvent() instanceof ShowItem show) {
                        ItemStack stack = show.item();
                        if (stack != null && !stack.isEmpty()) {
                            items.add(stack.copy());
                            names.add(stripClicks(part));
                            itemOrder.add(index[0]);
                        }
                    }

                    if (part.getStyle().getClickEvent() instanceof RunCommand run) {
                        int triggerx = Att2Triggers.parseTriggerCommand(run.command());
                        if (triggerx >= 0 && !isGamblingTrigger(triggerx)) {
                            triggers.add(triggerx);
                            triggerLabels.add(stripClicks(part));
                        }
                    }

                    index[0]++;
                });
                Component price = findPrice(message);
                boolean added = false;
                if (!items.isEmpty()) {
                    for (int i = 0; i < items.size(); i++) {
                        int trigger = triggerAfter(triggers, itemOrder.get(i), i);
                        if (trigger >= 0) {
                            String key = translationKey(names.get(i));
                            Component fallbackPrice = triggerLabels.isEmpty() ? Component.empty() : triggerLabels.get(Math.min(i, triggerLabels.size() - 1));
                            Identifier sprite = customSprite(findSprite(message));
                            this.catalog
                                .addOffer(
                                    new ShopOffer(
                                        items.get(i),
                                        names.get(i),
                                        price == null ? fallbackPrice : price,
                                        trigger,
                                        this.currentCategory,
                                        key,
                                        sprite,
                                        findShowText(message)
                                    )
                                );
                            added = true;
                        }
                    }
                } else if (!triggers.isEmpty() && !isSpecialOnly(triggers)) {
                    added = this.ingestNamedOffer(message, triggers.getFirst(), price);
                }

                if (this.ingestActions(message)) {
                    added = true;
                }

                return added;
            }
        }
    }

    private boolean ingestNamedOffer(Component message, int trigger, Component price) {
        if (!isGamblingTrigger(trigger)
            && (kindOfTrigger(trigger, message, message) == null || kindOfTrigger(trigger, message, message) == ShopAction.Kind.OTHER)) {
            Identifier sprite = findSprite(message);
            Component name = findAngleName(message);
            if (name == null) {
                name = findShopName(message);
            }

            if (name == null && sprite == null) {
                return false;
            }

            if (name == null) {
                name = Component.literal("?");
            }

            Component lore = findShowText(message);
            ItemStack fake = fakeStackFor(sprite, name);
            String key = translationKey(name);
            this.catalog
                .addOffer(
                    new ShopOffer(
                        fake, name, price == null ? Component.empty() : price, trigger, this.currentCategory, key, ShopIcons.keepNamedSprite(sprite), lore
                    )
                );
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSpecialOnly(List<Integer> triggers) {
        for (int trigger : triggers) {
            if (trigger != 423 && trigger != 413 && trigger != 414 && trigger != 415 && (trigger < 416 || trigger > 422)) {
                return false;
            }
        }

        return !triggers.isEmpty();
    }

    private boolean ingestActions(Component message) {
        boolean added = false;

        for (Component part : flatten(message)) {
            if (part.getStyle().getClickEvent() instanceof RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0 && !isGamblingTrigger(trigger)) {
                    ShopAction.Kind kind = kindOfTrigger(trigger, part, message);
                    if (kind != null) {
                        Component tip = hoverText(part);
                        this.catalog.addAction(new ShopAction(trigger, actionLabel(part, kind), tip, kind));
                        added = true;
                    }
                }
            }
        }

        return added;
    }

    private static ShopAction.Kind kindOfTrigger(int trigger, Component part, Component root) {
        if (trigger == 423) {
            return ShopAction.Kind.RESET;
        } else if (trigger == 413) {
            return ShopAction.Kind.MENDING_PRICE;
        } else if (trigger == 414) {
            return ShopAction.Kind.MENDING_REPAIR;
        } else if (trigger == 415) {
            return ShopAction.Kind.MENDING_TOOLS;
        } else if (trigger >= 416 && trigger <= 422) {
            return ShopAction.Kind.MENDING_SLOT;
        } else {
            String key = translationKey(part);
            if (key.startsWith("att2.shop.mending")) {
                return ShopAction.Kind.MENDING_SLOT;
            } else if (part.getStyle().getHoverEvent() instanceof ShowItem) {
                return null;
            } else if (containsKey(root, "att2.shop.chronotons.buy")
                || containsKey(root, "att2.shop.hover_event.buy")
                || containsKey(root, "att2.shop.esc.buy")
                || containsKey(root, "att2.shop.esc.hover_event.buy")) {
                return null;
            } else {
                return looksLikePurchase(root) ? null : ShopAction.Kind.OTHER;
            }
        }
    }

    private static boolean looksLikePurchase(Component message) {
        String text = message.getString().toLowerCase(Locale.ROOT);
        return text.contains("chronoton")
            || text.contains(" esc")
            || text.contains("[") && text.contains("esc")
            || text.contains("poudre")
            || text.contains("runic")
            || text.contains("runen")
            || text.contains("[") && text.contains("]");
    }

    private static Component actionLabel(Component part, ShopAction.Kind kind) {
        return switch (kind) {
            case RESET -> Component.translatable("shop.action.reset");
            case BACK -> Component.translatable("shop.action.back");
            case MENDING_PRICE -> Component.translatable("shop.action.mending_price");
            case MENDING_REPAIR -> Component.translatable("shop.action.mending_repair");
            case MENDING_TOOLS -> Component.translatable("shop.action.mending_tools");
            default -> stripClicks(part);
        };
    }

    private static int triggerAfter(List<Integer> triggers, int ignoredOrder, int itemIndex) {
        if (triggers.isEmpty()) {
            return -1;
        } else {
            return itemIndex < triggers.size() ? triggers.get(itemIndex) : triggers.getLast();
        }
    }

    private static Identifier findSprite(Component message) {
        Identifier[] found = new Identifier[]{null};
        visit(message, part -> {
            if (found[0] == null) {
                if (part.getContents() instanceof ObjectContents object && object.contents() instanceof AtlasSprite sprite) {
                    found[0] = sprite.sprite();
                }
            }
        });
        return found[0];
    }

    private static Component findAngleName(Component message) {
        Component[] found = new Component[]{null};
        visit(message, part -> {
            if (found[0] == null) {
                if (part.getContents() instanceof PlainTextContents plain) {
                    String text = plain.text().strip();
                    if (text.length() >= 3 && text.startsWith("<") && text.endsWith(">")) {
                        found[0] = Component.literal(text).withStyle(part.getStyle().withClickEvent(null));
                    }
                }
            }
        });
        return found[0];
    }

    private static Component findShopName(Component message) {
        Component[] found = new Component[]{null};
        visit(message, part -> {
            if (found[0] == null) {
                if (part.getContents() instanceof TranslatableContents translatable) {
                    String key = translatable.getKey();
                    if (isItemNameKey(key)) {
                        found[0] = stripClicks(part);
                    }
                }
            }
        });
        return found[0];
    }

    private static Component findShowText(Component message) {
        Component[] found = new Component[]{null};
        visit(message, part -> {
            if (found[0] == null) {
                if (part.getStyle().getHoverEvent() instanceof ShowText show) {
                    found[0] = show.value();
                }
            }
        });
        return found[0];
    }

    private static ItemStack fakeStackFor(Identifier sprite, Component name) {
        if (ShopIcons.spellId(translationKey(name), sprite) > 0) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        } else {
            ItemStack fromSprite = ShopIcons.itemFromSprite(sprite);
            if (fromSprite != null) {
                return fromSprite;
            } else {
                String path = sprite == null ? "" : sprite.getPath().toLowerCase(Locale.ROOT);
                String text = (name == null ? "" : name.getString() + " " + translationKey(name)).toLowerCase(Locale.ROOT);
                if (path.contains("spell") || text.contains("spell") || text.contains("dahal") || text.contains("rayon")) {
                    return new ItemStack(Items.ENCHANTED_BOOK);
                } else if (path.contains("horse")
                    || text.contains("horse")
                    || text.contains("camel")
                    || text.contains("ride")
                    || text.contains("monture")
                    || text.contains("pig")
                    || text.contains("mule")
                    || text.contains(".pig.")) {
                    return new ItemStack(Items.SADDLE);
                } else if (text.contains("fish")
                    || text.contains("poisson")
                    || text.contains("fishing")
                    || text.contains("bait")
                    || path.contains("fishing")
                    || path.contains("bait")) {
                    return new ItemStack(Items.FISHING_ROD);
                } else if (path.contains("item/rabbit") || text.contains("shop.rabbit")) {
                    return new ItemStack(Items.RABBIT);
                } else if (path.contains("item/chicken") || text.contains("shop.chicken")) {
                    return new ItemStack(Items.CHICKEN);
                } else if (path.contains("item/pork") || text.contains("shop.pork")) {
                    return new ItemStack(Items.PORKCHOP);
                } else if (path.contains("item/beef") || text.contains("shop.beef")) {
                    return new ItemStack(Items.BEEF);
                } else if (path.contains("item/mutton") || text.contains("shop.mutton")) {
                    return new ItemStack(Items.MUTTON);
                } else if (path.contains("coal") || text.contains("coal") || text.contains("charbon")) {
                    return new ItemStack(Items.COAL);
                } else if (path.contains("torch") || text.contains("torch") || text.contains("torche")) {
                    return new ItemStack(Items.TORCH);
                } else if (path.contains("scaffold") || text.contains("chair") || text.contains("chaise")) {
                    return new ItemStack(Items.SCAFFOLDING);
                } else if (path.contains("bundle") || text.contains("bundle")) {
                    return new ItemStack(Items.BUNDLE);
                } else if (path.contains("carrot_on_a_stick") || text.contains("carrot_on_a_stick")) {
                    return new ItemStack(Items.CARROT_ON_A_STICK);
                } else if (path.contains("map") || text.contains(".map.") || text.contains("sylberland")) {
                    return new ItemStack(Items.MAP);
                } else {
                    return !text.contains("chronoton") && !path.contains("diamond") ? new ItemStack(Items.BOOK) : new ItemStack(Items.GOLD_NUGGET);
                }
            }
        }
    }

    static Identifier customSprite(Identifier sprite) {
        if (sprite == null) {
            return null;
        }

        String path = sprite.getPath().toLowerCase(Locale.ROOT);
        return !path.contains("custom") && !path.contains("spell") && !path.contains("bait") && !path.contains("fishing") ? null : sprite;
    }

    private static boolean isItemNameKey(String key) {
        if (key != null && !key.isEmpty()) {
            String lower = key.toLowerCase(Locale.ROOT);
            return !lower.contains("hover")
                    && !lower.contains("mending")
                    && !lower.contains("remain")
                    && !lower.endsWith(".buy")
                    && !lower.contains("discount")
                    && !lower.contains("chronotons.buy")
                ? lower.startsWith("att2.shop.")
                    || lower.startsWith("att2.misc.")
                    || lower.startsWith("att2.fishing.")
                    || lower.startsWith("weapon")
                    || lower.startsWith("armor")
                    || lower.contains("potion")
                    || lower.contains("spell")
                    || lower.contains("horse")
                    || lower.contains("pig")
                    || lower.contains("mule")
                    || lower.contains("camel")
                : false;
        } else {
            return false;
        }
    }

    static Identifier spriteTexture(Identifier sprite) {
        if (sprite == null) {
            return null;
        }

        String path = sprite.getPath();
        return path.startsWith("textures/") && path.endsWith(".png")
            ? sprite
            : Identifier.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + path + ".png");
    }

    private static Component findPrice(Component message) {
        Component[] found = new Component[]{null};
        visit(
            message,
            part -> {
                if (found[0] == null) {
                    if (part.getContents() instanceof TranslatableContents translatable
                        && (
                            translatable.getKey().equals("att2.shop.chronotons.buy")
                                || translatable.getKey().contains("shop.chronoton")
                                || translatable.getKey().contains("shop.esc")
                                || translatable.getKey().contains("runepowder")
                        )) {
                        found[0] = stripClicks(part);
                    }
                }
            }
        );
        if (found[0] != null) {
            return found[0];
        }

        visit(
            message,
            part -> {
                if (found[0] == null) {
                    String text = part.getString();
                    String lower = text.toLowerCase(Locale.ROOT);
                    if ((
                            lower.contains("chronoton")
                                || lower.contains(" runes")
                                || lower.contains("esc")
                                || lower.contains("poudre")
                                || lower.contains("powder")
                                || lower.contains("runen")
                                || text.contains("[") && text.matches(".*\\d+.*")
                        )
                        && part.getStyle().getClickEvent() instanceof RunCommand) {
                        found[0] = stripClicks(part);
                    }
                }
            }
        );
        return found[0];
    }

    private boolean looksLikeShop(Component message) {
        if (containsKeyPrefix(message, "att2.shop.")) {
            return true;
        }

        if (containsKeyPrefix(message, "att2.misc.") && hasShopTrigger(message)) {
            return true;
        }

        if (containsKeyPrefix(message, "att2.fishing.") && hasShopTrigger(message)) {
            return true;
        }

        if (!containsKeyPrefix(message, "weapon") && !containsKeyPrefix(message, "armor")) {
            String raw = message.getString();
            if (raw.contains("<-°->")) {
                return true;
            }

            if (isDiscount(raw, message) || isRemaining(raw, message) || isPowderStock(raw, message)) {
                return true;
            }

            if (hasShowItem(message) && hasShopTrigger(message)) {
                return true;
            }

            if (!hasShopTrigger(message) || findSprite(message) == null && !looksLikePurchase(message)) {
                for (int trigger : collectTriggers(message)) {
                    if (trigger == 423 || trigger == 413 || trigger == 414 || trigger == 415 || trigger >= 416 && trigger <= 422) {
                        return true;
                    }
                }

                return false;
            } else {
                return true;
            }
        } else {
            return hasShowItem(message) && hasShopTrigger(message);
        }
    }

    private static boolean isPowderStock(String raw, Component message) {
        if (hasShopTrigger(message)) {
            return false;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("poudres runiques")
            || lower.contains("runic powder")
            || lower.contains("runenpulver")
            || lower.contains("polvos r")
            || lower.contains("pós rúnicos")
            || lower.contains("руническ")
            || lower.contains("符文粉末")
            || lower.contains("ルーン")
            || lower.contains("룬")
            || lower.contains("مساحيق الرون")
            || lower.contains("रूनिक")
            || lower.contains("stock total de poudres")
            || lower.contains("total stock runic");
    }

    private static boolean isDiscount(String raw, Component message) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("remise actuelle")
            || lower.contains("current discount")
            || lower.contains("descuento actual")
            || containsKey(message, "att2.shop.discount");
    }

    private static boolean isRemaining(String raw, Component message) {
        return containsKey(message, "att2.shop.remain") ? true : ShopTellraws.isRemainingTimer(raw);
    }

    private static boolean isResetConfirmation(Component message) {
        String raw = message.getString().toLowerCase(Locale.ROOT);
        return raw.contains("magasins réinitialisés") || raw.contains("stores reset") || raw.contains("tiendas reiniciadas");
    }

    private static boolean isGambling(Component message) {
        if (!containsKeyPrefix(message, "matching_game") && !containsKeyPrefix(message, "att2.gambling")) {
            for (int trigger : collectTriggers(message)) {
                if (isGamblingTrigger(trigger)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    static boolean isGamblingTrigger(int trigger) {
        if (trigger == 730 || trigger == 2856) {
            return true;
        } else if (trigger >= 394 && trigger <= 400) {
            return true;
        } else {
            return trigger >= 2844 && trigger <= 2855 ? true : trigger >= 2332 && trigger <= 2843;
        }
    }

    private static boolean isQuestNoise(Component message) {
        return containsKeyPrefix(message, "consciousness.")
            || containsKeyPrefix(message, "att2.mainquest")
            || containsKeyPrefix(message, "att2.dailyquest")
            || containsKeyPrefix(message, "att2.sidequest");
    }

    private static boolean isPrisonerCatalog(Component message) {
        if (!containsKeyPrefix(message, "att2.rune.list")
            && !containsKeyPrefix(message, "att2.rune_powder.list")
            && !containsKeyPrefix(message, "att2.rune_exchange")) {
            for (int trigger : collectTriggers(message)) {
                if (MinerCatalog.isPrisonerGameplayTrigger(trigger)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    private static boolean isEnchantmentCatalog(Component message) {
        return containsKeyPrefix(message, "enchantment.data.") || containsKeyPrefix(message, "enchantment.att2.");
    }

    private static boolean canAutoOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        } else {
            return client.screen instanceof CharlesScreen
                ? false
                : client.screen == null || client.screen instanceof ShopScreen || client.screen instanceof ChatScreen;
        }
    }

    private void maybeOpen() {
        if (this.catalog.isEmpty()) {
            this.openAfterTicks = -1;
        } else if (this.activeMenuEnabled()) {
            if (canAutoOpen()) {
                this.catalog.inferType();
                ShopScreen.open();
                this.openedThisBatch = true;
            }
        }
    }

    private static boolean isScreenShowing() {
        return Minecraft.getInstance().screen instanceof ShopScreen;
    }

    public ShopCatalog catalog() {
        return this.catalog;
    }

    public int version() {
        return this.version;
    }

    public boolean isCollecting() {
        return this.collectTicks > 0;
    }

    public void buy(ShopOffer offer) {
        if (offer != null && offer.trigger() >= 0) {
            Att2Triggers.send(offer.trigger());
            CurrencyEnabler.onTransaction();
        }
    }

    public void run(ShopAction action) {
        if (action != null) {
            if (action.kind() == ShopAction.Kind.BACK) {
                this.exitRepairView();
                if (this.catalog.offers().isEmpty() && this.lastOpeningTrigger > 0) {
                    Att2Triggers.send(this.lastOpeningTrigger);
                }

                this.version++;
            } else if (action.trigger() >= 0) {
                if (action.kind() != ShopAction.Kind.RESET && action.trigger() != 423) {
                    Att2Triggers.send(action.trigger());
                    CurrencyEnabler.onTransaction();
                } else {
                    this.startCatalogRefresh();
                    Att2Triggers.send(423);
                    CurrencyEnabler.onTransaction();
                    if (this.lastOpeningTrigger > 0) {
                        Att2Triggers.send(this.lastOpeningTrigger);
                    }
                }
            }
        }
    }

    private void startCatalogRefresh() {
        this.catalog.clearOffers();
        this.currentCategory = "";
        this.exitRepairView();
        this.collectTicks = 18;
        this.openedThisBatch = true;
        this.version++;
    }

    public String menuTypeId() {
        return this.seller != null && !this.seller.gambling() ? this.seller.type().id : this.catalog.type().id;
    }

    private static boolean menuEnabled(ShopType type) {
        return type != null && HUDConfig.get().isShopMenuEnabled(type.id);
    }

    private boolean activeMenuEnabled() {
        return this.seller != null && !this.seller.gambling() ? menuEnabled(this.seller.type()) : menuEnabled(this.catalog.type());
    }

    public ShopSeller seller() {
        return this.seller;
    }

    public boolean hasRepair() {
        if (this.catalog.usesEsc()) {
            return false;
        } else {
            return this.seller != null ? this.seller.repair() : this.catalog.type() == ShopType.BLACKSMITH;
        }
    }

    public boolean hasReset() {
        if (this.catalog.usesEsc()) {
            return false;
        }

        if (this.seller != null) {
            return this.seller.reset();
        }

        if (this.catalog.actions().stream().anyMatch(action -> action.kind() == ShopAction.Kind.RESET)) {
            return true;
        }

        ShopType type = this.catalog.type();
        return this.catalog.discount() != null
            || this.catalog.remaining() != null
            || type == ShopType.BLACKSMITH
            || type == ShopType.ALCHEMIST
            || type == ShopType.FLETCHER;
    }

    public boolean isRepairView() {
        return this.repairView;
    }

    public Component repairTitle() {
        return this.repairTitle;
    }

    public List<Component> repairLines() {
        return List.copyOf(this.repairLines);
    }

    public List<ShopOffer> repairOffers() {
        return List.copyOf(this.repairOffers);
    }

    public void exitRepairView() {
        this.repairView = false;
        this.repairTitle = null;
        this.repairLines.clear();
        this.repairOffers.clear();
    }

    public void openFromNpc(ShopSeller target) {
        if (target != null) {
            if (target.gambling() ? HUDConfig.get().charlesAutoOpen : menuEnabled(target.type())) {
                long now = System.currentTimeMillis();
                if (now - this.lastNpcOpenMs >= 400L) {
                    this.lastNpcOpenMs = now;
                    if (target.gambling()) {
                        Att2Triggers.send(target.trigger());
                    } else if (target.trigger() > 0) {
                        this.lockSeller(target.trigger());
                        Att2Triggers.send(target.trigger());
                    }
                }
            }
        }
    }

    void lockSeller(int openingTrigger) {
        ShopSeller next = ShopSellers.byTrigger(openingTrigger);
        if (next == null || !next.gambling()) {
            if (next == null || this.seller != null && this.seller.trigger() == next.trigger()) {
                if (next == null && openingTrigger > 0) {
                    this.lastOpeningTrigger = openingTrigger;
                }
            } else {
                this.seller = next;
                this.lastOpeningTrigger = openingTrigger;
                this.catalog.lockType(next.type());
                this.exitRepairView();
            }
        }
    }

    private void enterRepairView(Component title) {
        if (!this.repairView) {
            this.repairLines.clear();
            this.repairOffers.clear();
        }

        this.repairView = true;
        if (title != null && !title.getString().isBlank()) {
            this.repairTitle = title;
        }
    }

    private void ingestRepair(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find() && isRepairHeader(header.group(1))) {
            this.enterRepairView(Component.literal(header.group(1).trim()));
        } else if (isMendingLine(raw) && collectTriggers(message).isEmpty()) {
            this.enterRepairView(this.repairTitle);
            this.repairLines.add(stripClicks(message));
        } else {
            List<Integer> triggers = collectTriggers(message);
            boolean toolLine = false;

            for (int trigger : triggers) {
                if (isRepairToolTrigger(trigger)) {
                    toolLine = true;
                    break;
                }
            }

            if (!toolLine
                && !raw.toLowerCase(Locale.ROOT).contains("outil")
                && !raw.toLowerCase(Locale.ROOT).contains("repair tool")
                && !raw.toLowerCase(Locale.ROOT).contains("tool price")) {
                if (isMendingLine(raw)) {
                    this.enterRepairView(this.repairTitle);
                    this.repairLines.add(stripClicks(message));
                }
            } else {
                this.enterRepairView(this.repairTitle);
                this.ingestRepairOffer(message, triggers);
            }
        }
    }

    private void ingestRepairOffer(Component message, List<Integer> triggers) {
        List<ItemStack> items = new ArrayList<>();
        List<Component> names = new ArrayList<>();
        int[] index = new int[]{0};
        List<Integer> itemOrder = new ArrayList<>();
        visit(message, part -> {
            if (part.getStyle().getHoverEvent() instanceof ShowItem show) {
                ItemStack stack = show.item();
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.copy());
                    names.add(stripClicks(part));
                    itemOrder.add(index[0]);
                }
            }

            index[0]++;
        });
        Component price = findPrice(message);
        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                int trigger = triggerAfter(triggers, itemOrder.get(i), i);
                if (trigger >= 0) {
                    this.repairOffers.removeIf(existing -> existing.trigger() == trigger);
                    this.repairOffers
                        .add(
                            new ShopOffer(
                                items.get(i),
                                names.get(i),
                                price == null ? Component.empty() : price,
                                trigger,
                                this.currentCategory,
                                translationKey(names.get(i)),
                                customSprite(findSprite(message)),
                                findShowText(message)
                            )
                        );
                }
            }
        } else if (!triggers.isEmpty()) {
            int trigger = triggers.getFirst();
            Identifier sprite = findSprite(message);
            Component name = findAngleName(message);
            if (name == null) {
                name = findShopName(message);
            }

            if (name == null) {
                name = stripClicks(message);
            }

            ItemStack fake = fakeStackFor(sprite, name);
            this.repairOffers.removeIf(existing -> existing.trigger() == trigger);
            this.repairOffers
                .add(
                    new ShopOffer(
                        fake,
                        name,
                        price == null ? Component.empty() : price,
                        trigger,
                        this.currentCategory,
                        translationKey(name),
                        customSprite(sprite),
                        findShowText(message)
                    )
                );
        }
    }

    private static boolean isRepairToolTrigger(int trigger) {
        return trigger >= 406 && trigger <= 412;
    }

    private static boolean isRepairHeader(String title) {
        if (title == null) {
            return false;
        }

        String upper = title.toUpperCase(Locale.ROOT);
        return upper.contains("REPAIR PRICES")
            || title.contains("维修价格")
            || title.toLowerCase(Locale.ROOT).contains("prix de réparation")
            || title.toLowerCase(Locale.ROOT).contains("prix des outils");
    }

    private static boolean isMendingLine(String raw) {
        return raw == null ? false : MENDING_RARITY.matcher(raw).find();
    }

    private boolean isRepairOverlay(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find() && isRepairHeader(header.group(1))) {
            return true;
        }

        if (isMendingLine(raw) && collectTriggers(message).isEmpty()) {
            return true;
        }

        for (int trigger : collectTriggers(message)) {
            if (isRepairToolTrigger(trigger)) {
                return true;
            }
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("outil de réparation") || lower.contains("repair tool") || lower.contains("repairing tool");
    }

    private static boolean isMendingProposal(Component message) {
        List<Integer> triggers = collectTriggers(message);
        boolean mending = false;

        for (int trigger : triggers) {
            if (trigger != 413 && trigger != 414 && trigger != 415) {
                if (!isRepairToolTrigger(trigger)) {
                    return false;
                }
            } else {
                mending = true;
            }
        }

        return !mending ? false : !hasShowItem(message) && findSprite(message) == null;
    }

    private static boolean isMerchandiseInvitation(Component message, int opening) {
        if (opening < 0) {
            return false;
        }

        if (hasShowItem(message)) {
            return false;
        }

        if (containsKeyPrefix(message, "att2.shop.")) {
            return false;
        }

        if (containsKeyPrefix(message, "att2.misc.")) {
            return false;
        }

        if (containsKeyPrefix(message, "att2.fishing.")) {
            return false;
        }

        String raw = message.getString();
        return CATEGORY_HEADER.matcher(raw).find() ? false : !isDiscount(raw, message) && !isRemaining(raw, message) && !isPowderStock(raw, message);
    }

    private static int extractShopOpeningTrigger(Component message) {
        for (int trigger : collectTriggers(message)) {
            if (ShopSellers.isShopOpening(trigger) || trigger == 730) {
                return trigger;
            }
        }

        return -1;
    }

    private static boolean hasShowItem(Component message) {
        boolean[] found = new boolean[]{false};
        visit(message, part -> {
            if (!found[0] && part.getStyle().getHoverEvent() instanceof ShowItem) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean hasShopTrigger(Component message) {
        for (int trigger : collectTriggers(message)) {
            if (!isGamblingTrigger(trigger)) {
                return true;
            }
        }

        return false;
    }

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, part -> {
            if (part.getStyle().getClickEvent() instanceof RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0) {
                    triggers.add(trigger);
                }
            }
        });
        return triggers;
    }

    private static boolean containsKey(Component message, String key) {
        boolean[] found = new boolean[]{false};
        visit(message, part -> {
            if (!found[0] && part.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean containsKeyPrefix(Component message, String prefix) {
        boolean[] found = new boolean[]{false};
        visit(message, part -> {
            if (!found[0] && part.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(prefix)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    static String translationKey(Component component) {
        if (component == null) {
            return "";
        } else if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        } else {
            for (Component sibling : component.getSiblings()) {
                String inner = translationKey(sibling);
                if (!inner.isEmpty()) {
                    return inner;
                }
            }

            if (component.getContents() instanceof TranslatableContents translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component inner) {
                        String key = translationKey(inner);
                        if (!key.isEmpty()) {
                            return key;
                        }
                    }
                }
            }

            return "";
        }
    }

    private static Component hoverText(Component part) {
        return part.getStyle().getHoverEvent() instanceof ShowText show ? show.value() : null;
    }

    private static Component stripClicks(Component component) {
        return component == null ? Component.empty() : component.copy().withStyle(style -> style.withClickEvent(null));
    }

    private static List<Component> flatten(Component component) {
        List<Component> list = new ArrayList<>();
        visit(component, list::add);
        return list;
    }

    private static void visit(Component component, Consumer<Component> visitor) {
        if (component != null) {
            visitor.accept(component);
            if (component.getContents() instanceof TranslatableContents translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component inner) {
                        visit(inner, visitor);
                    }
                }
            }

            if (component.getContents() instanceof PlainTextContents) {
            }

            for (Component sibling : component.getSiblings()) {
                visit(sibling, visitor);
            }
        }
    }
}
