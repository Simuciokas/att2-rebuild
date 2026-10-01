package fr.poubone.att2.client.hud;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class HudLayout {
    public static final String XP = "xp";
    public static final String CHRONOTON = "chronoton";
    public static final String MANA = "mana";
    public static final String STATS = "stats";
    public static final String ARROWS = "arrows";
    public static final String ARMOR = "armor";
    public static final String SPELLS = "spells";
    public static final String[] IDS = new String[]{"xp", "chronoton", "mana", "stats", "arrows", "armor", "spells"};
    public static final float SCALE_MIN = 0.4F;
    public static final float SCALE_MAX = 3.0F;
    public static final float SCALE_STEP = 0.1F;
    public static final float GAP_MIN = 0.3F;
    public static final float GAP_MAX = 2.0F;
    public static final float GAP_STEP = 0.1F;
    private static final Map<String, HudSlot> DEFAULTS = new LinkedHashMap<>();

    private HudLayout() {
    }

    public static HudSlot slot(String id) {
        HUDConfig config = HUDConfig.get();
        if (config.slots == null) {
            config.slots = new LinkedHashMap<>();
        }

        return config.slots.computeIfAbsent(id, key -> defaultSlot(key).copy());
    }

    public static HudSlot defaultSlot(String id) {
        HudSlot fallback = DEFAULTS.get(id);
        return fallback == null ? new HudSlot(0.4F, 0.4F, 0.15F, 0.08F) : fallback;
    }

    public static void resetAll() {
        HUDConfig config = HUDConfig.get();
        if (config.slots == null) {
            config.slots = new LinkedHashMap<>();
        }

        config.slots.clear();

        for (String id : IDS) {
            config.slots.put(id, defaultSlot(id).copy());
        }
    }

    public static HudLayout.Box box(String id) {
        Minecraft client = Minecraft.getInstance();
        int sw = Math.max(1, client.getWindow().getGuiScaledWidth());
        int sh = Math.max(1, client.getWindow().getGuiScaledHeight());
        return box(id, sw, sh);
    }

    public static HudLayout.Box box(String id, int screenW, int screenH) {
        return boxFromSlot(slot(id), screenW, screenH);
    }

    public static HudLayout.Box boxFromSlot(HudSlot slot, int screenW, int screenH) {
        int w = Mth.clamp(Math.round(slot.w * screenW), 24, screenW);
        int h = Mth.clamp(Math.round(slot.h * screenH), 14, screenH);
        int x = Mth.clamp(Math.round(slot.x * screenW), 0, Math.max(0, screenW - w));
        int y = Mth.clamp(Math.round(slot.y * screenH), 0, Math.max(0, screenH - h));
        if (x + w > screenW) {
            x = Math.max(0, screenW - w);
        }

        if (y + h > screenH) {
            y = Math.max(0, screenH - h);
        }

        return new HudLayout.Box(x, y, w, h);
    }

    public static int snapToEdge(int value, int min, int max, int snap) {
        int clamped = Mth.clamp(value, min, max);
        if (clamped - min <= snap) {
            return min;
        } else if (max - clamped <= snap) {
            return max;
        } else {
            return snap <= 0 ? clamped : Mth.clamp(clamped / snap * snap, min, max);
        }
    }

    public static float fractionX(int x, int w, int sw) {
        return edgeFraction(x, w, sw);
    }

    public static float fractionY(int y, int h, int sh) {
        return edgeFraction(y, h, sh);
    }

    private static float edgeFraction(int pos, int size, int screen) {
        if (screen <= 0) {
            return 0.0F;
        }

        if (pos <= 0) {
            return 0.0F;
        }

        int maxPos = Math.max(0, screen - Math.max(0, size));
        return maxPos > 0 && pos >= maxPos ? 1.0F : Mth.clamp((float)pos / screen, 0.0F, 1.0F);
    }

    public static float contentScale(String id, int intrinsicW, int intrinsicH) {
        float fit = box(id).fit(intrinsicW, intrinsicH);
        float user = slot(id).scale;
        if (user <= 0.0F) {
            user = 1.0F;
        }

        user = Mth.clamp(user, 0.4F, 3.0F);
        return fit * user;
    }

    public static void adjustScale(String id, float delta) {
        HudSlot s = slot(id);
        float next = (s.scale <= 0.0F ? 1.0F : s.scale) + delta;
        s.scale = Mth.clamp(next, 0.4F, 3.0F);
        s.scale = Math.round(s.scale * 10.0F) / 10.0F;
    }

    public static void adjustGap(String id, float delta) {
        HudSlot s = slot(id);
        float next = (s.gap <= 0.0F ? 1.0F : s.gap) + delta;
        s.gap = Mth.clamp(next, 0.3F, 2.0F);
        s.gap = Math.round(s.gap * 10.0F) / 10.0F;
    }

    public static void writePixels(String id, int x, int y, int w, int h) {
        Minecraft client = Minecraft.getInstance();
        int sw = Math.max(1, client.getWindow().getGuiScaledWidth());
        int sh = Math.max(1, client.getWindow().getGuiScaledHeight());
        HudSlot slot = slot(id);
        slot.x = fractionX(x, w, sw);
        slot.y = fractionY(y, h, sh);
        slot.w = Mth.clamp((float)w / sw, 0.03F, 1.0F);
        slot.h = Mth.clamp((float)h / sh, 0.025F, 1.0F);
    }

    public static String labelKey(String id) {
        return "screen.hud_config.slot." + id;
    }

    static {
        DEFAULTS.put("xp", new HudSlot(0.88056F, 0.00438F, 0.11944F, 0.03501F, 1.9F));
        DEFAULTS.put("chronoton", new HudSlot(0.93677F, 0.05689F, 0.06323F, 0.03939F, 2.6F));
        DEFAULTS.put("mana", new HudSlot(0.88525F, 0.7593F, 0.07494F, 0.15317F, 1.0F));
        DEFAULTS.put("stats", new HudSlot(0.01F, 0.86F, 0.18F, 0.11F, 1.1F));
        DEFAULTS.put("arrows", new HudSlot(0.28337F, 0.9453F, 0.09016F, 0.0547F, 0.8F));
        DEFAULTS.put("armor", new HudSlot(0.62295F, 0.92779F, 0.14052F, 0.06127F, 1.3F));
        DEFAULTS.put("spells", new HudSlot(0.0F, 0.24508F, 0.03747F, 0.0919F, 0.4F));
    }

    public record Box(int x, int y, int w, int h) {
        public float fit(int intrinsicW, int intrinsicH) {
            return intrinsicW > 0 && intrinsicH > 0 ? Mth.clamp(Math.min((float)this.w / intrinsicW, (float)this.h / intrinsicH), 0.4F, 3.0F) : 1.0F;
        }
    }
}
