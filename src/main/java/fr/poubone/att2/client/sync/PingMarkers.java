package fr.poubone.att2.client.sync;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Matrix4f;

public final class PingMarkers {
    public static final long TTL_MS = 7000L;
    private static final Identifier ARROW = Identifier.fromNamespaceAndPath("att2", "textures/ping/arrow.png");
    private static final SoundEvent SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("att2", "ui.ping"));
    private static final List<PingMarkers.Ping> PINGS = new ArrayList<>();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static boolean matricesReady;
    private static Vec3 cameraPos = Vec3.ZERO;
    private static float partialTick;

    private PingMarkers() {
    }

    public static void captureMatrices(Camera camera, Matrix4f view, Matrix4f projection, float tickDelta) {
        VIEW.set(view);
        PROJECTION.set(projection);
        cameraPos = camera.position();
        partialTick = tickDelta;
        matricesReady = true;
    }

    public static void add(UUID authorId, String playerName, Vec3 pos, String dimension, UUID entityId, ItemStack item, long sequence, boolean playSound) {
        if (Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z)) {
            PINGS.removeIf(ping -> ping.authorId.equals(authorId) && ping.sequence == sequence);
            if (PINGS.size() >= 64) {
                PINGS.removeFirst();
            }

            PINGS.add(new PingMarkers.Ping(authorId, playerName, pos, dimension, entityId, item.copy(), sequence));
            Minecraft client = Minecraft.getInstance();
            if (playSound && client.player != null && client.level != null && dimension.equals(String.valueOf(client.level.dimension()))) {
                Vec3 delta = pos.subtract(client.player.position());
                Vec3 soundPos = client.player.position().add(delta.normalize().scale(Math.min(delta.length(), 64.0) / 64.0 * 14.0));
                client.getSoundManager()
                    .play(new SimpleSoundInstance(SOUND, SoundSource.MASTER, 1.0F, 1.0F, RandomSource.create(), soundPos.x, soundPos.y, soundPos.z));
            }
        }
    }

    public static void clear() {
        PINGS.clear();
        matricesReady = false;
    }

    public static void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        PINGS.removeIf(ping -> now - ping.at > 7000L);
    }

    public static boolean dismissLookedAt() {
        Minecraft client = Minecraft.getInstance();
        if (matricesReady && client.level != null) {
            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            PingMarkers.Ping closest = null;
            double nearest = 100.0;

            for (PingMarkers.Ping ping : PINGS) {
                if (ping.dimension.equals(String.valueOf(client.level.dimension())) && System.currentTimeMillis() - ping.at >= 1000L) {
                    PingGeometry.ScreenPos screen = screen(ping.pos, width, height);
                    double d = Math.pow(screen.x() - width / 2.0F, 2.0) + Math.pow(screen.y() - height / 2.0F, 2.0);
                    if (!screen.behind() && d < nearest) {
                        closest = ping;
                        nearest = d;
                    }
                }
            }

            return closest != null && PINGS.remove(closest);
        } else {
            return false;
        }
    }

    public static void renderHud(GuiGraphics graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null && !client.options.hideGui && matricesReady) {
            String dimension = String.valueOf(client.level.dimension());
            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();

            for (PingMarkers.Ping ping : PINGS) {
                if (dimension.equals(ping.dimension) && ping.entityId != null) {
                    for (Entity entity : client.level.entitiesForRendering()) {
                        if (entity.getUUID().equals(ping.entityId)) {
                            ping.pos = entity.getPosition(partialTick).add(0.0, entity.getBbHeight(), 0.0);
                            if (entity instanceof ItemEntity dropped) {
                                ping.item = dropped.getItem().copy();
                            }
                            break;
                        }
                    }
                }
            }

            PINGS.sort(Comparator.<PingMarkers.Ping>comparingDouble(pingx -> cameraPos.distanceToSqr(pingx.pos)).reversed());

            for (PingMarkers.Ping ping : PINGS) {
                if (dimension.equals(ping.dimension) && System.currentTimeMillis() - ping.at <= 7000L) {
                    double distance = cameraPos.distanceTo(ping.pos);
                    float scale = PingGeometry.scale(distance);
                    PingGeometry.ScreenPos screen = screen(ping.pos, width, height);
                    PlayerInfo author = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(ping.authorId);
                    int color = -1;
                    if (author != null && author.getTeam() != null && author.getTeam().getColor().getColor() != null) {
                        color = 0xFF000000 | author.getTeam().getColor().getColor();
                    }

                    if (!screen.onScreen(width, height)) {
                        PingGeometry.Edge edge = PingGeometry.edge(screen, width, height);
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(edge.x(), edge.y());
                        graphics.pose().pushMatrix();
                        graphics.pose().scale(scale, scale);
                        graphics.pose().translate((float)(-Math.cos(edge.angle())) * 12.0F, (float)(-Math.sin(edge.angle())) * 12.0F);
                        icon(graphics, ping.item, color);
                        graphics.pose().popMatrix();
                        graphics.pose().rotate(edge.angle());
                        graphics.pose().scale(0.25F, 0.25F);
                        graphics.pose().translate(-5.0F, 0.0F);
                        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, -5, -5, 0.0F, 0.0F, 10, 10, 10, 10, color);
                        graphics.pose().popMatrix();
                    } else {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(screen.x(), screen.y());
                        graphics.pose().scale(scale, scale);
                        label(graphics, Component.literal(String.format(Locale.ROOT, "%,.1f m", distance)), -1.5F, null, color);
                        icon(graphics, ping.item, color);
                        if (client.options.keyPlayerList.isDown()) {
                            Component name = author == null
                                ? Component.literal(ping.playerName)
                                : PlayerTeam.formatNameForTeam(author.getTeam(), Component.literal(author.getProfile().name()));
                            label(graphics, name, 1.75F, author, -1);
                        }

                        graphics.pose().popMatrix();
                    }
                }
            }
        }
    }

    private static PingGeometry.ScreenPos screen(Vec3 pos, int width, int height) {
        Vec3 rel = pos.subtract(cameraPos);
        return PingGeometry.project((float)rel.x, (float)rel.y, (float)rel.z, VIEW, PROJECTION, width, height);
    }

    private static void icon(GuiGraphics graphics, ItemStack item, int color) {
        if (!item.isEmpty()) {
            graphics.renderItem(item, -8, -8);
        } else {
            graphics.pose().pushMatrix();
            graphics.pose().rotate((float) (Math.PI / 4));
            graphics.pose().translate(-2.5F, -2.5F);
            graphics.fill(0, 0, 5, 5, color);
            graphics.pose().popMatrix();
        }
    }

    private static void label(GuiGraphics graphics, Component text, float yOffset, PlayerInfo author, int color) {
        Font font = Minecraft.getInstance().font;
        int extra = author == null ? 0 : 10;
        int width = font.width(text) + extra;
        graphics.pose().pushMatrix();
        graphics.pose().translate(-width / 2.0F, 9.0F * (yOffset - 0.5F));
        graphics.fill(-2, -2, width + 1, 9, 1073741824);
        graphics.drawString(font, text, extra, 0, color, false);
        if (author != null) {
            Identifier skin = author.getSkin().body().texturePath();
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(RenderPipelines.GUI_TEXTURED, skin, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
        }

        graphics.pose().popMatrix();
    }

    private static final class Ping {
        final UUID authorId;
        final UUID entityId;
        final String playerName;
        final String dimension;
        final long sequence;
        final long at = System.currentTimeMillis();
        Vec3 pos;
        ItemStack item;

        Ping(UUID authorId, String playerName, Vec3 pos, String dimension, UUID entityId, ItemStack item, long sequence) {
            this.authorId = authorId;
            this.playerName = playerName;
            this.pos = pos;
            this.dimension = dimension;
            this.entityId = entityId;
            this.item = item;
            this.sequence = sequence;
        }
    }
}
