package fr.poubone.att2.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

final class ManaGlobe {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("att2", "dynamic/dahal_globe");
    private static final int TRANSPARENT = ARGB.color(0, 0, 0, 0);
    private static ManaGlobe.GlobeTexture texture;
    private static int texSize;
    private static double lastBakeTime = Double.NEGATIVE_INFINITY;
    private static float lastRatio = -1.0F;
    private static final int[] ROW_MIN = new int[256];
    private static final int[] ROW_MAX = new int[256];

    private ManaGlobe() {
    }

    static void reset() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            Runnable close = () -> {
                if (texture != null) {
                    client.getTextureManager().release(TEXTURE_ID);
                    texture.close();
                    texture = null;
                }

                texSize = 0;
                lastBakeTime = Double.NEGATIVE_INFINITY;
                lastRatio = -1.0F;
            };
            if (!RenderSystem.isOnRenderThread()) {
                client.execute(close);
            } else {
                close.run();
            }
        }
    }

    static Identifier bake(float ratio, float danger, double time, int size, float holeCx, float holeCy, float holeR) {
        if (texture != null && texSize == size && ratio == lastRatio && time >= lastBakeTime && time - lastBakeTime < 0.03333333333333333) {
            return TEXTURE_ID;
        }

        NativeImage image = ensure(size);
        lastBakeTime = time;
        lastRatio = ratio;
        image.fillRect(0, 0, size, size, TRANSPARENT);
        Arrays.fill(ROW_MIN, 0, size, size);
        Arrays.fill(ROW_MAX, 0, size, -1);
        shade(image, size, ratio, danger, (float)time, holeCx, holeCy, holeR);
        texture.upload();
        return TEXTURE_ID;
    }

    static void blitLiquid(GuiGraphics gui, int destX, int destY, int destSize) {
        if (texture != null && texSize > 0) {
            float scale = (float)destSize / texSize;

            for (int row = 0; row < texSize; row++) {
                int x0 = ROW_MIN[row];
                int x1 = ROW_MAX[row];
                if (x1 >= x0) {
                    int srcW = x1 - x0 + 1;
                    int dx0 = destX + Math.round(x0 * scale);
                    int dy0 = destY + Math.round(row * scale);
                    int dx1 = destX + Math.round((x0 + srcW) * scale);
                    int dy1 = destY + Math.round((row + 1) * scale);
                    int dw = Math.max(1, dx1 - dx0);
                    int dh = Math.max(1, dy1 - dy0);
                    gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, dx0, dy0, x0, row, dw, dh, srcW, 1, texSize, texSize);
                }
            }
        }
    }

    static int resolution() {
        return texSize;
    }

    private static NativeImage ensure(int size) {
        Minecraft client = Minecraft.getInstance();
        if (texture != null && texSize == size) {
            return texture.pixels();
        }

        if (texture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            texture.close();
        }

        NativeImage image = new NativeImage(size, size, true);
        texture = new ManaGlobe.GlobeTexture(image);
        texSize = size;
        client.getTextureManager().register(TEXTURE_ID, texture);
        return texture.pixels();
    }

    private static void shade(NativeImage image, int size, float ratio, float danger, float time, float cx, float cy, float r) {
        float deepR = 3.0F;
        float deepG = 10.0F;
        float deepB = 48.0F;
        float midR = 12.0F;
        float midG = 62.0F;
        float midB = 190.0F;
        float hotR = 35.0F;
        float hotG = 133.0F;
        float hotB = 248.0F;
        float coreR = 116.0F;
        float coreG = 204.0F;
        float coreB = 255.0F;
        float fill = 1.0F - 2.0F * ratio;
        float rot = time * 0.14F;
        float cs = Mth.cos(rot);
        float sn = Mth.sin(rot);

        for (int y = 0; y < size; y++) {
            float v = (y + 0.5F - cy) / r;

            for (int x = 0; x < size; x++) {
                float u = (x + 0.5F - cx) / r;
                float d2 = u * u + v * v;
                if (!(d2 >= 1.0F)) {
                    float radial = (float)Math.sqrt(d2);
                    float hemi = (float)Math.sqrt(Math.max(0.0F, 1.0F - d2));
                    float tile = lerp(hemi, 1.0F, 2.0F);
                    float lu = u * tile;
                    float lv = v * tile;
                    float ru = lu * cs - lv * sn;
                    float rv = lu * sn + lv * cs;
                    float nR = clouds2(ru * 1.85F + time * 0.07F, rv * 1.85F + time * 0.035F, 11);
                    float nG = clouds2(ru * 2.7F - time * 0.055F, rv * 2.7F + time * 0.09F, 29);
                    float nB = clouds2(ru * 3.85F + time * 0.11F, rv * 3.85F - time * 0.06F, 47);
                    float wave1 = fill + 0.022F * Mth.sin(u * 6.1F + time * 1.85F) + 0.009F * Mth.sin(u * 13.4F + time * 2.45F);
                    float wave2 = fill + 0.018F * Mth.sin(-u * 7.6F + time * 2.18F) + 0.007F * Mth.sin(-u * 15.2F + time * 1.62F);
                    float mask1 = smooth(v - wave1, -0.016F, 0.016F);
                    float mask2 = smooth(v - wave2, -0.016F, 0.016F);
                    float backWave = Mth.clamp(mask1 - mask2, 0.0F, 1.0F);
                    float liquid = ratio <= 0.0F ? 0.0F : (ratio >= 1.0F ? 1.0F : mask1);
                    float density = nR * 0.42F + nG * 0.35F + nB * 0.23F;
                    density = Mth.clamp(density * 1.28F, 0.0F, 1.0F);
                    float wisps = smooth(nG, 0.46F, 0.9F);
                    float cores = (float)Math.pow(smooth(nB, 0.64F, 0.96F) * wisps, 1.4);
                    float bgShade = 0.62F + 0.38F * radial;
                    float br = lerp(backWave * 0.55F, deepR * bgShade, midR);
                    float bg = lerp(backWave * 0.55F, deepG * bgShade, midG);
                    float bb = lerp(backWave * 0.55F, deepB * bgShade, midB);
                    float cr = lerp(density, deepR, midR);
                    float cg = lerp(density, deepG, midG);
                    float cb = lerp(density, deepB, midB);
                    cr = lerp(wisps * 0.72F, cr, hotR);
                    cg = lerp(wisps * 0.72F, cg, hotG);
                    cb = lerp(wisps * 0.72F, cb, hotB);
                    cr = lerp(cores * 0.55F, cr, coreR);
                    cg = lerp(cores * 0.55F, cg, coreG);
                    cb = lerp(cores * 0.55F, cb, coreB);
                    cr = lerp(liquid, br, cr);
                    cg = lerp(liquid, bg, cg);
                    cb = lerp(liquid, bb, cb);
                    float axis = (u + v) * 0.70710677F;
                    float volume = smooth(axis, -0.9F, 1.0F) * hemi;
                    float shadeMul = lerp(volume, 1.08F, 0.7F);
                    cr *= shadeMul;
                    cg *= shadeMul;
                    cb *= shadeMul;
                    float dist = v - wave1;
                    if (dist > -0.02F && dist < 0.11F) {
                        float s = 1.0F - Mth.clamp(dist / 0.11F, 0.0F, 1.0F);
                        cr = lerp(s * 0.32F, cr, hotR);
                        cg = lerp(s * 0.32F, cg, hotG);
                        cb = lerp(s * 0.32F, cb, hotB);
                    }

                    float rimShade = 0.3F + 0.7F * hemi;
                    float belly = (float)Math.exp(-((u - 0.1F) * (u - 0.1F) * 4.0F + (v - 0.45F) * (v - 0.45F) * 7.0F));
                    cr = cr * rimShade + belly * 7.0F;
                    cg = cg * rimShade + belly * 25.0F;
                    cb = cb * rimShade + belly * 34.0F;
                    float meniscus = ratio > 0.0F && ratio < 1.0F ? (float)Math.exp(-Math.pow((v - wave1) / 0.014F, 2.0)) * hemi : 0.0F;
                    cr = lerp(meniscus * 0.65F, cr, 120.0F);
                    cg = lerp(meniscus * 0.65F, cg, 215.0F);
                    cb = lerp(meniscus * 0.65F, cb, 255.0F);
                    cr = lerp(liquid, 5.0F + hemi * 5.0F, cr);
                    cg = lerp(liquid, 8.0F + hemi * 8.0F, cg);
                    cb = lerp(liquid, 17.0F + hemi * 13.0F, cb);
                    float reflection = (float)Math.exp(-((u + 0.36F) * (u + 0.36F) / 0.018F + (v + 0.48F) * (v + 0.48F) / 0.04F));
                    float glassRim = smooth(radial, 0.84F, 0.98F) * (1.0F - smooth(radial, 0.985F, 1.0F)) * Mth.clamp((-u - v) * 0.5F, 0.0F, 1.0F);
                    float glass = Mth.clamp(reflection * 0.72F + glassRim * 0.36F, 0.0F, 0.8F);
                    cr = lerp(glass, cr, 198.0F);
                    cg = lerp(glass, cg, 225.0F);
                    cb = lerp(glass, cb, 255.0F);
                    float edge = smooth(1.0F - radial, 0.0F, 0.01F);
                    int alpha = Mth.clamp((int)(edge * 255.0F), 0, 255);
                    if (alpha > 0) {
                        image.setPixel(x, y, ARGB.color(alpha, Mth.clamp((int)cr, 0, 255), Mth.clamp((int)cg, 0, 255), Mth.clamp((int)cb, 0, 255)));
                        if (x < ROW_MIN[y]) {
                            ROW_MIN[y] = x;
                        }

                        if (x > ROW_MAX[y]) {
                            ROW_MAX[y] = x;
                        }
                    }
                }
            }
        }
    }

    private static float clouds2(float x, float y, int seed) {
        float n = 0.5F * valueNoise(x + seed * 0.37F, y + seed * 0.19F)
            + 0.28F * valueNoise(x * 2.03F + seed * 1.17F, y * 2.03F)
            + 0.15F * valueNoise(x * 4.07F + seed * 2.41F, y * 4.07F)
            + 0.07F * valueNoise(x * 8.13F + seed * 0.63F, y * 8.13F);
        return Mth.clamp(n, 0.0F, 1.0F);
    }

    private static float valueNoise(float x, float y) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        float fx = x - x0;
        float fy = y - y0;
        fx = fx * fx * (3.0F - 2.0F * fx);
        fy = fy * fy * (3.0F - 2.0F * fy);
        float a = hash(x0, y0);
        float b = hash(x0 + 1, y0);
        float c = hash(x0, y0 + 1);
        float d = hash(x0 + 1, y0 + 1);
        return lerp(fy, lerp(fx, a, b), lerp(fx, c, d));
    }

    private static float hash(int x, int y) {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ n >> 13) * 1274126177;
        return ((n ^ n >> 16) & 2147483647) * 4.656613E-10F;
    }

    private static float smooth(float v, float a, float b) {
        float t = Mth.clamp((v - a) / (b - a), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    private static final class GlobeTexture extends AbstractTexture {
        private NativeImage pixels;

        private GlobeTexture(NativeImage image) {
            this.pixels = image;
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture(() -> "att2_dahal_globe", 5, TextureFormat.RGBA8, image.getWidth(), image.getHeight(), 1, 1);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
            this.upload();
        }

        private NativeImage pixels() {
            return this.pixels;
        }

        private void upload() {
            if (this.pixels != null && this.texture != null) {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.pixels);
            }
        }

        @Override
        public void close() {
            if (this.pixels != null) {
                this.pixels.close();
                this.pixels = null;
            }

            super.close();
        }
    }
}
