package fr.poubone.att2.client.rune;

import java.util.OptionalInt;
import net.minecraft.ChatFormatting;

public final class RuneCardLogic {
    public static final int CARDS_PER_PAGE = 6;

    private RuneCardLogic() {
    }

    public static int pageCount(int itemCount) {
        return itemCount <= 0 ? 1 : (itemCount + 6 - 1) / 6;
    }

    public static int clampPage(int page, int pageCount) {
        return pageCount <= 0 ? 0 : Math.max(0, Math.min(page, pageCount - 1));
    }

    public static int gridCol(int localIndex) {
        return localIndex % 3;
    }

    public static int gridRow(int localIndex) {
        return localIndex / 3;
    }

    public static ChatFormatting stockColor(int have, int need, OptionalInt pouch) {
        if (have >= need) {
            return ChatFormatting.GREEN;
        } else if (pouch.isEmpty()) {
            return ChatFormatting.GRAY;
        } else {
            return have + pouch.getAsInt() >= need ? ChatFormatting.GOLD : ChatFormatting.RED;
        }
    }
}
