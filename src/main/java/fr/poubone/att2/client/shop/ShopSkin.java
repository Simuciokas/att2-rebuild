package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.quest.QuestBookSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShopSkin {
    private ShopSkin() {
    }

    public static void texture(GuiGraphics g, ShopType type, String asset, int x, int y, int w, int h, int sw, int sh) {
        int edge = Math.max(1, Math.min(6, Math.min(w, h) / 3));
        if (asset.equals("panel_main")) {
            QuestBookSkin.panel(g, "cover", x, y, w, h, 100, 100, edge);
            int inset = Math.max(3, w / 90);
            int top = h * 140 / 880;
            QuestBookSkin.panel(g, "page", x + inset, y + top, w - 2 * inset, h - top - inset, 100, 100, edge);
        } else if (asset.equals("emblem")) {
            Item item = switch (type) {
                case FOOD -> Items.BREAD;
                case FISH -> Items.COD;
                case BLACKSMITH, MINER -> Items.ANVIL;
                case FLETCHER -> Items.BOW;
                case ALCHEMIST -> Items.BREWING_STAND;
                case STABLE -> Items.SADDLE;
                case TAILOR, CHARLES -> Items.LEATHER;
                case DAHAL, RUNES -> Items.ENCHANTED_BOOK;
                default -> Items.CHEST;
            };
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(w / 16.0F, h / 16.0F);
            g.renderItem(new ItemStack(item), 0, 0);
            g.pose().popMatrix();
        } else if (!asset.equals("ambiance")) {
            if (asset.equals("separator")) {
                g.fill(x, y + h / 2, x + w, y + h / 2 + 1, -4216453);
            } else if (!asset.equals("close_icon") && !asset.startsWith("arrow_")) {
                if (!asset.startsWith("button_") && !asset.startsWith("category_")) {
                    QuestBookSkin.panel(g, asset.equals("wallet") ? "cover" : "page", x, y, w, h, 100, 100, edge);
                    if (asset.equals("card_hover")) {
                        g.fill(x + edge, y + edge, x + w - edge, y + h - edge, 581470036);
                    }
                } else {
                    boolean selected = asset.endsWith("selected") || asset.startsWith("button_buy_") && asset.endsWith("hover");
                    String sprite = selected ? "button_selected" : (asset.endsWith("hover") ? "button_pressed" : "button");
                    QuestBookSkin.panel(g, sprite, x, y, w, h, 190, sprite.equals("button_pressed") ? 45 : 49, edge);
                    if (asset.endsWith("disabled")) {
                        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, -1997675832);
                    }
                }
            } else {
                QuestBookSkin.icon(g, asset.equals("close_icon") ? "close" : (asset.equals("arrow_left") ? "previous" : "next"), x, y, w, h);
            }
        }
    }
}
