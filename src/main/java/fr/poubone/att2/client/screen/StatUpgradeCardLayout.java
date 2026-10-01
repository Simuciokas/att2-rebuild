package fr.poubone.att2.client.screen;

public final class StatUpgradeCardLayout {
    static final int CARD_H = 72;
    static final int MIN_CARD_H = 48;
    private static final int ICON_SIZE = 16;
    private static final int PLUS_SIZE = 20;

    private StatUpgradeCardLayout() {
    }

    public static int plusTop(int cardHeight, int plusMargin) {
        return cardHeight - 20 - plusMargin;
    }

    static int clampLevelClearOfPlus(int levelY, int pad, int plusTop) {
        int maxLevelY = plusTop - 10;
        if (levelY > maxLevelY) {
            levelY = Math.min(pad + 2, maxLevelY);
        }

        return levelY;
    }

    public static StatUpgradeCardLayout.Interior layout(int h, boolean totDiffers) {
        int pad = scaleCard(h, 6);
        int plusMargin = scaleCard(h, 4);
        int plusTop = plusTop(h, plusMargin);
        int levelY = pad + scaleCard(h, 18);
        int costY = h - scaleCard(h, 18);
        boolean showTot = totDiffers && h >= 60;
        if (showTot) {
            int totY = pad + scaleCard(h, 30);
            if (totY + 10 <= costY) {
                levelY = clampLevelClearOfPlus(levelY, pad, plusTop);
                return new StatUpgradeCardLayout.Interior(pad, levelY, totY, costY, plusMargin, true);
            }

            showTot = false;
        }

        if (levelY + 10 <= plusTop && costY + 9 <= h && costY < plusTop) {
            if (costY < levelY + 10) {
                costY = plusTop + Math.max(0, 5);
            }

            levelY = clampLevelClearOfPlus(levelY, pad, plusTop);
            return new StatUpgradeCardLayout.Interior(pad, levelY, -1, costY, plusMargin, false);
        } else {
            int iconRow = Math.min(16, h / 3);
            levelY = pad + iconRow + 2;
            costY = plusTop + Math.max(0, 5);
            if (levelY + 9 > costY) {
                levelY = pad + 2;
            }

            levelY = clampLevelClearOfPlus(levelY, pad, plusTop);
            return new StatUpgradeCardLayout.Interior(pad, levelY, -1, costY, plusMargin, false);
        }
    }

    static int scaleCard(int cardHeight, int nominalAt72) {
        return Math.max(2, (nominalAt72 * cardHeight + 36) / 72);
    }

    public record Interior(int pad, int levelY, int totY, int costY, int plusMargin, boolean showTot) {
    }
}
