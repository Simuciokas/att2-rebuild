package fr.poubone.att2.client.data;

import java.util.OptionalInt;

public final class DahalAmount {
    private DahalAmount() {
    }

    public static OptionalInt current(float progress, int max) {
        if (max > 0 && !(progress < 0.0F) && !Float.isNaN(progress)) {
            float clamped = Math.min(1.0F, progress);
            return OptionalInt.of(Math.round(clamped * max));
        } else {
            return OptionalInt.empty();
        }
    }
}
