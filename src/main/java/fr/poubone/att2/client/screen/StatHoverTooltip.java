package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class StatHoverTooltip {
    private static final int PAD = 10;
    private static final int TITLE_H = 14;
    private static final int LINE_H = 11;
    private static final int MIN_BOX_W = 120;
    private static final int CURSOR_OFFSET_X = 12;
    private static final int CURSOR_OFFSET_Y = 12;

    private StatHoverTooltip() {
    }

    public static void render(GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY, Component current, Component next, Component costOrNull) {
        render(g, screenW, screenH, mouseX, mouseY, current, next, costOrNull, List.of());
    }

    public static void render(
        GuiGraphics g, int screenW, int screenH, int mouseX, int mouseY, Component current, Component next, Component costOrNull, List<Component> sources
    ) {
        Font font = Minecraft.getInstance().font;
        Component title = ModLanguageManager.get("screen.stat_upgrade.tooltip.title");
        if (sources == null) {
            sources = List.of();
        }

        int textW = font.width(title);
        textW = Math.max(textW, font.width(current));
        textW = Math.max(textW, font.width(next));
        if (costOrNull != null) {
            textW = Math.max(textW, font.width(costOrNull));
        }

        for (Component line : sources) {
            textW = Math.max(textW, font.width(line));
        }

        int boxW = Math.max(120, textW + 20);
        int bodyLines = (costOrNull == null ? 2 : 3) + sources.size();
        int boxH = 14 + bodyLines * 11 + 20 + 4;
        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + boxW > screenW) {
            x = mouseX - 12 - boxW;
        }

        if (y + boxH > screenH) {
            y = mouseY - 12 - boxH;
        }

        if (x < 0) {
            x = 0;
        }

        if (y < 0) {
            y = 0;
        }

        if (x + boxW > screenW) {
            x = Math.max(0, screenW - boxW);
        }

        if (y + boxH > screenH) {
            y = Math.max(0, screenH - boxH);
        }

        g.fill(x + 2, y + 3, x + boxW + 2, y + boxH + 3, 1342177280);
        QuestBookSkin.panel(g, "cover", x, y, boxW, boxH, 100, 100, 6);
        QuestBookSkin.panel(g, "page", x + 3, y + 3, boxW - 6, boxH - 6, 100, 100, 4);
        int innerW = boxW - 20;
        ShopTheme.text(g, title, x + 10, y + 10 - 1, innerW, 1.0F, -9413049, false);
        int lineY = y + 10 + 14;
        ShopTheme.text(g, current, x + 10, lineY, innerW, 1.0F, -13096159, false);
        lineY += 11;
        ShopTheme.text(g, next, x + 10, lineY, innerW, 1.0F, -13096159, false);
        if (costOrNull != null) {
            lineY += 11;
            ShopTheme.text(g, costOrNull, x + 10, lineY, innerW, 1.0F, -13096159, false);
        }

        for (Component line : sources) {
            lineY += 11;
            ShopTheme.text(g, line, x + 10, lineY, innerW, 1.0F, -13096159, false);
        }
    }
}
