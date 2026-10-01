package fr.poubone.att2.client.shop;

public final class ShopHover {
    private ShopHover() {
    }

    public static boolean showsTooltip(boolean hovered, boolean focused) {
        return hovered;
    }

    public static String pick(ShopHover.Widget... widgets) {
        String slot = null;
        String action = null;

        for (ShopHover.Widget widget : widgets) {
            if (showsTooltip(widget.hovered, widget.focused)) {
                if (widget.kind == ShopHover.Kind.SLOT) {
                    slot = widget.id;
                } else {
                    action = widget.id;
                }
            }
        }

        return slot != null ? slot : action;
    }

    public enum Kind {
        SLOT,
        ACTION;
    }

    public record Widget(String id, ShopHover.Kind kind, boolean hovered, boolean focused) {
    }
}
