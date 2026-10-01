package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

public final class MapStatDisplayEnabler {
    public static final int ALL_RUN = 556;
    public static final int COUNT = 558;
    public static final String ATTRIBUTE_DISPLAY_KEY = "consciousness.attribute_display";
    private static final int WARMUP_TICKS = 40;
    private static final int LISTEN_TICKS = 80;
    private static int warmup = 40;
    private static boolean requested;
    private static boolean listening;
    private static int listenTicks;

    private MapStatDisplayEnabler() {
    }

    public static boolean shouldRequest(boolean hudOn, boolean showStats, boolean hasPointTotals) {
        return hudOn && showStats && !hasPointTotals;
    }

    public static int triggerToSend(boolean hudOn, boolean showStats, boolean hasPointTotals, boolean hasDetailedValues) {
        if (!shouldRequest(hudOn, showStats, hasPointTotals)) {
            return -1;
        } else {
            return hasDetailedValues ? 558 : 556;
        }
    }

    public static int triggerToSend(boolean hudOn, boolean showStats, boolean hasPointTotals, boolean hasDetailedValues, boolean mapReady) {
        return !mapReady ? -1 : triggerToSend(hudOn, showStats, hasPointTotals, hasDetailedValues);
    }

    public static void tick(Minecraft client) {
        if (client.player != null && client.level != null) {
            decayListenWindow();
            if (warmup > 0) {
                warmup--;
            } else {
                boolean hudOn = KeybindManager.showCustomHUD;
                boolean showStats = HUDConfig.get().showStats;
                boolean hasPoints = MapStatBar.sawPointTotals();
                boolean hasDetailed = MapStatBar.sawDetailedValues();
                int trigger = triggerToSend(hudOn, showStats, hasPoints, hasDetailed, MapReady.isReady());
                if (trigger >= 0) {
                    if (!requested) {
                        requested = true;
                        onSentDisplayTrigger();
                        Att2Triggers.send(trigger);
                    }
                } else {
                    if (!hudOn || !showStats) {
                        requested = false;
                    }
                }
            }
        }
    }

    public static void onSentDisplayTrigger() {
        listening = true;
        listenTicks = 80;
    }

    public static boolean applyIfListening(Component title, Component body) {
        if (!listening) {
            return false;
        }

        if (!isAttributeDisplay(title, body)) {
            return false;
        }

        listening = false;
        listenTicks = 0;
        return true;
    }

    public static boolean onDialog(Holder<Dialog> holder) {
        if (listening && holder != null) {
            Dialog dialog = holder.value();
            CommonDialogData common = commonOf(dialog);
            return common == null ? false : applyIfListening(common.title(), bodyOf(common));
        } else {
            return false;
        }
    }

    static boolean isAttributeDisplay(Component title, Component body) {
        return DialogComponents.hasKeyOrChild(title, "consciousness.attribute_display")
            || DialogComponents.hasKeyOrChild(body, "consciousness.attribute_display");
    }

    public static void reset() {
        warmup = 40;
        requested = false;
        listening = false;
        listenTicks = 0;
    }

    private static void decayListenWindow() {
        if (listenTicks > 0) {
            listenTicks--;
            if (listenTicks == 0) {
                listening = false;
            }
        }
    }

    private static CommonDialogData commonOf(Dialog dialog) {
        if (dialog instanceof MultiActionDialog multi) {
            return multi.common();
        } else {
            return dialog instanceof NoticeDialog notice ? notice.common() : null;
        }
    }

    private static Component bodyOf(CommonDialogData common) {
        Component body = Component.empty();

        for (DialogBody part : common.body()) {
            if (part instanceof PlainMessage message) {
                body = body.copy().append(message.contents());
            }
        }

        return body;
    }
}
