package fr.poubone.att2.client.teleport;

import java.lang.reflect.Field;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

public final class TeleportStepEffectRenderer {
    private static final double DEFAULT_FOV_DEGREES = 70.0;
    private static final double FALLBACK_MASK_CAMERA_HEIGHT = 150.0;
    private static final double CHUNK_SCREEN_MARGIN = 32.0;
    private static final int START_COLOR_CAPTURE_TICK = 30;
    private static Field fogRendererField;
    private static int[] capturedStartMaskColor;

    private TeleportStepEffectRenderer() {
    }

    public static void render(GuiGraphics context, DeltaTracker deltaTracker) {
        updateStartMaskColorCapture(deltaTracker);
        float tickProgress = deltaTracker.getGameTimeDeltaPartialTick(false);
        boolean shaderScreenMaskOnly = TeleportTransitionController.shouldUseShaderScreenMaskOnly();
        float maskIntensity = TeleportTransitionController.getShaderScreenMaskIntensity(tickProgress);
        if (maskIntensity > 0.0F) {
            renderChunkScreenMask(context, deltaTracker, tickProgress, maskIntensity);
        }

        float intensity;
        if (shaderScreenMaskOnly) {
            intensity = TeleportTransitionController.getCameraMotionStepEffectIntensity(tickProgress);
        } else {
            intensity = TeleportTransitionController.getStepEffectIntensity(tickProgress);
            intensity = Math.max(intensity, TeleportTransitionController.getHudFadeOverlayIntensity(tickProgress));
        }

        renderStepFlash(context, intensity);
    }

    private static void renderStepFlash(GuiGraphics context, float intensity) {
        if (!(intensity <= 0.0F)) {
            int flashAlpha = (int)(68.0F * intensity);
            context.fill(0, 0, context.guiWidth(), context.guiHeight(), argb(flashAlpha, 245, 245, 235));
        }
    }

    private static void updateStartMaskColorCapture(DeltaTracker deltaTracker) {
        if (!TeleportTransitionController.isRunning()) {
            capturedStartMaskColor = null;
        } else if (capturedStartMaskColor == null
            && TeleportTransitionController.shouldUseShaderScreenMaskOnly()
            && TeleportTransitionController.getTicks() >= 30) {
            capturedStartMaskColor = getSkyMaskColor(deltaTracker);
        }
    }

    private static int[] getSkyMaskColor(DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.gameRenderer != null) {
            try {
                if (getFogRendererField().get(client.gameRenderer) instanceof FogRenderer fogRenderer) {
                    Vector4f fogColor = fogRenderer.setupFog(client.gameRenderer.getMainCamera(), getFogViewDistance(client), deltaTracker, 0.0F, client.level);
                    return softenMaskColor(fogColor);
                }
            } catch (ReflectiveOperationException | LinkageError var5) {
            }
        }

