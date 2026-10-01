package fr.poubone.att2.client.hud;

public final class SpellBarLayout {
    private SpellBarLayout() {
    }

    public static int iconX(int boxX, int boxW, int cell, int screenW, int column, int stride) {
        boolean left = boxX + boxW / 2 < screenW / 2;
        return left ? boxX + column * stride : boxX + boxW - cell - column * stride;
    }
}
