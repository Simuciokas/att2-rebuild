package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.quest.QuestStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ModToast {
    private static final int FADE_IN = 4;
    private static final int HOLD = 70;
    private static final int FADE_OUT = 12;
    private static final int LIFE = 86;
    private static Component title;
    private static Component subtitle;
    private static ModToast.Kind kind = ModToast.Kind.INFO;
    private static int ticks = -1;

    private ModToast() {
    }

    public static void reset() {
        title = null;
        subtitle = null;
        kind = ModToast.Kind.INFO;
        ticks = -1;
    }

    public static void showError(Component text) {
        show(text, null, ModToast.Kind.ERROR);
    }

    public static void showOk(Component text) {
        show(text, null, ModToast.Kind.OK);
    }

    public static void showInfo(Component text) {
        show(text, null, ModToast.Kind.INFO);
    }

    public static void showBanner(Component titleText, Component subtitleText) {
        show(titleText, subtitleText, ModToast.Kind.INFO);
    }

    public static void show(Component titleText, Component subtitleText, ModToast.Kind toastKind) {
        if (titleText != null) {
            title = titleText;
            subtitle = subtitleText;
            kind = toastKind == null ? ModToast.Kind.INFO : toastKind;
            ticks = 0;
        }
    }

    public static void tick() {
        if (ticks >= 0) {
            if (++ticks >= 86) {
                reset();
            }
        }
    }

    public static boolean isActive() {
        return ticks >= 0 && title != null;
    }

    public static void render(GuiGraphics graphics) {
        if (isActive() && visible()) {
            Minecraft client = Minecraft.getInstance();
            Font font = client.font;
            String name = title.getString();
            String tag = subtitle == null ? "" : subtitle.getString();
            int nameW = font.width(name);
            int tagW = tag.isEmpty() ? 0 : font.width(tag);
            int textW = Math.max(nameW, tagW);
            int padX = 18;
            int boxW = Math.min(client.getWindow().getGuiScaledWidth() - 24, Math.max(160, textW + padX * 2));
            int boxH = tag.isEmpty() ? 28 : 40;
            int x = (client.getWindow().getGuiScaledWidth() - boxW) / 2;
            int y = 12;
            graphics.fill(x + 3, y + 4, x + boxW + 3, y + boxH + 4, 1342177280);
            QuestBookSkin.panel(graphics, "cover", x, y, boxW, boxH, 100, 100, 6);
            int inset = 4;
            QuestBookSkin.panel(graphics, "page", x + inset, y + inset, boxW - 2 * inset, boxH - 2 * inset, 100, 100, 4);

            int accent = switch (kind) {
                case ERROR -> QuestBookSkin.statusColor(QuestStatus.FAILED);
                case OK -> QuestBookSkin.statusColor(QuestStatus.COMPLETED);
                case INFO -> -4216453;
            };
            graphics.fill(x + inset + 2, y + inset + 2, x + boxW - inset - 2, y + inset + 4, accent);
            int nameColor = -13096159;
            int nameX = x + (boxW - nameW) / 2;
            int nameY = tag.isEmpty() ? y + (boxH - 8) / 2 : y + 9;
            graphics.drawString(font, name, nameX, nameY, nameColor, false);
            if (!tag.isEmpty()) {
                graphics.drawString(font, tag, x + (boxW - tagW) / 2, y + 22, -9413049, false);
            }
        }
    }

    private static boolean visible() {
        return ticks < 0 ? false : ticks >= 4 && ticks < 74;
    }

    public enum Kind {
        ERROR,
        OK,
        INFO;
    }
}
