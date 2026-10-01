package fr.poubone.att2.client.miner;

import java.util.Arrays;
import java.util.OptionalInt;

public final class MinerPrefetch {
    public static final int[] DEFAULT_TRIGGERS = new int[]{3489, 3488, 3535};
    public static final int DEFAULT_DELAY_TICKS = 2;
    private final int[] triggers;
    private final int delayTicks;
    private int nextIndex;
    private int ticksUntilNext;
    private boolean cancelled;
    private boolean done;

    public MinerPrefetch() {
        this(DEFAULT_TRIGGERS, 2);
    }

    public MinerPrefetch(int[] triggers, int delayTicks) {
        this.triggers = Arrays.copyOf(triggers, triggers.length);
        this.delayTicks = delayTicks;
        this.ticksUntilNext = 0;
    }

    public boolean isActive() {
        return !this.done && !this.cancelled;
    }

    public OptionalInt tick() {
        if (this.done || this.cancelled) {
            return OptionalInt.empty();
        }

        if (this.ticksUntilNext > 0) {
            this.ticksUntilNext--;
            return OptionalInt.empty();
        }

        int trigger = this.triggers[this.nextIndex++];
        if (this.nextIndex >= this.triggers.length) {
            this.done = true;
        } else {
            this.ticksUntilNext = this.delayTicks;
        }

        return OptionalInt.of(trigger);
    }

    public void cancel() {
        this.cancelled = true;
        this.done = true;
    }

    public boolean isDone() {
        return this.done;
    }
}
