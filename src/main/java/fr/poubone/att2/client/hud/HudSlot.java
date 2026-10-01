package fr.poubone.att2.client.hud;

public final class HudSlot {
    public float x;
    public float y;
    public float w;
    public float h;
    public float scale = 1.0F;
    public float gap = 1.0F;

    public HudSlot() {
    }

    public HudSlot(float x, float y, float w, float h) {
        this(x, y, w, h, 1.0F);
    }

    public HudSlot(float x, float y, float w, float h, float scale) {
        this(x, y, w, h, scale, 1.0F);
    }

    public HudSlot(float x, float y, float w, float h, float scale, float gap) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.scale = scale;
        this.gap = gap;
    }

    public HudSlot copy() {
        return new HudSlot(this.x, this.y, this.w, this.h, this.scale, this.gap);
    }
}
