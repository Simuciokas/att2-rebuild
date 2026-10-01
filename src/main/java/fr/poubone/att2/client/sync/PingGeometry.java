package fr.poubone.att2.client.sync;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class PingGeometry {
    private PingGeometry() {
    }

    public static PingGeometry.ScreenPos project(float x, float y, float z, Matrix4f view, Matrix4f projection, int width, int height) {
        Vector4f clip = new Vector4f(x, y, z, 1.0F).mul(view).mul(projection);
        float depth = clip.w;
        float divisor = Math.abs(depth) < 1.0E-4F ? (depth < 0.0F ? -1.0E-4F : 1.0E-4F) : depth;
        return new PingGeometry.ScreenPos(width * (0.5F + clip.x / divisor * 0.5F), height * (0.5F - clip.y / divisor * 0.5F), depth <= 0.0F);
    }

    public static float scale(double distance) {
        return (float)Math.max(1.0, 2.0 / Math.pow(Math.max(0.5, distance), 0.3)) * 0.5F;
    }

    public static PingGeometry.Edge edge(PingGeometry.ScreenPos pos, int width, int height) {
        float left = 5.0F;
        float right = Math.max(6, width - 5);
        float top = 5.0F;
        float bottom = Math.max(6, height - 60);
        float cx = (left + right) / 2.0F;
        float cy = (top + bottom) / 2.0F;
        float dx = pos.x - cx;
        float dy = pos.y - cy;
        if (pos.behind) {
            dx = -dx;
            dy = -dy;
        }

        if (Math.abs(dx) + Math.abs(dy) < 0.001F) {
            dy = 1.0F;
        }

        float t = Math.min((right - left) / 2.0F / Math.max(1.0E-4F, Math.abs(dx)), (bottom - top) / 2.0F / Math.max(1.0E-4F, Math.abs(dy)));
        return new PingGeometry.Edge(cx + dx * t, cy + dy * t, (float)Math.atan2(dy, dx));
    }

    public record Edge(float x, float y, float angle) {
    }

    public record ScreenPos(float x, float y, boolean behind) {
        public boolean onScreen(int width, int height) {
            return !this.behind && this.x >= 0.0F && this.x <= width && this.y >= 0.0F && this.y <= height;
        }
    }
}
