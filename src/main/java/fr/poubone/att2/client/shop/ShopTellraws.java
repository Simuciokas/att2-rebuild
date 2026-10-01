package fr.poubone.att2.client.shop;

import java.util.Locale;

final class ShopTellraws {
    private ShopTellraws() {
    }

    static boolean isForceResetAction(String raw) {
        if (raw != null && !raw.isBlank()) {
            String lower = raw.toLowerCase(Locale.ROOT);
            return lower.contains("force store reset")
                || lower.contains("forcer la ré-initialisation")
                || lower.contains("forcer la re-initialisation")
                || lower.contains("forcer la réinitialisation");
        } else {
            return false;
        }
    }

    static boolean isRemainingTimer(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }

        if (isForceResetAction(raw)) {
            return false;
        }

        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("ré-initialisation")
            || lower.contains("re-initialisation")
            || lower.contains("shop reset")
            || lower.contains("réinitialisation des magasins");
    }
}
