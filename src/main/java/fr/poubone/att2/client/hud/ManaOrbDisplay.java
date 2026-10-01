package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.DahalAmount;
import fr.poubone.att2.client.data.MapStatBar;
import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.util.ModTextures;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class ManaOrbDisplay {
    private static final int ORB_SRC = 256;
    private static final float HOLE_CX = 127.47F;
    private static final float HOLE_CY = 114.75F;
    private static final float HOLE_R = 84.0F;

    public static void reset() {
        ManaGlobe.reset();
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            OptionalInt currentOpt = ScoreCache.get("DAHAL");
            OptionalInt maxOpt = ScoreCache.get("DAHALMAX");
            if (currentOpt.isEmpty() && maxOpt.isPresent() && MapStatBar.hasDahalProgress()) {
                currentOpt = DahalAmount.current(MapStatBar.dahalProgress(), maxOpt.getAsInt());
            }

            if (!currentOpt.isEmpty() && !maxOpt.isEmpty()) {
                int current = currentOpt.getAsInt();
                int max = Math.max(1, maxOpt.getAsInt());
                HudLayout.Box box = HudLayout.box("mana");
                int size = Math.max(24, Math.round(256.0F * HudLayout.contentScale("mana", 256, 268)));
                int x = box.x() + (box.w() - size) / 2;
                int y = box.y() + (box.h() - size) / 2;
                float ratio = Mth.clamp((float)current / max, 0.0F, 1.0F);
                double time = Util.getMillis() / 1000.0;
                float danger = ratio < 0.28F ? Mth.clamp((0.28F - ratio) / 0.28F, 0.0F, 1.0F) : 0.0F;
                float scale = size / 256.0F;
                float cx = x + 127.47F * scale;
                float cy = y + 114.75F * scale;
                float radius = 84.0F * scale;
                if (ratio > 0.15F) {
                    float pulse = ratio >= 0.98F ? 0.55F + 0.45F * (float)Math.sin(time * 4.2) : 0.35F + 0.15F * ratio;
                    int glowSize = Math.max(8, (int)(radius * 2.7F * (1.04F + 0.08F * pulse)));
                    int gx = Math.round(cx - glowSize * 0.5F);
                    int gy = Math.round(cy - glowSize * 0.5F);
                    int glowColor = ARGB.color((int)(pulse * (ratio >= 0.98F ? 120 : 70)), 55, 120, 255);
                    context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.FX_GLOW, gx, gy, 0.0F, 0.0F, glowSize, glowSize, glowSize, glowSize, glowColor);
                }

                ManaGlobe.bake(ratio, danger, time, 256, 127.47F, 114.75F, 84.0F);
                ManaGlobe.blitLiquid(context, x, y, size);
                context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.MANA_FRAME, x, y, 0.0F, 0.0F, size, size, 256, 256, 256, 256);
                String text = current + "/" + max;
                int textWidth = client.font.width(text);
                int textColor = danger > 0.4F ? lerpRgb(-1, -38037, danger) : -1;
                context.drawString(client.font, text, x + (size - textWidth) / 2, y + size - 4, textColor, true);
            }
        }
    }

    private static int lerpRgb(int from, int to, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int r = (int)Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
        int g = (int)Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
        int b = (int)Mth.lerp(t, from & 0xFF, to & 0xFF);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
