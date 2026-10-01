package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.util.ModTextures;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class HudFx {
    private static final List<HudFx.Particle> PARTICLES = new ArrayList<>();
    private static long lastPickupFxMs = 0L;
    private static final ItemStack COIN = new ItemStack(Items.GOLD_NUGGET);

    private HudFx() {
    }

    public static void reset() {
        PARTICLES.clear();
        lastPickupFxMs = 0L;
    }

    public static void tick() {
        Iterator<HudFx.Particle> it = PARTICLES.iterator();

        while (it.hasNext()) {
            HudFx.Particle p = it.next();
            p.age++;
            if (p.age >= p.life) {
                it.remove();
            } else if (p.homing) {
                float t = (float)p.age / p.life;
                float ease = t * t * (3.0F - 2.0F * t);
                p.x = Mth.lerp(ease, p.sx, p.tx);
                p.y = Mth.lerp(ease, p.sy, p.ty);
                p.x = p.x + (float)Math.sin((p.age + p.seed) * 0.35F) * (1.0F - t) * 4.0F;
            } else {
                p.x = p.x + p.vx;
                p.y = p.y + p.vy;
                p.vy = p.vy + p.gravity;
                p.vx *= 0.96F;
                p.vy *= 0.96F;
            }
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        for (HudFx.Particle p : PARTICLES) {
            float t = (float)p.age / p.life;
            int alpha = (int)((1.0F - t) * p.alpha * 255.0F);
            if (alpha > 4) {
                int color = alpha << 24 | p.rgb & 16777215;
                if (p.kind == HudFx.Kind.TEXT) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(p.x, p.y);
                    float scale = 0.9F + (1.0F - t) * 0.25F;
                    graphics.pose().scale(scale, scale);
                    graphics.drawString(font, p.text, 0, 0, color, false);
                    graphics.pose().popMatrix();
                } else if (p.kind == HudFx.Kind.COIN) {
                    float scale = 0.7F + (1.0F - t) * 0.35F;
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(p.x, p.y);
                    graphics.pose().scale(scale, scale);
                    graphics.renderItem(COIN, -8, -8);
                    graphics.pose().popMatrix();
                } else {
                    Identifier texture = switch (p.kind) {
                        case BUBBLE -> ModTextures.FX_BUBBLE;
                        case XP -> ModTextures.FX_XP_MOTE;
                        default -> ModTextures.FX_SPARK;
                    };
                    int size = p.size;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, texture, (int)p.x - size / 2, (int)p.y - size / 2, 0.0F, 0.0F, size, size, size, size, color);
                }
            }
        }
    }

    public static void spark(float x, float y, int rgb) {
        HudFx.Particle p = new HudFx.Particle(HudFx.Kind.SPARK);
        p.x = x;
        p.y = y;
        p.vx = (float)((Math.random() - 0.5) * 2.4);
        p.vy = (float)((Math.random() - 0.5) * 2.4);
        p.life = 12 + (int)(Math.random() * 10.0);
        p.rgb = rgb;
        p.size = 6 + (int)(Math.random() * 5.0);
        PARTICLES.add(p);
    }

    public static void bubble(float x, float y) {
        HudFx.Particle p = new HudFx.Particle(HudFx.Kind.BUBBLE);
        p.x = x;
        p.y = y;
        p.vx = (float)((Math.random() - 0.5) * 0.4);
        p.vy = -0.35F - (float)Math.random() * 0.45F;
        p.life = 28 + (int)(Math.random() * 18.0);
        p.rgb = 12120319;
        p.size = 4 + (int)(Math.random() * 5.0);
        PARTICLES.add(p);
    }

    public static void convergeXp(float fromX, float fromY, float toX, float toY) {
        HudFx.Particle p = new HudFx.Particle(HudFx.Kind.XP);
        p.sx = fromX;
        p.sy = fromY;
        p.x = fromX;
        p.y = fromY;
        p.tx = toX;
        p.ty = toY;
        p.homing = true;
        p.life = 16 + (int)(Math.random() * 10.0);
        p.rgb = 5111629;
        p.size = 6 + (int)(Math.random() * 4.0);
        p.seed = (float)(Math.random() * 40.0);
        PARTICLES.add(p);
    }

    public static void floatingText(float x, float y, String text, int rgb) {
        HudFx.Particle p = new HudFx.Particle(HudFx.Kind.TEXT);
        p.x = x;
        p.y = y;
        p.vy = -0.55F;
        p.life = 28;
        p.rgb = rgb;
        p.text = text;
        p.alpha = 1.0F;
        PARTICLES.add(p);
    }

    public static void flyingCoin(float fromX, float fromY, float toX, float toY) {
        HudFx.Particle p = new HudFx.Particle(HudFx.Kind.COIN);
        p.sx = fromX;
        p.sy = fromY;
        p.x = fromX;
        p.y = fromY;
        p.tx = toX;
        p.ty = toY;
        p.homing = true;
        p.life = 14 + (int)(Math.random() * 8.0);
        p.seed = (float)(Math.random() * 30.0);
        lastPickupFxMs = System.currentTimeMillis();
        PARTICLES.add(p);
    }

    public static boolean recentlySpawnedPickupFx() {
        return System.currentTimeMillis() - lastPickupFxMs < 400L;
    }

    public static void burstToward(float toX, float toY, int count, int rgb, boolean xp) {
        Minecraft client = Minecraft.getInstance();
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        for (int i = 0; i < count; i++) {
            float angle = (float)(Math.random() * Math.PI * 2.0);
            float dist = 28.0F + (float)Math.random() * Math.min(sw, sh) * 0.28F;
            float fx = toX + Mth.cos(angle) * dist;
            float fy = toY + Mth.sin(angle) * dist;
            if (xp) {
                convergeXp(fx, fy, toX, toY);
            } else {
                spark(fx, fy, rgb);
            }
        }
    }

    private enum Kind {
        SPARK,
        BUBBLE,
        XP,
        TEXT,
        COIN;
    }

    private static final class Particle {
        final HudFx.Kind kind;
        float x;
        float y;
        float vx;
        float vy;
        float sx;
        float sy;
        float tx;
        float ty;
        float seed;
        float gravity = 0.0F;
        float alpha = 1.0F;
        int rgb = 16777215;
        int size = 8;
        int age;
        int life = 20;
        boolean homing;
        String text = "";

        Particle(HudFx.Kind kind) {
            this.kind = kind;
        }
    }
}
