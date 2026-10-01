package fr.poubone.att2.client.teleport;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;

final class SodiumCompat {
    private static final double FALLBACK_CHUNK_SECTION_FADE_SECONDS = 0.75;
    private static boolean active;

    private SodiumCompat() {
    }

    static void beginTransition(Minecraft client, boolean fallbackTerrainMode) {
        ensureFallbackChunkFadeInTime(client, fallbackTerrainMode);
        if (!active) {
            active = true;
            scheduleTerrainUpdate();
        }
    }

    static void endTransition() {
        if (active) {
            active = false;
            scheduleTerrainUpdate();
        }
    }

    static void ensureFallbackChunkFadeInTime(Minecraft client, boolean fallbackTerrainMode) {
        if (fallbackTerrainMode && client != null && client.options != null) {
            Double seconds = client.options.chunkSectionFadeInTime().get();
            if (seconds == null || seconds < 0.75) {
                client.options.chunkSectionFadeInTime().set(0.75);
            }
        }
    }

    static void scheduleTerrainUpdate() {
        try {
            Class<?> rendererClass = Class.forName("net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer");
            Method instanceNullable = rendererClass.getMethod("instanceNullable");
            Object renderer = instanceNullable.invoke(null);
            if (renderer == null) {
                return;
            }

            rendererClass.getMethod("scheduleTerrainUpdate").invoke(renderer);
        } catch (ReflectiveOperationException | LinkageError var3) {
        }
    }
}
