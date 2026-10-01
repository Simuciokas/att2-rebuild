package fr.poubone.att2.client.data;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

final class DialogComponents {
    private static final Pattern INT = Pattern.compile("-?\\d+");
    private static final Pattern SLASH = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

    private DialogComponents() {
    }

    static TranslatableContents findTranslatable(Component component, String key) {
        if (component != null && key != null) {
            if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)) {
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
        } else {
            return null;
        }
    }

    static boolean hasKey(Component component, String key) {
        return findTranslatable(component, key) != null;
    }

    static boolean hasKeyOrChild(Component component, String key) {
        return component != null && key != null && !key.isEmpty() ? hasKey(component, key) || hasKeyStartingWith(component, key + ".") : false;
    }

    static boolean hasKeyStartingWith(Component component, String prefix) {
        if (component != null && prefix != null && !prefix.isEmpty()) {
            if (component.getContents() instanceof TranslatableContents translatable && translatable.getKey().startsWith(prefix)) {
                return true;
            } else {
                if (component.getContents() instanceof TranslatableContents translatable) {
                    for (Object arg : translatable.getArgs()) {
                        if (arg instanceof Component inner && hasKeyStartingWith(inner, prefix)) {
                            return true;
                        }
                    }
                }

                for (Component sibling : component.getSiblings()) {
                    if (hasKeyStartingWith(sibling, prefix)) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    static OptionalInt argInt(TranslatableContents contents, int index) {
        return contents != null && index >= 0 && index < contents.getArgs().length ? parseInt(contents.getArgs()[index]) : OptionalInt.empty();
    }

    static OptionalInt parseInt(Object arg) {
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);
        if (text == null) {
            return OptionalInt.empty();
        }

        Matcher matcher = INT.matcher(text.replace("§", ""));
        if (!matcher.find()) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.parseInt(matcher.group().replaceAll("[^0-9-]", "")));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    static OptionalInt slashDenominator(Component component) {
        if (component == null) {
            return OptionalInt.empty();
        }

        Matcher matcher = SLASH.matcher(component.getString());
        if (!matcher.find()) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
