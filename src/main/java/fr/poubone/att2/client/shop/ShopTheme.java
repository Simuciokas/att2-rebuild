package fr.poubone.att2.client.shop;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class ShopTheme {
    private ShopTheme() {
    }

    public static void texture(GuiGraphics graphics, ShopType type, String asset, int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        String base = switch (type) {
            case RUNES -> "dahal";
            case CHARLES -> "tailor";
            case MINER -> "blacksmith";
            default -> type.id;
        };
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            Identifier.fromNamespaceAndPath("att2", "textures/shop/themes/" + base + "/" + asset + ".png"),
            x,
            y,
            0.0F,
            0.0F,
            width,
            height,
            sourceWidth,
            sourceHeight,
            sourceWidth,
            sourceHeight
        );
    }

    public static void text(GuiGraphics graphics, Component text, int x, int y, int width, float scale, int color, boolean centered) {
        if (width > 0) {
            Font font = Minecraft.getInstance().font;
            int limit = Math.max(1, (int)(width / scale));
            Component line = text;
            if (font.width(text) > limit) {
                MutableComponent clipped = Component.empty();
                font.substrByWidth(text, Math.max(0, limit - font.width("…"))).visit((style, part) -> {
                    clipped.append(Component.literal(part).withStyle(style));
                    return Optional.empty();
                }, Style.EMPTY);
                line = clipped.append("…");
            }

            float left = centered ? x + (width - font.width(line) * scale) / 2.0F : x;
            graphics.pose().pushMatrix();
            graphics.pose().translate(left, y);
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, line, 0, 0, color, false);
            graphics.pose().popMatrix();
        }
    }
}
