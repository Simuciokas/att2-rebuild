package com.lootbeams.render;

import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class LootBeamShaderState {
    private static final ThreadLocal<Vector4f> COLOR_MODULATOR = new ThreadLocal<>();

    private LootBeamShaderState() {
    }

    public static void setColorModulator(float red, float green, float blue, float alpha) {
        COLOR_MODULATOR.set(new Vector4f(red, green, blue, alpha));
    }

    public static void clear() {
        COLOR_MODULATOR.remove();
    }

    public static Vector4fc colorModulatorOr(Vector4fc fallback) {
        Vector4f current = COLOR_MODULATOR.get();
        return (Vector4fc)(current != null ? current : fallback);
    }
}
