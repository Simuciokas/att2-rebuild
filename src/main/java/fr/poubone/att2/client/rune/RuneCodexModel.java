package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.data.ScoreboardSync;
import fr.poubone.att2.client.hud.HUDConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.contents.ScoreContents;

public final class RuneCodexModel {
    private static final RuneCodexModel INSTANCE = new RuneCodexModel();
    private static final int POWDER_WINDOW_TICKS = 40;
    private static final int POWDER_DEFER_TICKS = 8;
    private static final int SERIES_GAP_TICKS = 2;
    private int powderWindow;
    private boolean openedThisBurst;
    private Component pendingPowder;
    private int pendingPowderTicks;
    private int seriesTrigger;
    private int seriesRemaining;
    private int seriesCooldown;
    private BooleanSupplier seriesCanContinue;
    private final RuneBundleMessage bundleMessage = new RuneBundleMessage();

    private RuneCodexModel() {
    }

    public static RuneCodexModel get() {
        return INSTANCE;
    }

    public void reset() {
        this.abortCraftSeries();
        this.powderWindow = 0;
        this.openedThisBurst = false;
        this.pendingPowder = null;
        this.pendingPowderTicks = 0;
        this.bundleMessage.reset();
        if (Minecraft.getInstance().screen instanceof RuneCodexScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public void tick() {
        if (this.pendingPowderTicks > 0) {
            this.pendingPowderTicks--;
            if (this.pendingPowderTicks == 0) {
                this.flushPendingPowder();
            }
        }

        if (this.powderWindow > 0) {
            this.powderWindow--;
        }

        if (this.powderWindow == 0) {
            this.openedThisBurst = false;
        }

        if (this.seriesRemaining > 0) {
            if (this.seriesCooldown > 0) {
                this.seriesCooldown--;
            } else {
                if (this.seriesCanContinue != null && !this.seriesCanContinue.getAsBoolean()) {
                    this.abortCraftSeries();
                    return;
                }

                Att2Triggers.sendAlways(this.seriesTrigger);
                this.seriesRemaining--;
                this.seriesCooldown = 2;
            }
        }
    }

    public void onScreenOpened() {
        CurrencyModel.request();
        this.bundleMessage.reset();
        Att2Triggers.send(3608);
        ScoreboardSync.setFastMode(true, RuneCatalog.syncObjectives());
    }

    public void onScreenClosed() {
        this.abortCraftSeries();
        CurrencyModel.close();
        ScoreboardSync.setFastMode(false, List.of());
    }

    public void craft(int trigger) {
        this.craft(trigger, 1, false);
    }

    public void craft(int trigger, int qty, boolean wordMode) {
        this.craft(trigger, qty, wordMode, null);
    }

    public void craft(int trigger, int qty, boolean wordMode, BooleanSupplier canContinue) {
        if (this.seriesRemaining <= 0) {
            int sends = CraftSeriesLogic.triggerSends(qty, wordMode);
            if (sends >= 1) {
                this.seriesTrigger = trigger;
                this.seriesRemaining = sends;
                this.seriesCooldown = 0;
                this.seriesCanContinue = canContinue;
                Att2Triggers.sendAlways(this.seriesTrigger);
                this.seriesRemaining--;
                this.seriesCooldown = 2;
            }
        }
    }

    public void abortCraftSeries() {
        this.seriesRemaining = 0;
        this.seriesCooldown = 0;
        this.seriesTrigger = 0;
        this.seriesCanContinue = null;
    }

    public boolean onSystemMessage(Component message) {
        return this.onSystemMessage(message, HUDConfig.get().runeMenuEnabled);
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
        if (this.bundleMessage.accept(message)) {
            return isScreenShowing();
        }

        List<Integer> triggers = collectTriggers(message);

        for (int trigger : triggers) {
            if (RuneCatalog.isMenuTrigger(trigger)) {
                this.dropPendingPowder();
                this.powderWindow = 40;
                if (!this.openedThisBurst) {
                    this.openedThisBurst = true;
                    if (!isScreenShowing()) {
                        RuneCodexScreen.open(RuneCatalog.menuTab(trigger));
                    }
                }

                return true;
            }
        }

        if (isScreenShowing()) {
            for (int trigger : triggers) {
                if (isRecipeDumpTrigger(trigger)) {
                    return true;
                }
            }
        }

        if (triggers.isEmpty() && isPowderLine(message)) {
            this.pendingPowder = message;
            this.pendingPowderTicks = 8;
            return true;
        } else {
            return false;
        }
    }

    private void dropPendingPowder() {
        this.pendingPowder = null;
        this.pendingPowderTicks = 0;
    }

    private void flushPendingPowder() {
        Component line = this.pendingPowder;
        this.pendingPowder = null;
        this.pendingPowderTicks = 0;
        if (line != null) {
            Minecraft client = Minecraft.getInstance();
            if (client.gui != null) {
                client.gui.getChat().addMessage(line);
            }
        }
    }

    private static boolean isRecipeDumpTrigger(int trigger) {
        return trigger >= 343 && trigger <= 365 || trigger >= 366 && trigger <= 392;
    }

    private static boolean isPowderLine(Component message) {
        return visitScore(message);
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

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, component -> {
            if (component.getStyle().getClickEvent() instanceof RunCommand run) {
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
        return Minecraft.getInstance().screen instanceof RuneCodexScreen;
    }
}
