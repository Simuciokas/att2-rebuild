package fr.poubone.att2.client.data;

import java.util.Locale;
import java.util.Optional;

public final class StatEffectText {
    private static final String UNKNOWN = "screen.stat_upgrade.effect.unknown";
    private static final String HUN_HUNGER = "screen.stat_upgrade.effect.hun.hunger";
    private static final String HUN_NONE = "screen.stat_upgrade.effect.hun.none";
    private static final String HUN_NEUTRAL = "screen.stat_upgrade.effect.hun.neutral";
    private static final String HUN_SAT = "screen.stat_upgrade.effect.hun.sat";

    private StatEffectText() {
    }

    public static double hunSeconds(int timerTicks) {
        return timerTicks / 20.0;
    }

    public static StatEffectText.EffectView current(String statKey, int tot) {
        return current(statKey, tot, null);
    }

    public static StatEffectText.EffectView current(String statKey, int tot, StatEffectText.WeaponStats weapon) {
        return switch (statKey) {
            case "STR" -> strength(tot, weapon);
            case "CRT" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.crt", new Object[]{tot});
            case "RES" -> resistance(tot);
            case "HAS" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.has", new Object[]{5 * tot});
            case "SPD" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.spd", new Object[]{10 * tot});
            case "HER" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.her", new Object[]{String.format(Locale.ROOT, "%.1f", tot / 10.0)});
            case "DAR" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.dar", new Object[]{dahalPerSecond(tot)});
            case "LUC" -> new StatEffectText.EffectView("screen.stat_upgrade.effect.luc", new Object[]{String.format(Locale.ROOT, "%.1f", 0.5 * tot)});
            case "HUN" -> hunCurrent(tot);
            default -> new StatEffectText.EffectView("screen.stat_upgrade.effect.unknown", new Object[]{statKey});
        };
    }

    public static Optional<StatEffectText.EffectView> next(String statKey, int tot, int base) {
        return next(statKey, tot, base, null);
    }

    public static Optional<StatEffectText.EffectView> next(String statKey, int tot, int base, StatEffectText.WeaponStats weapon) {
        return base >= StatCaps.maxBase(statKey) ? Optional.empty() : Optional.of(current(statKey, tot + 1, weapon));
    }

    public static int strengthDamage(int strTot, int damageX100, int attackSpeedX1000) {
        int dps = 1000;
        dps += attackSpeedX1000;
        dps *= 4;
        dps *= damageX100;
        dps /= 1000;
        int percent = strTot * strTot * 6 + 1100;
        dps *= percent;
        dps /= 100000;
        int damage = strTot * 2;
        damage -= damageX100 / 100;
        return damage + dps;
    }

    public static int dahalPerSecond(int darTot) {
        int x = darTot * 10 / 3;
        int z = darTot * 100 % 3;
        int op = 15 * x + z;
        x = x * 10 + 100;
        op = op * x + 10000;
        return op / 10000;
    }

    private static StatEffectText.EffectView strength(int tot, StatEffectText.WeaponStats weapon) {
        return weapon == null
            ? new StatEffectText.EffectView("screen.stat_upgrade.effect.str.weapon", new Object[0])
            : new StatEffectText.EffectView("screen.stat_upgrade.effect.str", new Object[]{strengthDamage(tot, weapon.damageX100(), weapon.attackSpeedX1000())});
    }

    private static StatEffectText.EffectView resistance(int tot) {
        if (tot <= 0) {
            return new StatEffectText.EffectView("screen.stat_upgrade.effect.res.none", new Object[0]);
        } else {
            return tot >= 17
                ? new StatEffectText.EffectView("screen.stat_upgrade.effect.res.immune", new Object[0])
                : new StatEffectText.EffectView("screen.stat_upgrade.effect.res", new Object[]{2 * tot, 6 * tot});
        }
    }

    private static StatEffectText.EffectView hunCurrent(int tot) {
        if (tot <= -10) {
            return hunger(4, 1.5);
        } else if (tot <= -8) {
            return hunger(3, 2.0);
        } else if (tot <= -6) {
            return hunger(3, 2.5);
        } else if (tot == -5) {
            return hunger(2, 2.75);
        } else if (tot == -4) {
            return hunger(2, 3.25);
        } else if (tot == -3) {
            return hunger(2, 4.0);
        } else if (tot == -2) {
            return hunger(2, 5.0);
        } else if (tot == -1) {
            return hunger(2, 6.25);
        } else if (tot == 0) {
            return hunger(2, 7.5);
        } else if (tot == 1) {
            return hunger(2, 8.75);
        } else if (tot == 2) {
            return hunger(1, 10.0);
        } else if (tot == 3) {
            return new StatEffectText.EffectView("screen.stat_upgrade.effect.hun.none", new Object[0]);
        } else if (tot == 4) {
            return hunger(1, 12.5);
        } else if (tot == 5) {
            return hunger(1, 15.0);
        } else if (tot <= 7) {
            return new StatEffectText.EffectView("screen.stat_upgrade.effect.hun.neutral", new Object[0]);
        } else if (tot == 8) {
            return sat(1, 125.0);
        } else if (tot == 9) {
            return sat(1, 112.5);
        } else if (tot == 10) {
            return sat(1, 100.0);
        } else if (tot == 11) {
            return sat(1, 87.5);
        } else if (tot == 12) {
            return sat(1, 75.0);
        } else if (tot == 13) {
            return sat(1, 62.5);
        } else if (tot == 14) {
            return sat(2, 56.0);
        } else {
            return tot == 15 ? sat(2, 50.0) : sat(2, 40.0);
        }
    }

    private static StatEffectText.EffectView hunger(int level, double seconds) {
        return new StatEffectText.EffectView("screen.stat_upgrade.effect.hun.hunger", new Object[]{level, seconds});
    }

    private static StatEffectText.EffectView sat(int level, double seconds) {
        return new StatEffectText.EffectView("screen.stat_upgrade.effect.hun.sat", new Object[]{level, seconds});
    }

    public record EffectView(String langKey, Object[] args) {
    }

    public record WeaponStats(int damageX100, int attackSpeedX1000) {
    }
}
