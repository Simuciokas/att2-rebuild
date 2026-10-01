package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.rune.RuneCatalog;
import fr.poubone.att2.client.shop.ShopSellers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.EntityHitResult;

public final class MinerShopModel {
    private static final MinerShopModel INSTANCE = new MinerShopModel();
    private static final int BURST_TICKS = 40;
    private int burstTicks;
    private boolean openedThisBurst;
    private boolean npcSession;
    private boolean useWasDown;
    private long lastNpcOpenMs;
    private MinerPrefetch prefetch;
    private final List<MinerCatalog.ExchangeOffer> exchanges = new ArrayList<>();
    private final Map<Integer, Integer> prices = new HashMap<>();
    private int resinCount = -1;
    private int version;

    private MinerShopModel() {
    }

    public static MinerShopModel get() {
        return INSTANCE;
    }

    public void reset() {
        this.cancelPrefetch();
        this.npcSession = false;
        this.useWasDown = false;
        this.lastNpcOpenMs = 0L;
        this.burstTicks = 0;
        this.openedThisBurst = false;
        this.exchanges.clear();
        this.prices.clear();
        this.resinCount = -1;
        this.version++;
        if (Minecraft.getInstance().screen instanceof MinerShopScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public void tick() {
        if (this.burstTicks > 0 && --this.burstTicks == 0) {
            this.openedThisBurst = false;
        }

        if (this.prefetch != null) {
            this.prefetch.tick().ifPresent(this::send);
            if (this.prefetch.isDone()) {
                this.prefetch = null;
            }
        }
    }

    public void pollNpcUse(Minecraft client) {
        if (client != null && client.player != null && client.level != null) {
            boolean down = client.options.keyUse.isDown();
            boolean pressed = down && !this.useWasDown;
            this.useWasDown = down;
            if (pressed && client.screen == null) {
                if (client.hitResult instanceof EntityHitResult hit) {
                    if (ShopSellers.isEldric(hit.getEntity())) {
                        this.openFromNpc();
                    }
                }
            }
        }
    }

    public void onScreenOpened() {
        CurrencyModel.request();
    }

    public void onScreenClosed() {
        this.cancelPrefetch();
        this.npcSession = false;
        CurrencyModel.close();
    }

    public void send(int trigger) {
        Att2Triggers.sendAlways(trigger);
    }

    public void openFromNpc() {
        if (HUDConfig.get().minerMenuEnabled) {
            long now = System.currentTimeMillis();
            if (now - this.lastNpcOpenMs >= 400L) {
                this.lastNpcOpenMs = now;
                this.npcSession = true;
                if (isScreenShowing()) {
                    this.onScreenOpened();
                } else {
                    this.exchanges.clear();
                    this.cancelPrefetch();
                    this.prefetch = new MinerPrefetch(MinerPrefetch.DEFAULT_TRIGGERS, 2);
                    MinerShopScreen.open(MinerCatalog.Tab.BUY);
                }
            }
        }
    }

    public void requestExchangeRefresh() {
        this.send(3535);
    }

    private void cancelPrefetch() {
        if (this.prefetch != null) {
            this.prefetch.cancel();
            this.prefetch = null;
        }
    }

    public List<MinerCatalog.ExchangeOffer> exchanges() {
        return List.copyOf(this.exchanges);
    }

    public int version() {
        return this.version;
    }

    public OptionalInt price(int trigger) {
        Integer value = this.prices.get(trigger);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public OptionalInt resinCount() {
        return this.resinCount < 0 ? OptionalInt.empty() : OptionalInt.of(this.resinCount);
    }

    public boolean onSystemMessage(Component message) {
        return this.onSystemMessage(message, HUDConfig.get().minerMenuEnabled);
    }

    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) {
            return false;
        }

        try {
            return this.handle(message);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean handle(Component message) {
        if (isDialogSpeech(message)) {
            if (isEldricSpeech(message)) {
                Minecraft.getInstance().execute(this::openFromNpc);
            }

            return false;
        } else {
            List<Integer> triggers = collectTriggers(message);
            if (triggers.stream().anyMatch(RuneCatalog::isWorkshopTrigger)) {
                return false;
            }

            boolean catalogKey = hasCatalogKey(message);
            boolean resinLine = containsKey(message, "att2.dark_resin");
            boolean powderStock = this.burstTicks > 0 && isPowderStock(message);
            boolean exchangeLine = hasExchange(message, triggers);
            boolean prisoner = catalogKey || resinLine || powderStock || exchangeLine;
            if (!prisoner) {
                for (int trigger : triggers) {
                    if (MinerCatalog.isBuyTrigger(trigger) || MinerCatalog.isPowderTrigger(trigger) || MinerCatalog.isExchangeTrigger(trigger)) {
                        prisoner = true;
                        break;
                    }
                }
            }

            if (!prisoner) {
                return false;
            }

            this.burstTicks = 40;
            MinerCatalog.Tab tab = inferTab(triggers, catalogKey, exchangeLine);
            Map<Integer, Integer> parsedPrices = MinerPriceParser.parse(message);
            if (!parsedPrices.isEmpty()) {
                this.prices.putAll(parsedPrices);
                this.version++;
            }

            int parsedResin = parseTranslatedNumber(message, "att2.dark_resin");
            if (parsedResin >= 0) {
                this.resinCount = parsedResin;
                this.version++;
            }

            if (exchangeLine) {
                List<MinerCatalog.ExchangeOffer> parsed = parseExchanges(message);
                if (!parsed.isEmpty()) {
                    this.exchanges.clear();
                    this.exchanges.addAll(parsed);
                    this.version++;
                }
            }

            if (!this.npcSession || !isScreenShowing()) {
                if (!this.openedThisBurst) {
                    this.openedThisBurst = true;
                    if (!isScreenShowing()) {
                        MinerShopScreen.open(tab);
                    } else if (Minecraft.getInstance().screen instanceof MinerShopScreen screen) {
                        screen.switchTab(tab);
                    }
                } else if (Minecraft.getInstance().screen instanceof MinerShopScreen screen && (exchangeLine || tab != screen.tab())) {
                    screen.switchTab(tab);
                }
            }

            return isScreenShowing();
        }
    }

    private static MinerCatalog.Tab inferTab(List<Integer> triggers, boolean catalogKey, boolean exchangeLine) {
        if (exchangeLine) {
            return MinerCatalog.Tab.EXCHANGE;
        }

        for (int trigger : triggers) {
            if (MinerCatalog.isPrisonerGameplayTrigger(trigger)) {
                return MinerCatalog.tabForTrigger(trigger);
            }
        }

        return catalogKey ? MinerCatalog.Tab.BUY : MinerCatalog.Tab.BUY;
    }

    private static boolean isDialogSpeech(Component message) {
        return message.getString().contains("°-°");
    }

    private static boolean isEldricSpeech(Component message) {
        String raw = message.getString().toLowerCase(Locale.ROOT);
        return raw.contains("°-°") && raw.contains("eldric");
    }

    private static boolean hasCatalogKey(Component message) {
        return containsKey(message, "att2.rune.list") || containsKey(message, "att2.rune_powder.list");
    }

    private static int parseTranslatedNumber(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(key)) {
                for (Object arg : t.getArgs()) {
                    String value = arg instanceof Component c ? c.getString() : String.valueOf(arg);
                    Matcher matcher = Pattern.compile("\\d+").matcher(value);
                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group());
                    }
                }
            }

            for (Object arg : t.getArgs()) {
                if (arg instanceof Component c) {
                    int found = parseTranslatedNumber(c, key);
                    if (found >= 0) {
                        return found;
                    }
                }
            }
        }

