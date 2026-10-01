package fr.poubone.att2.client.quest;

import net.minecraft.resources.Identifier;

public final class QuestTextures {
    public static final int PANEL_WIDTH = 512;
    public static final int PANEL_HEIGHT = 256;
    public static final int LEFT_X = 58;
    public static final int LEFT_Y = 36;
    public static final int LEFT_W = 164;
    public static final int LEFT_H = 176;
    public static final int RIGHT_X = 290;
    public static final int RIGHT_Y = 36;
    public static final int RIGHT_W = 164;
    public static final int RIGHT_H = 176;
    public static final int TITLE = -4684245;
    public static final int TEXT = -14017520;
    public static final int SECONDARY = -10861004;
    public static final int ENTRY = 858397712;
    public static final int ENTRY_HOVER = 1430267928;
    public static final int SELECTED = 1724162128;
    public static final int SELECTED_HOVER = -1999327136;
    public static final int TRACKED = 1432387648;
    public static final int TRACKED_HOVER = 1953538128;
    public static final Identifier PANEL = Identifier.fromNamespaceAndPath("att2", "textures/quest/panel.png");
    public static final int ICON_SRC = 64;

    private QuestTextures() {
    }

    public static Identifier icon(String name, boolean hover) {
        String file = hover ? name + "_hover" : name;
        return Identifier.fromNamespaceAndPath("att2", "textures/quest/icon/" + file + ".png");
    }

    public static int statusColor(QuestStatus status) {
        return switch (status) {
            case STARTED -> -1522326;
            case COMPLETED -> -9712774;
            case FAILED -> -3909046;
            case UNKNOWN -> -7701912;
        };
    }
}
