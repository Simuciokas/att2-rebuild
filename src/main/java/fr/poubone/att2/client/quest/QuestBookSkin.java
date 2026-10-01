package fr.poubone.att2.client.quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class QuestBookSkin {
    public static final int INK = -13096159;
    public static final int MUTED = -9413049;
    public static final int RULE = -4216453;
    public static final int CREAM = -660784;

    private QuestBookSkin() {
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("att2", "textures/quest_book/kenney/" + name + ".png");
    }

    public static void book(GuiGraphics g, QuestBookLayout l) {
        g.fill(l.x() + 5, l.y() + 6, l.x() + l.width() + 5, l.y() + l.height() + 6, 1610612736);
        panel(g, "cover", l.x(), l.y(), l.width(), l.height(), 100, 100, 6);
        int py = l.y() + 36;
        int ph = l.height() - 44;
        int pw = l.pageWidth();

        for (int px : new int[]{l.x() + 8, l.x() + l.width() / 2 + 5}) {
            panel(g, "page", px, py + 3, pw, ph, 100, 100, 5);
            panel(g, "page", px, py, pw, ph - 2, 100, 100, 5);
        }

        int spine = l.x() + l.width() / 2;
        g.fill(spine - 4, py + 3, spine + 4, py + ph - 2, -9219784);
        g.fill(spine - 2, py + 4, spine + 1, py + ph - 3, -5798827);
        g.fill(spine + 12, py - 5, spine + 22, py + 3, -8637392);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        String name = selected ? "button_selected" : (hovered ? "button_pressed" : "button");
        panel(g, name, x, y, w, h, 190, hovered && !selected ? 45 : 49, 6);
    }

    public static void icon(GuiGraphics g, String name, int x, int y, int w, int h) {
        int sw = !name.equals("previous") && !name.equals("next") ? 16 : 22;
        int sh = sw == 22 ? 21 : 15;
        g.blit(RenderPipelines.GUI_TEXTURED, texture(name), x, y, 0.0F, 0.0F, w, h, sw, sh, sw, sh);
    }

    public static int statusColor(QuestStatus status) {
        return switch (status) {
            case STARTED -> -8104171;
            case COMPLETED -> -13214411;
            case FAILED -> -7193294;
            case UNKNOWN -> -9413049;
        };
    }

    public static void panel(GuiGraphics g, String name, int x, int y, int w, int h, int sw, int sh, int edge) {
        int[] dx = new int[]{x, x + edge, x + w - edge};
        int[] dy = new int[]{y, y + edge, y + h - edge};
        int[] dw = new int[]{edge, w - 2 * edge, edge};
        int[] dh = new int[]{edge, h - 2 * edge, edge};
        int[] sx = new int[]{0, edge, sw - edge};
        int[] sy = new int[]{0, edge, sh - edge};
        int[] rw = new int[]{edge, sw - 2 * edge, edge};
        int[] rh = new int[]{edge, sh - 2 * edge, edge};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dw[col] > 0 && dh[row] > 0) {
                    g.blit(RenderPipelines.GUI_TEXTURED, texture(name), dx[col], dy[row], sx[col], sy[row], dw[col], dh[row], rw[col], rh[row], sw, sh);
                }
            }
        }
    }
}
