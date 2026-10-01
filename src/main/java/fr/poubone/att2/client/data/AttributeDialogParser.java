package fr.poubone.att2.client.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class AttributeDialogParser {
    public static final String TITLE_KEY = "consciousness.stat.title";
    public static final String ADD_KEY = "consciousness.stat.point.add";
    public static final String ADD_ERROR_KEY = "consciousness.stat.point.add.error";
    public static final String MAX_KEY = "consciousness.stat.point.max";
    public static final String POINT_KEY = "consciousness.stat.point";
    public static final String BASE_KEY = "consciousness.stat.point.base";
    public static final List<String> SOURCE_SUFFIXES = List.of("base", "equipment", "spell", "enchantment", "potion", "environment", "food", "legendary");
    public static final List<String> CONSCIENCE_ORDER = List.of("STR", "CRT", "RES", "HAS", "SPD", "HER", "DAR", "LUC", "HUN");

    private AttributeDialogParser() {
    }

    public static Optional<AttributeDialogParser.Snapshot> parse(Component title, List<AttributeDialogParser.Action> actions) {
        if (title != null && isAttributeTitle(title)) {
            int skillPoints = skillPoints(title).orElse(0);
            Map<String, Integer> costs = new HashMap<>();
            if (actions != null) {
                for (AttributeDialogParser.Action action : actions) {
                    if (action != null) {
                        String stat = statKeyForTrigger(action.trigger());
                        if (stat != null) {
                            if (isMaxed(action.label())) {
                                costs.put(stat, 0);
                            } else {
                                upgradeCost(action.label()).ifPresent(cost -> costs.put(stat, cost));
                            }
                        }
                    }
                }
            }

            return Optional.of(new AttributeDialogParser.Snapshot(skillPoints, Map.copyOf(costs)));
        } else {
            return Optional.empty();
        }
    }

    public static Map<String, Integer> parseBases(Component body) {
        List<TranslatableContents> found = new ArrayList<>();
        collectTranslatable(body, "consciousness.stat.point.base", found);
        Map<String, Integer> bases = new HashMap<>();

        for (int i = 0; i < found.size() && i < CONSCIENCE_ORDER.size(); i++) {
            TranslatableContents contents = found.get(i);
            if (contents.getArgs().length != 0) {
                String statKey = CONSCIENCE_ORDER.get(i);
                argToInt(contents.getArgs()[0]).ifPresent(value -> bases.put(statKey, value));
            }
        }

        return Map.copyOf(bases);
    }

    public static Map<String, Map<String, Integer>> parseSources(Component tooltips) {
        List<TranslatableContents> found = new ArrayList<>();
        collectSources(tooltips, found);
        List<Map<String, Integer>> groups = new ArrayList<>();
        Map<String, Integer> current = null;

        for (TranslatableContents contents : found) {
            String suffix = sourceSuffix(contents.getKey());
            if (suffix != null && contents.getArgs().length != 0) {
                OptionalInt value = argToInt(contents.getArgs()[0]);
                if (!value.isEmpty()) {
                    if ("base".equals(suffix) || current == null) {
                        current = new LinkedHashMap<>();
                        groups.add(current);
                    }

                    current.put(suffix, value.getAsInt());
                }
            }
        }

        Map<String, Map<String, Integer>> out = new HashMap<>();

        for (int i = 0; i < groups.size() && i < CONSCIENCE_ORDER.size(); i++) {
            out.put(CONSCIENCE_ORDER.get(i), Map.copyOf(groups.get(i)));
        }

        return Map.copyOf(out);
    }

    public static boolean isAttributeTitle(Component title) {
        return findTranslatable(title, "consciousness.stat.title") != null;
    }

    public static OptionalInt skillPoints(Component title) {
        TranslatableContents contents = findTranslatable(title, "consciousness.stat.title");
        return contents != null && contents.getArgs().length != 0 ? argToInt(contents.getArgs()[0]) : OptionalInt.empty();
    }

    public static OptionalInt upgradeCost(Component label) {
        TranslatableContents add = findTranslatable(label, "consciousness.stat.point.add");
        if (add == null) {
            add = findTranslatable(label, "consciousness.stat.point.add.error");
        }

        return add != null && add.getArgs().length != 0 ? argToInt(add.getArgs()[0]) : OptionalInt.empty();
    }

    public static boolean isMaxed(Component label) {
        return findTranslatable(label, "consciousness.stat.point.max") != null;
    }

    public static String statKeyForTrigger(int trigger) {
        for (Entry<String, Integer> entry : Att2Triggers.STAT_UPGRADE.entrySet()) {
            if (entry.getValue() == trigger) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        if (component == null) {
            return null;
        } else if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
            return translatable;
        } else {
            if (component.getContents() instanceof TranslatableContents translatable) {
                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component inner) {
                        TranslatableContents nested = findTranslatable(inner, key);
                        if (nested != null) {
                            return nested;
                        }
                    }
                }
            }

            for (Component sibling : component.getSiblings()) {
                TranslatableContents nested = findTranslatable(sibling, key);
                if (nested != null) {
                    return nested;
                }
            }

            return null;
        }
    }

    private static String sourceSuffix(String key) {
        String prefix = "consciousness.stat.point.";
        if (key != null && key.startsWith(prefix)) {
            String suffix = key.substring(prefix.length());
            return SOURCE_SUFFIXES.contains(suffix) ? suffix : null;
        } else {
            return null;
        }
    }

    private static void collectSources(Component component, List<TranslatableContents> out) {
        if (component != null) {
            if (component.getContents() instanceof TranslatableContents translatable) {
                if (sourceSuffix(translatable.getKey()) != null) {
                    out.add(translatable);
                }

                for (Object arg : translatable.getArgs()) {
                    if (arg instanceof Component inner) {
                        collectSources(inner, out);
                    }
                }
            }

            for (Component sibling : component.getSiblings()) {
                collectSources(sibling, out);
            }
        }
    }

    private static void collectTranslatable(Component component, String key, List<TranslatableContents> out) {
        if (component != null) {
            if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
                out.add(translatable);
            } else {
                if (component.getContents() instanceof TranslatableContents translatable) {
                    for (Object arg : translatable.getArgs()) {
                        if (arg instanceof Component inner) {
                            collectTranslatable(inner, key, out);
                        }
                    }
                }

                for (Component sibling : component.getSiblings()) {
                    collectTranslatable(sibling, key, out);
                }
            }
        }
    }

    private static OptionalInt argToInt(Object arg) {
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);

        try {
            return OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public record Action(Component label, int trigger) {
    }

    public record Snapshot(int skillPoints, Map<String, Integer> costs) {
        public int cost(String statKey) {
            return this.costs.getOrDefault(statKey, 0);
        }
    }
}
