package fr.poubone.att2.client.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;

public final class PlayerInfoModel {
    private static boolean listening;
    private static boolean received;

    private PlayerInfoModel() {
    }

    public static void request() {
        listening = true;
        Att2Triggers.send(2314);
    }

    public static void reset() {
        listening = false;
        received = false;
    }

    public static boolean isListening() {
        return listening;
    }

    public static boolean hasData() {
        return received;
    }

    public static boolean applyIfListening(List<PlayerInfoDialogParser.Action> actions) {
        if (!listening) {
            return false;
        }

        Optional<PlayerInfoDialogParser.Snapshot> snap = PlayerInfoDialogParser.parse(actions);
        if (snap.isPresent()) {
            PlayerInfoDialogParser.Snapshot s = snap.get();
            ScoreCache.put("GAMELEVEL", s.gameLevel());
            ScoreCache.put("LEVELMASTER", s.levelMaster());
            ScoreCache.put("DAHALMAX", s.dahalMax());
            ScoreCache.put("LVL_UPGRADE_REQ", s.xpRequired());
            received = true;
            listening = false;
            return true;
        }

        if (!looksLikePlayerInfo(actions)) {
            return false;
        }

        listening = false;
        return true;
    }

    public static boolean onDialog(Holder<Dialog> holder) {
        if (listening && holder != null) {
            Dialog dialog = holder.value();
            if (dialog instanceof MultiActionDialog multi) {
                if (DialogComponents.hasKeyOrChild(multi.common().title(), "consciousness.player_infor")) {
                    List<PlayerInfoDialogParser.Action> actions = actionsOf(multi);
                    applyIfListening(actions);
                    listening = false;
                    return true;
                } else {
                    List<PlayerInfoDialogParser.Action> actions = actionsOf(multi);
                    return applyIfListening(actions);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static List<PlayerInfoDialogParser.Action> actionsOf(MultiActionDialog multi) {
        List<PlayerInfoDialogParser.Action> actions = new ArrayList<>();

        for (ActionButton button : multi.actions()) {
            Component label = button.button().label();
            Component tooltip = button.button().tooltip().orElse(null);
            actions.add(new PlayerInfoDialogParser.Action(label, tooltip));
        }

        return actions;
    }

    private static boolean looksLikePlayerInfo(List<PlayerInfoDialogParser.Action> actions) {
        if (actions == null) {
            return false;
        }

        for (PlayerInfoDialogParser.Action action : actions) {
            if (action != null && action.label() != null && DialogComponents.hasKeyOrChild(action.label(), "consciousness.player_infor")) {
                return true;
            }
        }

        return false;
    }
}
