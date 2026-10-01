package fr.poubone.att2.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class CityToast {
    private CityToast() {
    }

    public static void reset() {
    }

    public static void tick() {
    }

    public static boolean isActive() {
        return ModToast.isActive();
    }

    public static boolean consumeTitle(Component text) {
        String key = translationKey(text);
        if (!isCitywalkTitle(key)) {
            return false;
        }

        String id = key.substring("att2.citywalk.".length(), key.length() - ".title".length());
        ModToast.showBanner(text, Component.translatable("att2.citywalk." + id + ".subtitle"));
        return true;
    }

    public static boolean consumeSubtitle(Component text) {
        return isCitywalkSubtitle(translationKey(text));
    }

    public static void render(GuiGraphics graphics) {
    }

    private static boolean isCitywalkTitle(String key) {
        return key.startsWith("att2.citywalk.") && key.endsWith(".title");
    }

    private static boolean isCitywalkSubtitle(String key) {
        return key.startsWith("att2.citywalk.") && key.endsWith(".subtitle");
    }

    private static String translationKey(Component component) {
        if (component == null) {
            return "";
        } else if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        } else {
            for (Component sibling : component.getSiblings()) {
                String inner = translationKey(sibling);
                if (!inner.isEmpty()) {
                    return inner;
                }
            }

            return "";
        }
    }
}
