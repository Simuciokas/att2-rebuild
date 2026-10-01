package fr.poubone.att2.client.data;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MapStatBarParser {
    public static final String TRANSLATION_PREFIX = "att2.stat.display.";
    private static final Set<String> POINT_SUFFIXES = Set.of("str", "crt", "res", "spd", "has", "her", "dar", "luc", "hun");
    private static final Pattern SIGNED_INT = Pattern.compile("-?\\d+");
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern FLAT_POINT = Pattern.compile("(STR|CRT|RES|SPD|HAS|HER|DAR|LUC|HUN):(-?\\d+)");

    private MapStatBarParser() {
    }

    public static boolean isStatDisplayTranslation(String key) {
        return key != null && key.startsWith("att2.stat.display.");
    }

    public static boolean isDetailedValueTranslation(String key) {
        return isStatDisplayTranslation(key) && key.contains(".value");
    }

    public static Optional<String> objectiveFromTranslationKey(String key) {
        if (!isStatDisplayTranslation(key)) {
            return Optional.empty();
        }

        String suffix = key.substring("att2.stat.display.".length());
        return !POINT_SUFFIXES.contains(suffix) ? Optional.empty() : Optional.of(suffix.toUpperCase(Locale.ROOT) + "_TOT");
    }

    public static OptionalInt parseSignedInt(String raw) {
        if (raw != null && !raw.isEmpty()) {
            String plain = FORMATTING.matcher(raw).replaceAll("").trim();
            Matcher matcher = SIGNED_INT.matcher(plain);
            if (!matcher.find()) {
                return OptionalInt.empty();
            }

            try {
                return OptionalInt.of(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        } else {
            return OptionalInt.empty();
        }
    }

    public static void collect(String translationKey, List<String> args, Map<String, Integer> out) {
        if (out != null && args != null && !args.isEmpty()) {
            Optional<String> objective = objectiveFromTranslationKey(translationKey);
            if (!objective.isEmpty()) {
                OptionalInt value = parseSignedInt(args.getFirst());
                if (!value.isEmpty()) {
                    out.put(objective.get(), value.getAsInt());
                }
            }
        }
    }

    public static void collectFromFlattened(String flattened, Map<String, Integer> out) {
        if (out != null && flattened != null && !flattened.isEmpty()) {
            String plain = FORMATTING.matcher(flattened).replaceAll("");
            Matcher matcher = FLAT_POINT.matcher(plain);

            while (matcher.find()) {
                try {
                    out.put(matcher.group(1) + "_TOT", Integer.parseInt(matcher.group(2)));
                } catch (NumberFormatException var5) {
                }
            }
        }
    }

    public static boolean looksLikePointTotals(String flattened) {
        if (flattened != null && !flattened.isEmpty()) {
            Map<String, Integer> parsed = new HashMap<>();
            collectFromFlattened(flattened, parsed);
            return parsed.size() >= 2;
        } else {
            return false;
        }
    }

    public static boolean looksLikeStatBarText(String flattened) {
        if (flattened == null || flattened.isEmpty()) {
            return false;
        }

        if (looksLikePointTotals(flattened)) {
            return true;
        }

        String plain = FORMATTING.matcher(flattened).replaceAll("");
        return plain.contains("Dahal") && plain.contains("|");
    }
}
