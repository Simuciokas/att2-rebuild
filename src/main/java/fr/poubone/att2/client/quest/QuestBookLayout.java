package fr.poubone.att2.client.quest;

public record QuestBookLayout(int x, int y, int width, int height) {
    public static QuestBookLayout fit(int screenWidth, int screenHeight) {
        int width = Math.min(660, Math.max(1, screenWidth - 16));
        int height = Math.min(390, Math.max(1, screenHeight - 16));
        return new QuestBookLayout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    public int pageWidth() {
        return (this.width - 26) / 2;
    }

    public int contentWidth() {
        return this.pageWidth() - 24;
    }

    public int leftX() {
        return this.x + 20;
    }

    public int rightX() {
        return this.x + this.width / 2 + 17;
    }

    public int detailTop() {
        return this.y + 70;
    }

    public int detailBottom() {
        return this.y + this.height - 39;
    }

    public int searchY() {
        return this.y + 44;
    }

    public int filterY() {
        return this.y + 70;
    }

    public int filterColumns() {
        return this.contentWidth() < 236 && this.height >= 210 ? 2 : 4;
    }

    public int filterWidth() {
        return (this.contentWidth() - (this.filterColumns() - 1) * 4) / this.filterColumns();
    }

    public int listTop() {
        return this.filterY() + 4 / this.filterColumns() * 22 + 6;
    }

    public int footerY() {
        return this.y + this.height - 34;
    }

    public int rowHeight() {
        return this.height < 210 ? 24 : (this.width >= 500 ? 34 : 30);
    }

    public int rowsPerPage() {
        return Math.max(1, (this.footerY() - 8 - this.listTop()) / this.rowHeight());
    }

    public float textScale() {
        return this.width >= 500 ? 1.15F : 1.0F;
    }
}
