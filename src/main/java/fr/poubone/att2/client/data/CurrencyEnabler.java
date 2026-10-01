package fr.poubone.att2.client.data;

import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

public final class CurrencyEnabler {
    private static final int PICKUP_DELAY_TICKS = 3;
    private static final int TRANSACTION_DELAY_TICKS = 3;
    private static final int MIN_REFRESH_GAP_TICKS = 20;
    private static final int RETRY_TICKS = 100;
    private static int pendingTicks = -1;
    private static int retryTicks = -1;
    private static int cooldownTicks;
    private static boolean transactionPending;
    private static Integer lastLiveScore;

    private CurrencyEnabler() {
    }

    public static void onPickup() {
        schedule(3);
    }

    public static void onCurrencySound(Identifier sound, SoundSource source, float pitch, double x, double y, double z) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            onCurrencySound(sound, source, pitch, client.player.distanceToSqr(x, y, z));
        }
    }

    static void onCurrencySound(Identifier sound, SoundSource source, float pitch, double distanceSquared) {
        if (sound != null && "minecraft".equals(sound.getNamespace()) && !(distanceSquared > 9.0)) {
            if (source == SoundSource.BLOCKS && "piece2".equals(sound.getPath())) {
                onPickup();
            } else if (source == SoundSource.AMBIENT && "piece1".equals(sound.getPath()) && Math.abs(pitch - 0.7F) < 0.001F) {
                transactionPending = true;
                schedule(0);
            }
        }
    }

    public static void onTransaction() {
        transactionPending = true;
        schedule(3);
    }

    private static void schedule(int delay) {
        if (pendingTicks < 0 || delay < pendingTicks) {
            pendingTicks = delay;
        }
    }

    public static void tick(Minecraft client) {
        if (client.player != null && client.level != null && !Att2Triggers.isGameplayPaused(client) && MapReady.isReady()) {
            OptionalInt live = ScoreCache.readLive("CHRONOTON");
            if (live.isPresent()) {
                int value = live.getAsInt();
                if (lastLiveScore != null && value != lastLiveScore) {
                    schedule(3);
                }

                lastLiveScore = value;
            } else {
                lastLiveScore = null;
            }

            tickRefresh();
        }
    }

    static void tickRefresh() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (CurrencyModel.isListening() && retryTicks < 0) {
            retryTicks = 100;
        }

        if (!CurrencyModel.hasData() && pendingTicks < 0 && retryTicks < 0) {
            schedule(0);
        }

        if (pendingTicks > 0) {
            pendingTicks--;
        }

        if (pendingTicks == 0 && (cooldownTicks == 0 || transactionPending) && !CurrencyModel.isListening()) {
            CurrencyModel.request();
            pendingTicks = -1;
            transactionPending = false;
            cooldownTicks = 20;
            retryTicks = 100;
        }

        if (retryTicks >= 0) {
            if (!CurrencyModel.isListening()) {
                retryTicks = -1;
            } else if (--retryTicks == 0) {
                CurrencyModel.close();
                CurrencyModel.request();
                retryTicks = 100;
                cooldownTicks = 20;
            }
        }
    }

    public static void reset() {
        pendingTicks = -1;
        retryTicks = -1;
        cooldownTicks = 0;
        transactionPending = false;
        lastLiveScore = null;
    }
}
