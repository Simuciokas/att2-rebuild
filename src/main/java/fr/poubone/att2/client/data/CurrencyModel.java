package fr.poubone.att2.client.data;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

public final class CurrencyModel {
    private static boolean listening;
    private static boolean asked;
    private static boolean received;

    private CurrencyModel() {
    }

    public static void request() {
        if (!asked) {
            asked = true;
            listening = true;
            Att2Triggers.send(2312);
        }
    }

    public static void close() {
        listening = false;
        asked = false;
    }

    public static void reset() {
        listening = false;
        asked = false;
        received = false;
    }

    public static boolean hasData() {
        return received;
    }

    public static boolean isListening() {
        return listening;
    }

    public static boolean applyIfListening(Component body) {
        if (!listening) {
            return false;
        }

        Optional<CurrencyDialogParser.Snapshot> snap = CurrencyDialogParser.parse(body);
        if (snap.isEmpty()) {
            return false;
        }

        CurrencyDialogParser.Snapshot s = snap.get();
        ScoreCache.putDialogChronotons(s.chronotons());
        ScoreCache.put("ESC", s.esc());
        ScoreCache.putHolder("RUNE_POWDER", "#stock", s.runePowder());
        listening = false;
        asked = false;
        received = true;
        return true;
    }

    public static boolean onDialog(Holder<Dialog> holder) {
        if (listening && holder != null) {
            Dialog dialog = holder.value();
            if (dialog instanceof MultiActionDialog multi) {
                MutableComponent body = Component.empty();
                boolean any = false;

                for (DialogBody part : multi.common().body()) {
                    if (part instanceof PlainMessage message) {
                        body = body.copy().append(message.contents());
                        any = true;
                    }
                }

                return applyIfListening(any ? body : null);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
