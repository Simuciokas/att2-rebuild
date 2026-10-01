package fr.poubone.att2.client.hud;

public final class StatIconsLayout {
    private final int iconSize;
    private final int textOffsetX;
    private final int spacingX;
    private final int spacingY;

    private StatIconsLayout(int iconSize, int textOffsetX, int spacingX, int spacingY) {
        this.iconSize = iconSize;
        this.textOffsetX = textOffsetX;
        this.spacingX = spacingX;
        this.spacingY = spacingY;
    }

    public static StatIconsLayout of(float scale, float gap, int numberWidth) {
        if (scale <= 0.0F) {
            scale = 1.0F;
        }

        if (gap <= 0.0F) {
            gap = 1.0F;
        }

        int iconSize = Math.max(6, Math.round(8.0F * scale));
        int textOffsetX = iconSize + 2;
        int minStrideX = textOffsetX + Math.max(0, numberWidth);
        int spacingX = Math.max(minStrideX, Math.round(42.0F * scale * gap));
        int spacingY = Math.max(Math.max(10, iconSize + 2), Math.round(12.0F * scale * gap));
        return new StatIconsLayout(iconSize, textOffsetX, spacingX, spacingY);
    }

    public int iconSize() {
        return this.iconSize;
    }

    public int textOffsetX() {
        return this.textOffsetX;
    }

    public int spacingX() {
        return this.spacingX;
    }

    public int spacingY() {
        return this.spacingY;
    }

    public int[] cell(int index, int startX, int startY) {
        return new int[]{startX + index * this.spacingX, startY};
    }
}
