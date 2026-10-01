package fr.poubone.att2.client.teleport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.resources.ResourceKey;

final class DimensionIds {
    static final String OVERWORLD = "minecraft:overworld";
    static final String NETHER = "minecraft:the_nether";
    static final String END = "minecraft:the_end";

    private DimensionIds() {
    }

    static String fromResourceKey(ResourceKey<?> key) {
        if (key == null) {
            return null;
        }

        for (String methodName : new String[]{"location", "getValue", "getLocation"}) {
            try {
                Method method = key.getClass().getMethod(methodName);
                Object result = method.invoke(key);
                String normalized = normalize(result == null ? null : result.toString());
                if (normalized != null) {
                    return normalized;
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException var8) {
            }
        }

        return normalize(key.toString());
    }

    static String normalize(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return null;
        }

        if (value.contains("minecraft:overworld")) {
            return "minecraft:overworld";
        }

        if (value.contains("minecraft:the_nether")) {
            return "minecraft:the_nether";
        }

        if (value.contains("minecraft:the_end")) {
            return "minecraft:the_end";
        }

        return switch (value) {
            case "overworld", "minecraft:overworld" -> "minecraft:overworld";
            case "nether", "the_nether", "minecraft:nether", "minecraft:the_nether" -> "minecraft:the_nether";
            case "end", "the_end", "minecraft:end", "minecraft:the_end" -> "minecraft:the_end";
            default -> value;
        };
    }
}