        for (Component sibling : component.getSiblings()) {
            int found = parseTranslatedNumber(sibling, key);
            if (found >= 0) {
                return found;
            }
        }

        return -1;
    }

    private static boolean hasExchange(Component message, List<Integer> triggers) {
        if (containsKey(message, "att2.rune_exchange")) {
            return true;
        }

        for (int trigger : triggers) {
            if (MinerCatalog.isExchangeTrigger(trigger)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPowderStock(Component message) {
        if (visitScore(message)) {
            return true;
        }

        String lower = message.getString().toLowerCase(Locale.ROOT);
        return lower.contains("stock total de poudres")
            || lower.contains("total stock runic")
            || lower.contains("poudres runiques")
            || lower.contains("runic powder")
            || lower.contains("runenpulver");
    }

    static List<MinerCatalog.ExchangeOffer> parseExchanges(Component message) {
        List<MinerCatalog.ExchangeOffer> out = new ArrayList<>();
        List<String> ingredients = new ArrayList<>();
        String result = null;
        boolean resultNext = false;
        int trigger = -1;
        boolean inRecipe = false;
        List<Component> parts = new ArrayList<>();
        visit(message, parts::add);
        Iterator var8 = parts.iterator();

        while (true) {
            String text;
            while (true) {
                if (!var8.hasNext()) {
                    flushRecipe(out, ingredients, result, trigger, inRecipe || trigger >= 0);
                    return out;
                }

                Component part = (Component)var8.next();
                if (part.getStyle().getClickEvent() instanceof RunCommand run) {
                    int parsed = Att2Triggers.parseTriggerCommand(run.command());
                    if (MinerCatalog.isExchangeTrigger(parsed)) {
                        trigger = parsed;
                    }
                }

                if (part.getContents() instanceof TranslatableContents translatable && "att2.rune_exchange".equals(translatable.getKey())) {
                    inRecipe = true;
                }

                if (part.getContents() instanceof PlainTextContents plain) {
                    text = plain.text();
                    if (!text.contains("⚙") && !text.contains("\n")) {
                        break;
                    }

                    flushRecipe(out, ingredients, result, trigger, inRecipe || trigger >= 0);
                    ingredients.clear();
                    result = null;
                    resultNext = false;
                    trigger = -1;
                    inRecipe = text.contains("⚙");
                    if (text.contains("⚙")) {
                        text = text.replace("⚙", "");
                        break;
                    }
                }
            }

            String token = text.replace("+", " ").replace("=>", " => ").strip();
            if (token.isEmpty()) {
                if (text.contains("=>")) {
                    resultNext = true;
                }
            } else {
                if (text.contains("=>")) {
                    resultNext = true;
                }

                for (String word : token.split("[\\s|]+")) {
                    String clean = word.replace("⚙", "").strip();
                    if (!clean.isEmpty() && !clean.equals("+") && !clean.equals("=>")) {
                        if (MinerCatalog.runeByName(clean).isPresent()) {
                            inRecipe = true;
                            String id = clean.toLowerCase(Locale.ROOT);
                            if (resultNext && result == null) {
                                result = id;
                                resultNext = false;
                            } else {
                                ingredients.add(id);
                            }
                        }
                    } else if (clean.equals("=>")) {
                        resultNext = true;
                    }
                }
            }
        }
    }

    private static void flushRecipe(List<MinerCatalog.ExchangeOffer> out, List<String> ingredients, String result, int trigger, boolean started) {
        if (started) {
            if (result != null && !ingredients.isEmpty() && trigger >= 0) {
                out.add(new MinerCatalog.ExchangeOffer(trigger, List.copyOf(ingredients), result));
            }
        }
    }

    private static boolean visitScore(Component component) {
        if (component.getContents() instanceof ScoreContents score && "RUNE_POWDER".equals(score.objective())) {
            return true;
        } else {
            for (Component sibling : component.getSiblings()) {
                if (visitScore(sibling)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean containsKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translatable && key.equals(translatable.getKey())) {
            return true;
        } else {
            for (Component sibling : component.getSiblings()) {
                if (containsKey(sibling, key)) {
                    return true;
                }
            }

            return false;
        }
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

    private static void visit(Component component, Consumer<Component> visitor) {
        visitor.accept(component);

        for (Component sibling : component.getSiblings()) {
            visit(sibling, visitor);
        }
    }

    private static boolean isScreenShowing() {
        return Minecraft.getInstance().screen instanceof MinerShopScreen;
    }
}
