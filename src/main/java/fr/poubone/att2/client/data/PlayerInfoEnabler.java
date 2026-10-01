package fr.poubone.att2.client.data;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import net.minecraft.client.Minecraft;

public final class PlayerInfoEnabler {
    private static final int WARMUP_TICKS = 40;
    private static final int XP_SETTLE_TICKS = 3;
    private static final int MIN_REFRESH_GAP_TICKS = 10;
    private static final int RETRY_TICKS = 100;
    private static int warmup = 40;
    private static int pendingTicks = -1;
    private static int retryTicks = -1;
    private static int cooldownTicks;
    private static int lastXpLevel = -1;
    private static float lastXpProgress;
    private static boolean wasActive;

    private PlayerInfoEnabler() {
    }

    public static boolean xpChanged(int previousLevel, float previousProgress, int level, float progress) {
        return previousLevel >= 0 && (previousLevel != level || Float.compare(previousProgress, progress) != 0);
    }

    private static void schedule(int delay) {
        if (pendingTicks < 0 || delay < pendingTicks) {
            pendingTicks = delay;
        }
    }

    public static void tick(Minecraft client) {
        if (client.player != null && client.level != null && !Att2Triggers.isGameplayPaused(client)) {
            int xpLevel = client.player.experienceLevel;
            float xpProgress = client.player.experienceProgress;
            if (warmup > 0) {
                warmup--;
                lastXpLevel = xpLevel;
                lastXpProgress = xpProgress;
            } else {
                boolean changed = xpChanged(lastXpLevel, lastXpProgress, xpLevel, xpProgress);
                lastXpLevel = xpLevel;
                lastXpProgress = xpProgress;
                HUDConfig config = HUDConfig.get();
                boolean active = KeybindManager.showCustomHUD && (config.showMana || config.showXP) && MapReady.isReady();
                if (!active) {
                    wasActive = false;
                    pendingTicks = -1;
                } else {
                    if (changed) {
                        schedule(3);
                    } else if (!wasActive || !PlayerInfoModel.hasData() && !PlayerInfoModel.isListening() && pendingTicks < 0 && retryTicks < 0) {
                        schedule(0);
                    }

                    wasActive = true;
                    if (cooldownTicks > 0) {
                        cooldownTicks--;
                    }

                    if (pendingTicks > 0) {
                        pendingTicks--;
                    }

                    if (pendingTicks == 0 && cooldownTicks == 0 && !PlayerInfoModel.isListening()) {
                        PlayerInfoModel.request();
                        pendingTicks = -1;
                        cooldownTicks = 10;
                        retryTicks = 100;
                    }

                    if (retryTicks >= 0) {
                        if (!PlayerInfoModel.isListening()) {
                            retryTicks = -1;
                        } else if (--retryTicks == 0) {
                            PlayerInfoModel.request();
                            pendingTicks = -1;
                            retryTicks = 100;
                            cooldownTicks = 10;
                        }
                    }
                }
            }
        }
    }

    public static void reset() {
        warmup = 40;
        pendingTicks = -1;
        retryTicks = -1;
        cooldownTicks = 0;
        lastXpLevel = -1;
        lastXpProgress = 0.0F;
        wasActive = false;
    }
}
