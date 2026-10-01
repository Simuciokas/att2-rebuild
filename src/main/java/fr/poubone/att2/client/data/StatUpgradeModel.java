package fr.poubone.att2.client.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

public final class StatUpgradeModel {
    private static boolean listening;
    private static boolean received;
    private static int skillPoints;
    private static final Map<String, Integer> costs = new HashMap<>();
    private static final Map<String, Integer> bases = new HashMap<>();
    private static final Map<String, Map<String, Integer>> sources = new HashMap<>();

    private StatUpgradeModel() {
    }

    public static void open() {
        listening = true;
        Att2Triggers.send(2313);
    }

    public static void close() {
        listening = false;
    }

    public static void reset() {
        listening = false;
        received = false;
        skillPoints = 0;
        costs.clear();
        bases.clear();
        sources.clear();
    }

    public static boolean isListening() {
        return listening;
    }

    public static boolean hasData() {
        return received;
    }

    public static int skillPoints() {
        return skillPoints;
    }

    public static OptionalInt upgradeCost(String statKey) {
        return received && statKey != null && costs.containsKey(statKey) ? OptionalInt.of(costs.get(statKey)) : OptionalInt.empty();
    }

    public static OptionalInt base(String statKey) {
        return received && statKey != null && bases.containsKey(statKey) ? OptionalInt.of(bases.get(statKey)) : OptionalInt.empty();
    }

    public static Map<String, Integer> sources(String statKey) {
        return received && statKey != null ? sources.getOrDefault(statKey, Map.of()) : Map.of();
    }

    public static boolean onDialog(Holder<Dialog> holder) {
        if (listening && holder != null) {
            Dialog dialog = holder.value();
            if (dialog instanceof MultiActionDialog multi) {
                ArrayList actions = new ArrayList();
                MutableComponent actionTooltips = Component.empty();

                for (ActionButton button : multi.actions()) {
                    Component label = button.button().label();
                    Component tooltip = button.button().tooltip().orElse(null);
                    if (tooltip != null && DialogComponents.hasKey(label, "consciousness.stat.point")) {
                        actionTooltips = actionTooltips.copy().append(tooltip);
                    }

                    int trigger = triggerOf(button);
                    if (trigger >= 0) {
                        actions.add(new AttributeDialogParser.Action(label, trigger));
                    }
                }

                Component body = Component.empty();

                for (DialogBody part : multi.common().body()) {
                    if (part instanceof PlainMessage message) {
                        body = body.copy().append(message.contents());
                    }
                }

                return applyIfListening(multi.common().title(), actions, body, actionTooltips);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean applyIfListening(Component title, List<AttributeDialogParser.Action> actions) {
        return applyIfListening(title, actions, Component.empty(), Component.empty());
    }

    public static boolean applyIfListening(Component title, List<AttributeDialogParser.Action> actions, Component body) {
        return applyIfListening(title, actions, body, Component.empty());
    }

    public static boolean applyIfListening(Component title, List<AttributeDialogParser.Action> actions, Component body, Component actionTooltips) {
        if (!listening) {
            return false;
        }

        Optional<AttributeDialogParser.Snapshot> snap = AttributeDialogParser.parse(title, actions);
        if (snap.isEmpty()) {
            return false;
        }

        skillPoints = snap.get().skillPoints();
        costs.clear();
        costs.putAll(snap.get().costs());
        bases.clear();
        Map<String, Integer> parsedBases = AttributeDialogParser.parseBases(actionTooltips);
        if (parsedBases.isEmpty() && body != null && !body.getString().isEmpty()) {
            parsedBases = AttributeDialogParser.parseBases(body);
        }

        bases.putAll(parsedBases);
        sources.clear();
        sources.putAll(AttributeDialogParser.parseSources(actionTooltips));
        received = true;
        return true;
    }

    private static int triggerOf(ActionButton button) {
        return button.action().isPresent()
                && button.action().get() instanceof StaticAction staticAction
                && staticAction.value() instanceof RunCommand runCommand
            ? Att2Triggers.parseTriggerCommand(runCommand.command())
            : -1;
    }
}
