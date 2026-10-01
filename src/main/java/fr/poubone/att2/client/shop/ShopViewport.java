package fr.poubone.att2.client.shop;

public record ShopViewport(float scale, float left, float top) {
    public static ShopViewport fit(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        float scale = Math.max(0.01F, Math.min(1.0F, Math.min((screenWidth - 12.0F) / panelWidth, (screenHeight - 12.0F) / panelHeight)));
        return new ShopViewport(scale, (screenWidth - panelWidth * scale) / 2.0F, (screenHeight - panelHeight * scale) / 2.0F);
    }

    public double localX(double x) {
        return (x - this.left) / this.scale;
    }

    public double localY(double y) {
        return (y - this.top) / this.scale;
    }

    public int screenX(double x) {
        return Math.round((float)x * this.scale + this.left);
    }

    public int screenY(double y) {
        return Math.round((float)y * this.scale + this.top);
    }
}
