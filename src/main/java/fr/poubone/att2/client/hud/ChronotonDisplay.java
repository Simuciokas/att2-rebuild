package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.ScoreCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

public class ChronotonDisplay {
    private static int displayedValue = 0;
    private static int animationTimer = 0;
    private static final int MAX_TICKS = 20;
    public static float iconX = 400.0F;
    public static float iconY = 40.0F;
    private static int lastScore = Integer.MIN_VALUE;

    public static void reset() {
        displayedValue = 0;
        animationTimer = 0;
        lastScore = Integer.MIN_VALUE;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            ScoreCache.get("CHRONOTON").ifPresent(score -> {
                if (lastScore != Integer.MIN_VALUE && score > lastScore && !HudFx.recentlySpawnedPickupFx()) {
                    int extra = Math.min(8, Math.max(2, (score - lastScore) / 4));
                    float fromX = client.getWindow().getGuiScaledWidth() * 0.5F;
                    float fromY = client.getWindow().getGuiScaledHeight() * 0.55F;

                    for (int i = 0; i < extra; i++) {
                        HudFx.flyingCoin(fromX + (float)(Math.random() - 0.5) * 40.0F, fromY + (float)(Math.random() - 0.5) * 20.0F, iconX, iconY);
                    }
                }

                if (displayedValue != score) {
                    if (score > displayedValue && animationTimer == 0) {
                        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.55F, 0.22F));
                    }

                    animationTimer = 20;
                    int diff = score - displayedValue;
                    int step = Math.max(1, Math.abs(diff) / 10);
                    if (Math.abs(diff) < 20) {
                        step = 1;
                    }

                    displayedValue = displayedValue + Integer.signum(diff) * step;
                    if (diff > 0 && displayedValue > score || diff < 0 && displayedValue < score) {
                        displayedValue = score;
                    }
                }

                lastScore = score;
                HudLayout.Box box = HudLayout.box("chronoton");
                float scale = HudLayout.contentScale("chronoton", 120, 20);
                iconX = box.x();
                iconY = box.y();
                HudDrawUtils.drawHUDValue(context, displayedValue, animationTimer, 20, box.x(), box.y(), scale, Items.GOLD_NUGGET, -10934, true);
                if (animationTimer > 0) {
                    animationTimer--;
                }
            });
        }
    }
}
