package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import net.minecraft.client.Minecraft;

public final class PlayerInfoEnabler {
    private static final int WARMUP_TICKS = 40;
    private static int warmup = 40;
    private static boolean requested;
    private static int lastXpLevel = -1;

    private PlayerInfoEnabler() {
    }

    public static int triggerToSend(boolean hudOn, boolean showMana, boolean showXp, boolean hasSnapshot, boolean leveledUp) {
        if (hudOn && (showMana || showXp)) {
            return hasSnapshot && !leveledUp ? -1 : 2314;
        } else {
            return -1;
        }
    }

    public static int triggerToSend(boolean hudOn, boolean showMana, boolean showXp, boolean hasSnapshot, boolean leveledUp, boolean mapReady) {
        return !mapReady ? -1 : triggerToSend(hudOn, showMana, showXp, hasSnapshot, leveledUp);
    }

    public static boolean leveledUp(int previousXpLevel, int xpLevel) {
        return previousXpLevel > 0 && xpLevel == 0;
    }

    public static void tick(Minecraft client) {
        if (client.player != null && client.level != null) {
            if (warmup > 0) {
                warmup--;
                lastXpLevel = client.player.experienceLevel;
            } else {
                boolean hudOn = KeybindManager.showCustomHUD;
                HUDConfig config = HUDConfig.get();
                int xp = client.player.experienceLevel;
                boolean up = leveledUp(lastXpLevel, xp);
                lastXpLevel = xp;
                if (up) {
                    requested = false;
                }

                int trigger = triggerToSend(hudOn, config.showMana, config.showXP, PlayerInfoModel.hasData(), up, MapReady.isReady());
                if (trigger >= 0) {
                    if (!requested || up) {
                        requested = true;
                        PlayerInfoModel.request();
                    }
                } else {
                    if (!hudOn || !config.showMana && !config.showXP) {
                        requested = false;
                    }
                }
            }
        }
    }

    public static void reset() {
        warmup = 40;
        requested = false;
        lastXpLevel = -1;
    }
}
