package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class XPDisplay {
    static float iconX;
    static float iconY;
    private static int lastLevel = Integer.MIN_VALUE;
    private static float flash = 0.0F;

    public static void reset() {
        lastLevel = Integer.MIN_VALUE;
        flash = 0.0F;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            OptionalInt requiredXpOpt = ScoreCache.get("LVL_UPGRADE_REQ");
            OptionalInt currentLvlOpt = ScoreCache.get("GAMELEVEL");
            if (!requiredXpOpt.isEmpty() && !currentLvlOpt.isEmpty()) {
                int requiredXp = requiredXpOpt.getAsInt();
                int currentLvl = currentLvlOpt.getAsInt();
                if (currentLvl >= 50) {
                    currentLvl += ScoreCache.getOrDefault("LEVELMASTER", 0);
                }

                int currentXp = client.player.experienceLevel;
                if (lastLevel != Integer.MIN_VALUE && currentLvl > lastLevel) {
                    flash = 1.0F;
                    client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.35F, 0.35F));
                    HudFx.burstToward(iconX, iconY, 18, 5111629, true);
                }

                lastLevel = currentLvl;
                if (flash > 0.02F) {
                    flash *= 0.88F;
                } else {
                    flash = 0.0F;
                }

                String display = ModLanguageManager.get("level.label").getString() + " " + currentLvl + " : " + currentXp + "/" + requiredXp;
                int color = flash > 0.05F
                    ? ARGB.color(255, (int)Mth.lerp(flash, 0.0F, 255.0F), (int)Mth.lerp(flash, 255.0F, 255.0F), (int)Mth.lerp(flash, 0.0F, 255.0F))
                    : -16711936;
                HudLayout.Box box = HudLayout.box("xp");
                float scale = HudLayout.contentScale("xp", 210, 18);
                iconX = box.x();
                iconY = box.y();
                HudDrawUtils.drawXPHUDValue(context, display, box.x(), box.y(), scale, color);
                if (flash > 0.08F) {
                    int a = (int)(flash * 90.0F);
                    context.fill(box.x() - 2, box.y() - 2, box.x() + box.w() + 2, box.y() + box.h() + 2, ARGB.color(a, 180, 255, 120));
                }
            }
        }
    }
}
