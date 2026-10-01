package fr.poubone.att2.client.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

public enum QuestStatus {
    STARTED(ChatFormatting.YELLOW),
    COMPLETED(ChatFormatting.GREEN),
    FAILED(ChatFormatting.DARK_RED),
    UNKNOWN(ChatFormatting.GRAY);

    private final ChatFormatting formatting;

    QuestStatus(ChatFormatting formatting) {
        this.formatting = formatting;
    }

    public ChatFormatting formatting() {
        return this.formatting;
    }

    public static QuestStatus fromColor(TextColor color) {
        if (color == null) {
            return UNKNOWN;
        } else {
            int rgb = color.getValue();
            if (rgb == ChatFormatting.YELLOW.getColor()) {
                return STARTED;
            } else if (rgb == ChatFormatting.GREEN.getColor()) {
                return COMPLETED;
            } else {
                return rgb != ChatFormatting.DARK_RED.getColor() && rgb != ChatFormatting.RED.getColor() ? UNKNOWN : FAILED;
            }
        }
    }
}
