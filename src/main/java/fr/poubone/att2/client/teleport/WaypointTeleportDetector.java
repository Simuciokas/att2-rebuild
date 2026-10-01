package fr.poubone.att2.client.teleport;

import fr.poubone.att2.client.data.Att2Triggers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class WaypointTeleportDetector {
    public static final int TP_DELAY_TICKS = 91;
    private static final int COMMAND_CONFIRM_TIMEOUT_TICKS = 40;
    private static final double PAD_RADIUS = 8.0;
    private static final int FIRST_TRIGGER = 576;
    private static final Identifier START_SOUND = Identifier.withDefaultNamespace("noise1");
    public static final List<WaypointTeleportDetector.Pad> PADS = List.of(
        new WaypointTeleportDetector.Pad("asunark", new Vec3(-3661.0, 70.0, -4977.0)),
        new WaypointTeleportDetector.Pad("eolorion", new Vec3(-5248.0, 100.0, -6202.0)),
        new WaypointTeleportDetector.Pad("kortaek", new Vec3(-5533.0, 87.0, -4678.0)),
        new WaypointTeleportDetector.Pad("meleim", new Vec3(-3845.0, 103.0, -5808.0)),
        new WaypointTeleportDetector.Pad("owsastr", new Vec3(-4661.0, 71.0, -4546.0)),
        new WaypointTeleportDetector.Pad("ryliath", new Vec3(-5005.0, 77.0, -5077.0)),
        new WaypointTeleportDetector.Pad("soquai", new Vec3(-4790.0, 100.0, -5686.0))
    );
    private static final Map<Integer, WaypointTeleportDetector.Route> ROUTES = new HashMap<>();
    private static WaypointTeleportDetector.Route pendingRoute;
    private static int pendingTicks;

    private WaypointTeleportDetector() {
    }

    public static void reset() {
        pendingRoute = null;
        pendingTicks = 0;
    }

    public static WaypointTeleportDetector.Route routeForTrigger(int trigger) {
        return ROUTES.get(trigger);
    }

    public static void onOutgoingCommand(String command) {
        if (TeleportAnimationConfig.isEffectEnabled()) {
            int trigger = Att2Triggers.parseTriggerCommand(command);
            WaypointTeleportDetector.Route route = ROUTES.get(trigger);
            if (route != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null && isNear(player.position(), route.from().pos(), 8.0)) {
                    pendingRoute = route;
                    pendingTicks = 40;
                }
            }
        }
    }

    public static void onSoundPacket(Identifier sound, SoundSource source, double x, double y, double z) {
        if (TeleportAnimationConfig.isEffectEnabled() && START_SOUND.equals(sound) && source == SoundSource.BLOCKS) {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;
            if (player != null && client.level != null) {
                if (isNear(player.position(), new Vec3(x, y, z), 3.0)) {
                    if (!TeleportTransitionController.isRunning()) {
                        Vec3 target = null;
                        if (pendingRoute != null) {
                            target = pendingRoute.to().pos();
                        } else if (nearestPad(player.position()) == null) {
                            return;
                        }

                        pendingRoute = null;
                        pendingTicks = 0;
                        TeleportTransitionController.startExternal(client, target, 91);
                    }
                }
            }
        }
    }

    public static void tick(Minecraft client) {
        if (pendingTicks > 0) {
            pendingTicks--;
            if (pendingTicks == 0) {
                pendingRoute = null;
            }
        }
    }

    private static WaypointTeleportDetector.Pad nearestPad(Vec3 pos) {
        for (WaypointTeleportDetector.Pad pad : PADS) {
            if (isNear(pos, pad.pos(), 8.0)) {
                return pad;
            }
        }

        return null;
    }

    private static boolean isNear(Vec3 a, Vec3 b, double radius) {
        return a.distanceToSqr(b) <= radius * radius;
    }

    static {
        int trigger = 576;

        for (WaypointTeleportDetector.Pad from : PADS) {
            for (WaypointTeleportDetector.Pad to : PADS) {
                if (from != to) {
                    ROUTES.put(trigger++, new WaypointTeleportDetector.Route(from, to));
                }
            }
        }
    }

    public record Pad(String name, Vec3 pos) {
    }

    public record Route(WaypointTeleportDetector.Pad from, WaypointTeleportDetector.Pad to) {
    }
}
