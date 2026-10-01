package fr.poubone.att2.client.input;

import java.util.ArrayDeque;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class InputSequencer {
    private static final ArrayDeque<InputSequencer.Step> QUEUE = new ArrayDeque<>();
    private static InputSequencer.Step current;
    private static int remainingTicks;
    private static int sneakTicks;
    private static boolean holdingUse;

    private InputSequencer() {
    }

    public static boolean isBusy() {
        return current != null || !QUEUE.isEmpty();
    }

    public static boolean isSneakForced() {
        return sneakTicks > 0;
    }

    public static void run(List<InputSequencer.Step> steps) {
        if (!isBusy()) {
            QUEUE.addAll(steps);
        }
    }

    public static void tick(Minecraft client) {
        if (client.player != null && client.screen == null) {
            if (sneakTicks > 0) {
                sneakTicks--;
            }

            if (current == null) {
                current = QUEUE.pollFirst();
                if (current == null) {
                    return;
                }

                remainingTicks = Math.max(1, current.ticks());
                start(client, current);
            }

            remainingTicks--;
            if (remainingTicks <= 0) {
                end(client, current);
                current = null;
            }
        } else {
            abort(client);
        }
    }

    private static void start(Minecraft client, InputSequencer.Step step) {
        switch (step.type()) {
            case FORCE_SNEAK:
                sneakTicks = step.ticks();
                remainingTicks = 1;
                break;
            case HOLD_USE:
                client.options.keyUse.setDown(true);
                holdingUse = true;
                break;
            case CLICK_SWAP:
                KeyMapping.click(KeyBindingHelper.getBoundKeyOf(client.options.keySwapOffhand));
            case WAIT:
        }
    }

    private static void end(Minecraft client, InputSequencer.Step step) {
        if (step.type() == InputSequencer.StepType.HOLD_USE) {
            releaseUse(client);
        }
    }

    public static void abort(Minecraft client) {
        QUEUE.clear();
        current = null;
        remainingTicks = 0;
        sneakTicks = 0;
        releaseUse(client);
    }

    private static void releaseUse(Minecraft client) {
        if (holdingUse) {
            holdingUse = false;
            client.options.keyUse.setDown(false);
        }
    }

    public record Step(InputSequencer.StepType type, int ticks) {
        public static InputSequencer.Step forceSneak(int ticks) {
            return new InputSequencer.Step(InputSequencer.StepType.FORCE_SNEAK, ticks);
        }

        public static InputSequencer.Step holdUse(int ticks) {
            return new InputSequencer.Step(InputSequencer.StepType.HOLD_USE, ticks);
        }

        public static InputSequencer.Step clickSwap() {
            return new InputSequencer.Step(InputSequencer.StepType.CLICK_SWAP, 1);
        }

        public static InputSequencer.Step wait(int ticks) {
            return new InputSequencer.Step(InputSequencer.StepType.WAIT, ticks);
        }
    }

    public enum StepType {
        FORCE_SNEAK,
        HOLD_USE,
        CLICK_SWAP,
        WAIT;
    }
}
