package fr.poubone.att2.client.shop;

public final class ShopPaging {
    private ShopPaging() {
    }

    public static int afterScroll(int page, int pages, double deltaY) {
        if (deltaY != 0.0 && pages > 1) {
            int next = page - (int)Math.signum(deltaY);
            return Math.max(0, Math.min(next, pages - 1));
        } else {
            return Math.max(0, page);
        }
    }
}
