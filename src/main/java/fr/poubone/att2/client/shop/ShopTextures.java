package fr.poubone.att2.client.shop;

import net.minecraft.resources.Identifier;

public final class ShopTextures {
    public static final int PANEL_WIDTH = 512;
    public static final int PANEL_HEIGHT = 256;
    public static final int SLOT_SRC = 64;
    public static final Identifier SLOT = id("slot");
    public static final Identifier SLOT_HOVER = id("slot_hover");
    public static final Identifier COIN = id("coin");
    public static final int COIN_SRC = 64;

    private ShopTextures() {
    }

    private static Identifier id(String file) {
        return Identifier.fromNamespaceAndPath("att2", "textures/shop/" + file + ".png");
    }
}
