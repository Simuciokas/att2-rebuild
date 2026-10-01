package fr.poubone.att2.client.gambling;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyEnabler;
import fr.poubone.att2.client.hud.HUDConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class GamblingModel {
    private static final GamblingModel INSTANCE = new GamblingModel();
    public static final Vec3 TABLE_POS = new Vec3(-4962.0, 97.0, -5803.0);
    public static final double FLIP_MAX_DISTANCE = 15.0;
    public static final int TRIGGER_BETS_MENU = 730;
    public static final int TRIGGER_BET_FIRST = 394;
    public static final int TRIGGER_BET_LAST = 400;
    public static final int TRIGGER_GRID_CONTINUE = 2844;
    public static final int TRIGGER_GRID_FIRST = 2845;
    public static final int TRIGGER_GRID_LAST = 2854;
    public static final int TRIGGER_GRID_RESET = 2855;
    public static final int TRIGGER_GRID_MENU = 2856;
    public static final int TRIGGER_CELL_FIRST = 2332;
    public static final int TRIGGER_CELL_LAST = 2843;
    private static final String KEY_BET_PRICE = "att2.shop.chronotons.buy";
    private static final String KEY_PRICE = "att2.chronotons.price";
    private static final String KEY_LOCK = "att2.lock";
    private static final String KEY_BET_SCORE = "att2.gambling.score";
    private static final String KEY_SELECT_TITLE = "matching_game.select.title";
    private static final String KEY_SELECT_LOCK = "matching_game.select.lock";
    private static final String KEY_CELL_LOCK = "matching_game.lock";
    private static final String KEY_CLICK_COUNT = "matching_game.show_click_count";
    private static final String KEY_CLICK_PRICE = "matching_game.click_price";
    private static final String KEY_BACK_MENU = "matching_game.back_menu";
    private static final String KEY_RESET = "matching_game.reset";
    private static final Identifier NOT_ENOUGH_SOUND = Identifier.withDefaultNamespace("noise4");
    private static final int MENU_WINDOW_TICKS = 40;
    private static final int FEEDBACK_WINDOW_TICKS = 40;
    private final List<GamblingModel.BetEntry> bets = new ArrayList<>();
    private final List<GamblingModel.GridChoice> gridChoices = new ArrayList<>();
    private final List<List<GamblingModel.GridCell>> grid = new ArrayList<>();
    private final List<GamblingModel.CountEntry> counts = new ArrayList<>();
    private int nextFlipPrice = -1;
    private boolean nextFlipAffordable = true;
    private int lastBetScore = -1;
    private long lastBetScoreTime = 0L;
    private int betsMenuTicks = 0;
    private int feedbackTicks = 0;
    private boolean notEnoughChronotons = false;
    private boolean betPending = false;
    private int betPendingTicks = 0;
    private boolean waitingForBets = false;
    private boolean waitingForGridMenu = false;
    private boolean waitingForGrid = false;
    private int waitTicks = 0;

    private GamblingModel() {
    }

    public static GamblingModel get() {
        return INSTANCE;
    }

    public void reset() {
        this.bets.clear();
        this.gridChoices.clear();
        this.grid.clear();
        this.counts.clear();
        this.nextFlipPrice = -1;
        this.nextFlipAffordable = true;
        this.lastBetScore = -1;
        this.lastBetScoreTime = 0L;
        this.betsMenuTicks = 0;
        this.feedbackTicks = 0;
        this.notEnoughChronotons = false;
        this.betPending = false;
        this.betPendingTicks = 0;
        this.waitingForBets = false;
        this.waitingForGridMenu = false;
        this.waitingForGrid = false;
        this.waitTicks = 0;
    }

    public void requestBets() {
        this.waitingForBets = true;
        this.waitTicks = 60;
        Att2Triggers.send(730);
    }

    public void placeBet(GamblingModel.BetEntry bet) {
        if (!bet.locked()) {
            this.betPending = true;
            this.betPendingTicks = 200;
            this.armFeedback();
            Att2Triggers.send(bet.trigger());
            CurrencyEnabler.onTransaction();
        }
    }

    public void requestGridMenu() {
        this.hideBoard();
        this.waitingForGridMenu = true;
        this.waitTicks = 60;
        Att2Triggers.send(2856);
    }

    public void chooseGrid(GamblingModel.GridChoice choice) {
        if (!choice.locked()) {
            this.waitingForGrid = true;
            this.waitTicks = 60;
            this.armFeedback();
            Att2Triggers.send(choice.trigger());
            CurrencyEnabler.onTransaction();
        }
    }

    public void flip(GamblingModel.GridCell cell) {
        if (cell.isLocked() && cell.trigger() >= 0 && this.isNearTable()) {
            this.armFeedback();
            Att2Triggers.send(cell.trigger());
            CurrencyEnabler.onTransaction();
        }
    }

    public void resetGrid() {
        this.waitingForGrid = true;
        this.waitTicks = 60;
        this.armFeedback();
        Att2Triggers.send(2855);
        CurrencyEnabler.onTransaction();
    }

    private void armFeedback() {
        this.notEnoughChronotons = false;
        this.feedbackTicks = 40;
    }

    public void tick(Minecraft client) {
        if (this.betsMenuTicks > 0) {
            this.betsMenuTicks--;
        }

        if (this.feedbackTicks > 0) {
            this.feedbackTicks--;
        }

        if (this.waitTicks > 0 && --this.waitTicks == 0) {
            this.waitingForBets = false;
            this.waitingForGridMenu = false;
            this.waitingForGrid = false;
        }

        if (this.betPending && --this.betPendingTicks <= 0) {
            this.betPending = false;
        }
    }

    public void onTitle(Component title) {
        TranslatableContents score = findTranslatable(title, "att2.gambling.score");
        if (score != null && score.getArgs().length != 0) {
            parseInt(argText(score.getArgs()[0])).ifPresent(value -> {
                this.lastBetScore = value;
                this.lastBetScoreTime = System.currentTimeMillis();
                this.betPending = false;
            });
        }
    }

    public void onSound(Identifier sound, SoundSource source) {
        if (this.feedbackTicks > 0 && NOT_ENOUGH_SOUND.equals(sound) && source == SoundSource.PLAYERS) {
            this.notEnoughChronotons = true;
            this.betPending = false;
            this.waitingForGrid = false;
        }
    }

    public boolean onSystemMessage(Component message) {
        return this.onSystemMessage(message, HUDConfig.get().charlesAutoOpen);
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
        List<Integer> triggers = collectTriggers(message);
        if (triggers.contains(730) && !containsKey(message, "matching_game.back_menu")) {
            return this.openFromCharles(CharlesScreen.Tab.BETS);
        }

        if (triggers.contains(2856)
            && !containsKey(message, "matching_game.back_menu")
            && !containsKey(message, "matching_game.lock")
            && !containsAnyCell(message)) {
            return this.openFromCharles(CharlesScreen.Tab.GRIDS);
        }

        for (int trigger : triggers) {
            if (trigger >= 394 && trigger <= 400) {
                return this.handleBetLine(message, trigger);
            }
        }

        if (this.betsMenuTicks > 0
            && containsKey(message, "att2.lock")
            && !containsKey(message, "matching_game.select.lock")
            && message.getString().contains("<???>")) {
            this.bets.add(new GamblingModel.BetEntry(this.bets.size() + 1, Component.literal("<???>"), -1, -1));
            this.betsMenuTicks = 40;
            return isScreenShowing();
        }

        if (containsKey(message, "matching_game.select.title")) {
            this.hideBoard();
            this.gridChoices.clear();
            this.waitingForGridMenu = false;
            return isScreenShowing();
        }

        for (int trigger : triggers) {
            if (trigger == 2844 || trigger >= 2845 && trigger <= 2854) {
                return this.handleGridChoiceLine(message, trigger);
            }
        }

        if (containsKey(message, "matching_game.select.lock")) {
            this.gridChoices.add(new GamblingModel.GridChoice(Component.translatable("matching_game.select.lock"), -1, -1, true, false));
            return isScreenShowing();
        } else if (containsKey(message, "matching_game.lock") || containsAnyCell(message)) {
            return this.handleGridLine(message);
        } else if (containsKey(message, "matching_game.show_click_count")) {
            return this.handleCountLine(message);
        } else {
            TranslatableContents price = findTranslatable(message, "matching_game.click_price");
            if (price != null && price.getArgs().length > 0) {
                Component arg = argComponent(price.getArgs()[0]);
                parseInt(argText(price.getArgs()[0])).ifPresent(value -> this.nextFlipPrice = value);
                TextColor color = arg == null ? null : findColor(arg);
                this.nextFlipAffordable = color == null || color.getValue() != 16733525;
                return isScreenShowing();
            } else {
                return containsKey(message, "matching_game.back_menu") && containsKey(message, "matching_game.reset") ? isScreenShowing() : false;
            }
        }
    }

    private boolean openFromCharles(CharlesScreen.Tab tab) {
        this.maybeOpenCharles(tab);
        if (tab == CharlesScreen.Tab.BETS) {
            this.requestBets();
        } else {
            this.requestGridMenu();
        }

        return false;
    }

    private void maybeOpenCharles(CharlesScreen.Tab tab) {
        if (HUDConfig.get().charlesAutoOpen) {
            Minecraft client = Minecraft.getInstance();
            if (client.screen == null || client.screen instanceof CharlesScreen || client.screen instanceof ChatScreen) {
                CharlesScreen.open(tab);
            }
        }
    }

    private boolean handleBetLine(Component message, int trigger) {
        int index = trigger - 394 + 1;
        if (this.betsMenuTicks == 0 || index == 1) {
            this.bets.clear();
        }

        Component label = findLabel(message, "<");
        TranslatableContents priceContents = findTranslatable(message, "att2.shop.chronotons.buy");
        int price = priceContents != null && priceContents.getArgs().length != 0 ? parseInt(argText(priceContents.getArgs()[0])).orElse(-1) : -1;
        this.bets.removeIf(b -> b.index() == index);
        this.bets.add(new GamblingModel.BetEntry(index, label == null ? Component.literal("<" + index + ">") : label, price, trigger));
        this.bets.sort((a, b) -> Integer.compare(a.index(), b.index()));
        this.betsMenuTicks = 40;
        this.waitingForBets = false;
        this.maybeOpenCharles(CharlesScreen.Tab.BETS);
        return isScreenShowing();
    }

    private boolean handleGridChoiceLine(Component message, int trigger) {
        Component label = findTranslatableComponent(message, "matching_game.select.");
        TranslatableContents priceContents = findTranslatable(message, "att2.chronotons.price");
        int price = priceContents != null && priceContents.getArgs().length != 0 ? parseInt(argText(priceContents.getArgs()[0])).orElse(-1) : -1;
        boolean isContinue = trigger == 2844;
        this.hideBoard();
        this.gridChoices.removeIf(c -> c.trigger() == trigger);
        this.gridChoices.add(new GamblingModel.GridChoice(label == null ? Component.literal("?") : label, price, trigger, false, isContinue));
        this.waitingForGridMenu = false;
        this.maybeOpenCharles(CharlesScreen.Tab.GRIDS);
        return isScreenShowing();
    }

    private boolean handleGridLine(Component message) {
        List<List<GamblingModel.GridCell>> rows = new ArrayList<>();
        List<GamblingModel.GridCell> row = new ArrayList<>();

        for (Component part : flatten(message)) {
            if (part.getContents() instanceof PlainTextContents plain && plain.text().contains("\n")) {
                if (!row.isEmpty()) {
                    rows.add(row);
                }

                row = new ArrayList<>();
            } else if (part.getContents() instanceof TranslatableContents translatable) {
                GamblingModel.CellType type = GamblingModel.CellType.fromTranslationKey(translatable.getKey());
                if (type != null) {
                    int trigger = -1;
                    if (part.getStyle().getClickEvent() instanceof RunCommand run) {
                        trigger = Att2Triggers.parseTriggerCommand(run.command());
                    }

                    row.add(new GamblingModel.GridCell(type, trigger, part));
                }
            }
        }

        if (!row.isEmpty()) {
            rows.add(row);
        }

        if (rows.isEmpty()) {
            return false;
        }

        this.grid.clear();
        this.grid.addAll(rows);
        this.gridChoices.clear();
        this.waitingForGrid = false;
        return isScreenShowing();
    }

    private boolean handleCountLine(Component message) {
        List<GamblingModel.CountEntry> parsed = new ArrayList<>();
        GamblingModel.CellType pendingType = null;

        for (Component part : flatten(message)) {
            if (part.getContents() instanceof ObjectContents object && object.contents() instanceof AtlasSprite sprite) {
                String path = sprite.sprite().getPath();
                pendingType = GamblingModel.CellType.fromId(path.substring(path.lastIndexOf(47) + 1));
            } else if (part.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals("matching_game.show_click_count")
                && translatable.getArgs().length > 0) {
                Component arg = argComponent(translatable.getArgs()[0]);
                int count = parseInt(argText(translatable.getArgs()[0])).orElse(0);
                if (pendingType != null) {
                    parsed.add(new GamblingModel.CountEntry(pendingType, count, arg == null ? null : findColor(arg)));
                }

                pendingType = null;
            }
        }

        this.counts.clear();
        this.counts.addAll(parsed);
        return isScreenShowing();
    }

    private static boolean isScreenShowing() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.screen instanceof CharlesScreen;
    }

    public List<GamblingModel.BetEntry> getBets() {
        return Collections.unmodifiableList(this.bets);
    }

    public List<GamblingModel.GridChoice> getGridChoices() {
        return Collections.unmodifiableList(this.gridChoices);
    }

    public List<List<GamblingModel.GridCell>> getGrid() {
        return Collections.unmodifiableList(this.grid);
    }

    public boolean hasGrid() {
        return !this.grid.isEmpty();
    }

    void hideBoard() {
        this.grid.clear();
        this.counts.clear();
        this.nextFlipPrice = -1;
        this.nextFlipAffordable = true;
    }

    public boolean isGridComplete() {
        return this.hasGrid() && this.grid.stream().flatMap(Collection::stream).noneMatch(GamblingModel.GridCell::isLocked);
    }

    public List<GamblingModel.CountEntry> getCounts() {
        return Collections.unmodifiableList(this.counts);
    }

    public int getNextFlipPrice() {
        return this.nextFlipPrice;
    }

    public boolean isNextFlipAffordable() {
        return this.nextFlipAffordable;
    }

    public int getLastBetScore() {
        return this.lastBetScore;
    }

    public long getLastBetScoreTime() {
        return this.lastBetScoreTime;
    }

    public boolean isBetPending() {
        return this.betPending;
    }

    public boolean isWaiting() {
        return this.waitingForBets || this.waitingForGridMenu || this.waitingForGrid;
    }

    public boolean isNotEnoughChronotons() {
        return this.notEnoughChronotons;
    }

    public void clearNotEnoughChronotons() {
        this.notEnoughChronotons = false;
    }

    public double distanceToTable() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? Double.MAX_VALUE : Math.sqrt(player.position().distanceToSqr(TABLE_POS));
    }

    public boolean isNearTable() {
        return this.distanceToTable() <= 15.0;
    }

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, c -> {
            if (c.getStyle().getClickEvent() instanceof RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0) {
                    triggers.add(trigger);
                }
            }
        });
        return triggers;
    }

    private static boolean containsAnyCell(Component message) {
        boolean[] found = new boolean[]{false};
        visit(
            message,
            c -> {
                if (!found[0]
                    && c.getContents() instanceof TranslatableContents translatable
                    && GamblingModel.CellType.fromTranslationKey(translatable.getKey()) != null) {
                    found[0] = true;
                }
            }
        );
        return found[0];
    }

    private static boolean containsKey(Component message, String key) {
        return findTranslatable(message, key) != null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        TranslatableContents[] result = new TranslatableContents[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
                result[0] = translatable;
            }
        });
        if (result[0] == null) {
            visit(component, c -> {
                if (result[0] == null && c.getStyle().getHoverEvent() instanceof ShowText show) {
                    TranslatableContents inner = findTranslatable(show.value(), key);
                    if (inner != null) {
                        result[0] = inner;
                    }
                }
            });
        }

        return result[0];
    }

    private static Component findTranslatableComponent(Component component, String keyPrefix) {
        Component[] result = new Component[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(keyPrefix)) {
                result[0] = Component.translatable(translatable.getKey()).withStyle(c.getStyle().withClickEvent(null).withHoverEvent(null));
            }
        });
        return result[0];
    }

    private static Component findLabel(Component component, String prefix) {
        Component[] result = new Component[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof PlainTextContents plain && plain.text().startsWith(prefix)) {
                result[0] = Component.literal(plain.text()).withStyle(c.getStyle().withClickEvent(null).withHoverEvent(null));
            }
        });
        return result[0];
    }

    private static TextColor findColor(Component component) {
        TextColor[] result = new TextColor[]{null};
        visit(component, c -> {
            if (result[0] == null && c.getStyle().getColor() != null) {
                result[0] = c.getStyle().getColor();
            }
        });
        return result[0];
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
                    if (arg instanceof Component argComponent) {
                        visit(argComponent, visitor);
                    }
                }
            }

            for (Component sibling : component.getSiblings()) {
                visit(sibling, visitor);
            }
        }
    }

    private static Component argComponent(Object arg) {
        return arg instanceof Component component ? component : null;
    }

    private static String argText(Object arg) {
        return arg instanceof Component component ? component.getString() : String.valueOf(arg);
    }

    private static OptionalInt parseInt(String text) {
        try {
            return OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public record BetEntry(int index, Component label, int price, int trigger) {
        public boolean locked() {
            return this.trigger < 0;
        }
    }

    public enum CellType {
        LOCK("lock", 32),
        MELEEWEAPON("meleeweapon", 32),
        RANGEWEAPON("rangeweapon", 32),
        ARMOR("armor", 32),
        SPELL("spell", 32),
        CURRENCY("currency", 32),
        FOOD("food", 32),
        POTION("potion", 32),
        RUNE("rune", 20);

        public final String id;
        public final Identifier texture;
        public final int textureSize;

        CellType(String id, int textureSize) {
            this.id = id;
            this.textureSize = textureSize;
            this.texture = Identifier.withDefaultNamespace("textures/item/custom/matching_game/" + id + ".png");
        }

        static GamblingModel.CellType fromTranslationKey(String key) {
            if ("matching_game.lock".equals(key)) {
                return LOCK;
            }

            for (GamblingModel.CellType type : values()) {
                if (type != LOCK && key.equals("matching_game." + type.id + ".unlock")) {
                    return type;
                }
            }

            return null;
        }

        static GamblingModel.CellType fromId(String id) {
            for (GamblingModel.CellType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }

            return null;
        }
    }

    public record CountEntry(GamblingModel.CellType type, int count, TextColor color) {
    }

    public record GridCell(GamblingModel.CellType type, int trigger, Component original) {
        public boolean isLocked() {
            return this.type == GamblingModel.CellType.LOCK;
        }
    }

    public record GridChoice(Component label, int price, int trigger, boolean locked, boolean isContinue) {
    }
}