        return new int[]{188, 197, 202};
    }

    private static void renderChunkScreenMask(GuiGraphics context, DeltaTracker deltaTracker, float tickProgress, float maskIntensity) {
        TeleportTransitionController.CameraFrame frame = TeleportTransitionController.getCameraFrame(tickProgress);
        if (frame != null) {
            double cameraY = 150.0;
            double worldHalfHeight = Math.tan(Math.toRadians(70.0) * 0.5) * cameraY;
            double worldHalfWidth = worldHalfHeight * context.guiWidth() / Math.max(1.0, context.guiHeight());
            double scaleX = context.guiWidth() / (worldHalfWidth * 2.0);
            double scaleY = context.guiHeight() / (worldHalfHeight * 2.0);
            double yawRadians = Math.toRadians(frame.yaw());
            double rightX = Math.cos(yawRadians);
            double rightZ = Math.sin(yawRadians);
            double forwardX = -Math.sin(yawRadians);
            double forwardZ = Math.cos(yawRadians);
            double minWorldX = Double.POSITIVE_INFINITY;
            double maxWorldX = Double.NEGATIVE_INFINITY;
            double minWorldZ = Double.POSITIVE_INFINITY;
            double maxWorldZ = Double.NEGATIVE_INFINITY;
            double[] screenWorldXs = new double[]{-worldHalfWidth - 32.0, worldHalfWidth + 32.0, worldHalfWidth + 32.0, -worldHalfWidth - 32.0};
            double[] screenWorldYs = new double[]{-worldHalfHeight - 32.0, -worldHalfHeight - 32.0, worldHalfHeight + 32.0, worldHalfHeight + 32.0};

            for (int i = 0; i < 4; i++) {
                double worldX = frame.pos().x + rightX * screenWorldXs[i] + forwardX * screenWorldYs[i];
                double worldZ = frame.pos().z + rightZ * screenWorldXs[i] + forwardZ * screenWorldYs[i];
                minWorldX = Math.min(minWorldX, worldX);
                maxWorldX = Math.max(maxWorldX, worldX);
                minWorldZ = Math.min(minWorldZ, worldZ);
                maxWorldZ = Math.max(maxWorldZ, worldZ);
            }

            int minChunkX = Mth.floor(minWorldX / 16.0) - 1;
            int maxChunkX = Mth.floor(maxWorldX / 16.0) + 1;
            int minChunkZ = Mth.floor(minWorldZ / 16.0) - 1;
            int maxChunkZ = Mth.floor(maxWorldZ / 16.0) + 1;
            int[] color = getSceneMaskColor(deltaTracker);
            boolean shaderScreenMaskOnly = TeleportTransitionController.shouldUseShaderScreenMaskOnly();

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    double chunkMinWorldX = chunkX * 16.0;
                    double chunkMinWorldZ = chunkZ * 16.0;
                    float sectionOpacity;
                    if (shaderScreenMaskOnly) {
                        sectionOpacity = TeleportTransitionController.getShaderScreenMaskSectionOpacity(
                            chunkMinWorldX + 8.0, frame.pos().y, chunkMinWorldZ + 8.0
                        );
                    } else {
                        float visibility = TeleportTransitionController.getFallbackTerrainSectionVisibility(
                            chunkMinWorldX + 8.0, frame.pos().y, chunkMinWorldZ + 8.0
                        );
                        sectionOpacity = 1.0F - visibility;
                    }

                    float opacity = sectionOpacity * maskIntensity;
                    if (!(opacity <= 0.001F)) {
                        int alpha = (int)(252.0F * opacity);
                        drawProjectedChunkQuad(
                            context, frame.pos(), chunkMinWorldX, chunkMinWorldZ, rightX, rightZ, forwardX, forwardZ, scaleX, scaleY, alpha, color
                        );
                    }
                }
            }
        }
    }

    private static void drawProjectedChunkQuad(
        GuiGraphics context,
        Vec3 cameraPos,
        double minWorldX,
        double minWorldZ,
        double rightX,
        double rightZ,
        double forwardX,
        double forwardZ,
        double scaleX,
        double scaleY,
        int alpha,
        int[] color
    ) {
        double maxWorldX = minWorldX + 16.0;
        double maxWorldZ = minWorldZ + 16.0;
        double[] worldXs = new double[]{minWorldX, maxWorldX, maxWorldX, minWorldX};
        double[] worldZs = new double[]{minWorldZ, minWorldZ, maxWorldZ, maxWorldZ};
        double[] screenXs = new double[4];
        double[] screenYs = new double[4];

        for (int i = 0; i < 4; i++) {
            double dx = worldXs[i] - cameraPos.x;
            double dz = worldZs[i] - cameraPos.z;
            double screenWorldX = dx * rightX + dz * rightZ;
            double screenWorldY = dx * forwardX + dz * forwardZ;
            screenXs[i] = context.guiWidth() * 0.5 + screenWorldX * scaleX;
            screenYs[i] = context.guiHeight() * 0.5 + screenWorldY * scaleY;
        }

        if (getScreenBounds(screenXs, screenYs, context.guiWidth(), context.guiHeight()) != null) {
            drawProjectedChunkScanlines(context, screenXs, screenYs, alpha, color);
        }
    }

    private static ScreenRectangle getScreenBounds(double[] xs, double[] ys, int screenWidth, int screenHeight) {
        double minScreenX = Double.POSITIVE_INFINITY;
        double maxScreenX = Double.NEGATIVE_INFINITY;
        double minScreenY = Double.POSITIVE_INFINITY;
        double maxScreenY = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < 4; i++) {
            minScreenX = Math.min(minScreenX, xs[i]);
            maxScreenX = Math.max(maxScreenX, xs[i]);
            minScreenY = Math.min(minScreenY, ys[i]);
            maxScreenY = Math.max(maxScreenY, ys[i]);
        }

        int x1 = Math.max(0, Mth.floor(minScreenX));
        int y1 = Math.max(0, Mth.floor(minScreenY));
        int x2 = Math.min(screenWidth, Mth.ceil(maxScreenX));
        int y2 = Math.min(screenHeight, Mth.ceil(maxScreenY));
        return x2 > x1 && y2 > y1 ? new ScreenRectangle(x1, y1, x2 - x1, y2 - y1) : null;
    }

    private static void drawProjectedChunkScanlines(GuiGraphics context, double[] xs, double[] ys, int alpha, int[] color) {
        double minScreenY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        double maxScreenY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        int y1 = Math.max(0, Mth.floor(minScreenY));
        int y2 = Math.min(context.guiHeight(), Mth.ceil(maxScreenY));
        if (y2 > y1) {
            int fillColor = argb(alpha, color[0], color[1], color[2]);

            for (int y = y1; y < y2; y += 2) {
                double scanY = Math.min(y2 - 0.5, y + 1.0);
                double[] intersections = new double[4];
                int count = 0;

                for (int i = 0; i < 4; i++) {
                    int next = i + 1 & 3;
                    double edgeY1 = ys[i];
                    double edgeY2 = ys[next];
                    if (edgeY1 <= scanY && edgeY2 > scanY || edgeY2 <= scanY && edgeY1 > scanY) {
                        double t = (scanY - edgeY1) / (edgeY2 - edgeY1);
                        intersections[count++] = xs[i] + (xs[next] - xs[i]) * t;
                    }
                }

                if (count >= 2) {
                    double left = Math.min(intersections[0], intersections[1]);
                    double right = Math.max(intersections[0], intersections[1]);
                    int x1 = Math.max(0, Mth.floor(left));
                    int x2 = Math.min(context.guiWidth(), Mth.ceil(right));
                    if (x2 > x1) {
                        context.fill(x1, y, x2, Math.min(y + 2, y2), fillColor);
                    }
                }
            }
        }
    }

    private static Field getFogRendererField() throws NoSuchFieldException {
        if (fogRendererField == null) {
            fogRendererField = GameRenderer.class.getDeclaredField("fogRenderer");
            fogRendererField.setAccessible(true);
        }

        return fogRendererField;
    }

    private static int getFogViewDistance(Minecraft client) {
        return Math.max(2, client.options == null ? 12 : client.options.getEffectiveRenderDistance());
    }

    private static int[] getSceneMaskColor(DeltaTracker deltaTracker) {
        return capturedStartMaskColor == null ? getSkyMaskColor(deltaTracker) : capturedStartMaskColor;
    }

    private static int[] softenMaskColor(Vector4f color) {
        float red = Mth.clamp(color.x, 0.0F, 1.0F);
        float green = Mth.clamp(color.y, 0.0F, 1.0F);
        float blue = Mth.clamp(color.z, 0.0F, 1.0F);
        float luma = red * 0.2126F + green * 0.7152F + blue * 0.0722F;
        red = red * 0.62F + luma * 0.38F;
        green = green * 0.62F + luma * 0.38F;
        blue = blue * 0.62F + luma * 0.38F;
        red = red * 0.78F + 0.2F;
        green = green * 0.78F + 0.2F;
        blue = blue * 0.78F + 0.2F;
        return new int[]{(int)(Mth.clamp(red, 0.0F, 1.0F) * 255.0F), (int)(Mth.clamp(green, 0.0F, 1.0F) * 255.0F), (int)(Mth.clamp(blue, 0.0F, 1.0F) * 255.0F)};
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return clamp(alpha) << 24 | clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
