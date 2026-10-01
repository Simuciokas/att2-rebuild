package fr.poubone.att2.client.sync;

import com.google.gson.JsonObject;
import fr.poubone.att2.client.util.ChatItemUtils;
import fr.poubone.att2.client.util.ModLanguageManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PartySync {
    private static final long HEARTBEAT_MS = 1000L;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "att2-party-sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final ConcurrentLinkedQueue<PartyEvent> INCOMING = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean HEARTBEAT_IN_FLIGHT = new AtomicBoolean(false);
    private static final AtomicBoolean PING_IN_FLIGHT = new AtomicBoolean(false);
    private static final Logger LOGGER = LoggerFactory.getLogger("att2-party-sync");
    private static volatile long nextHeartbeatMs;
    private static volatile long lastWarningMs;
    private static volatile int lastEventId;
    private static volatile UUID sessionPlayer;
    private static long lastHeartbeatMs;
    private static volatile boolean warnedUnreachable;
    private static long pingSequence;
    private static long lastPingMs;
    private static volatile long sessionGeneration;

    private PartySync() {
    }

    public static void tick(Minecraft client) {
        drainIncoming(client);
        PingMarkers.tick(client);
        if (client.player != null && client.level != null && PartySyncHttp.isConfigured()) {
            long now = System.currentTimeMillis();
            if (now >= nextHeartbeatMs && now - lastHeartbeatMs >= 1000L && !HEARTBEAT_IN_FLIGHT.get()) {
                lastHeartbeatMs = now;
                UUID self = client.player.getUUID();
                sessionPlayer = self;
                String name = client.player.getName().getString();
                List<String> seen = tabIds(client, self);
                long generation = sessionGeneration;
                HEARTBEAT_IN_FLIGHT.set(true);
                IO.execute(() -> {
                    try {
                        JsonObject response = PartySyncHttp.heartbeat(self.toString(), name, seen, lastEventId);
                        ingestHeartbeat(response, generation);
                        warnedUnreachable = false;
                    } catch (Exception failure) {
                        if (generation == sessionGeneration) {
                            nextHeartbeatMs = System.currentTimeMillis() + 5000L;
                            warnUnreachable(failure);
                        }
                    } finally {
                        HEARTBEAT_IN_FLIGHT.set(false);
                    }
                });
            }
        }
    }

    public static synchronized void onJoin() {
        sessionGeneration++;
        lastEventId = 0;
        lastHeartbeatMs = 0L;
        nextHeartbeatMs = 0L;
        lastWarningMs = 0L;
        INCOMING.clear();
        PingMarkers.clear();
        warnedUnreachable = false;
        lastPingMs = 0L;
    }

    public static synchronized void onDisconnect() {
        sessionGeneration++;
        UUID id = sessionPlayer;
        INCOMING.clear();
        PingMarkers.clear();
        lastEventId = 0;
        sessionPlayer = null;
        if (id != null && PartySyncHttp.isConfigured()) {
            IO.execute(() -> PartySyncHttp.leave(id.toString()));
        }
    }

    public static boolean isEnabled() {
        return PartySyncHttp.isConfigured();
    }

    public static void shareHeldItem(Minecraft client) {
        if (client.player != null && client.level != null) {
            ItemStack stack = client.player.getMainHandItem();
            if (stack.isEmpty()) {
                client.player.displayClientMessage(Component.literal("§c" + ModLanguageManager.getString("keybind.broadcast.empty")), true);
            } else {
                Optional<String> encoded = ItemShareCodec.encode(stack, client.level.registryAccess());
                if (encoded.isEmpty()) {
                    client.player.displayClientMessage(ModLanguageManager.get("keybind.broadcast.too_large"), true);
                } else {
                    String playerName = client.player.getName().getString();
                    ChatItemUtils.sendItemInChat(stack, playerName + " " + ModLanguageManager.getString("keybind.broadcast.shared"));
                    JsonObject payload = new JsonObject();
                    payload.addProperty("itemName", ItemShareCodec.displayName(stack));
                    payload.addProperty("itemId", ItemShareCodec.itemId(stack));
                    payload.addProperty("itemSnbt", encoded.get());
                    UUID self = client.player.getUUID();
                    List<String> seen = tabIds(client, self);
                    long generation = sessionGeneration;
                    IO.execute(() -> publishOrHeartbeat(self.toString(), playerName, seen, "item_share", payload, generation));
                }
            }
        }
    }

    public static void pingLookedAt(Minecraft client) {
        if (client.player != null && client.level != null) {
            if (!PingMarkers.dismissLookedAt()) {
                PingTargeting.Target target = PingTargeting.lookAt(client);
                if (target == null) {
                    client.player.displayClientMessage(Component.literal("§c" + ModLanguageManager.getString("keybind.ping.miss")), true);
                } else {
                    Vec3 pos = target.pos();
                    String dimension = String.valueOf(client.level.dimension());
                    String playerName = client.player.getName().getString();
                    long now = System.currentTimeMillis();
                    if (now - lastPingMs >= 250L && PING_IN_FLIGHT.compareAndSet(false, true)) {
                        if (now - lastPingMs > 1000L) {
                            pingSequence++;
                        }

                        lastPingMs = now;
                        PingMarkers.add(client.player.getUUID(), playerName, pos, dimension, target.entityId(), target.item(), pingSequence, true);
                        JsonObject payload = new JsonObject();
                        payload.addProperty("x", pos.x);
                        payload.addProperty("y", pos.y);
                        payload.addProperty("z", pos.z);
                        payload.addProperty("dimension", dimension);
                        payload.addProperty("targetLabel", target.label());
                        payload.addProperty("sequence", pingSequence);
                        if (target.entityId() != null) {
                            payload.addProperty("entityId", target.entityId().toString());
                        }

                        ItemShareCodec.encode(target.item(), client.level.registryAccess()).ifPresent(snbt -> payload.addProperty("itemSnbt", snbt));
                        UUID self = client.player.getUUID();
                        List<String> seen = tabIds(client, self);
                        long generation = sessionGeneration;
                        IO.execute(() -> {
                            try {
                                publishOrHeartbeat(self.toString(), playerName, seen, "ping", payload, generation);
                            } finally {
                                PING_IN_FLIGHT.set(false);
                            }
                        });
                    }
                }
            }
        }
    }

    private static void publishOrHeartbeat(String playerId, String playerName, List<String> seen, String type, JsonObject payload, long generation) {
        if (generation == sessionGeneration) {
            try {
                PartySyncRetry.publish(() -> {
                    try {
                        PartySyncHttp.publish(playerId, type, payload);
                    } catch (PartySyncHttp.ApiException ex) {
                        if (ex.status != 409) {
                            throw ex;
                        }

                        ingestHeartbeat(PartySyncHttp.heartbeat(playerId, playerName, seen, lastEventId), generation);
                        if (generation == sessionGeneration) {
                            PartySyncHttp.publish(playerId, type, payload);
                        }
                    }
                }, Thread::sleep, () -> generation == sessionGeneration);
                warnedUnreachable = false;
            } catch (Exception failure) {
                if (generation == sessionGeneration) {
                    warnUnreachable(failure);
                }
            }
        }
    }

    private static void warnUnreachable(Exception failure) {
        long now = System.currentTimeMillis();
        if (!warnedUnreachable && now - lastWarningMs >= 5000L) {
            warnedUnreachable = true;
            lastWarningMs = now;
            LOGGER.warn("Party-sync request failed: {}", failure.toString());
            String key = failure instanceof PartySyncHttp.ApiException api && api.status == 429
                ? "party_sync.rate_limited"
                : (failure instanceof PartySyncHttp.ApiException ? "party_sync.http_error" : "party_sync.unreachable");
            String detail;
            String var8 = detail = failure instanceof PartySyncHttp.ApiException api ? " (HTTP " + api.status + ")" : "";
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("§c" + ModLanguageManager.getString(key) + detail), true);
                }
            });
        }
    }

    private static synchronized void ingestHeartbeat(JsonObject response, long generation) {
        if (generation == sessionGeneration) {
            if (response.has("lastEventId")) {
                lastEventId = Math.max(lastEventId, response.get("lastEventId").getAsInt());
            }

            for (PartyEvent event : PartySyncHttp.eventsOf(response)) {
                INCOMING.offer(event);
                lastEventId = Math.max(lastEventId, event.id());
            }
        }
    }

    private static void drainIncoming(Minecraft client) {
        PartyEvent event;
        while ((event = INCOMING.poll()) != null) {
            try {
                apply(client, event);
            } catch (IllegalArgumentException | IllegalStateException var3) {
            }
        }
    }

    private static void apply(Minecraft client, PartyEvent event) {
        if (client.player != null && client.level != null) {
            if (event.isItemShare()) {
                ItemStack stack = ItemShareCodec.decode(string(event.payload(), "itemSnbt"), client.level.registryAccess());
                String label = event.playerName() + " " + ModLanguageManager.getString("keybind.broadcast.shared");
                if (!stack.isEmpty()) {
                    ChatItemUtils.sendItemInChat(stack, label);
                } else {
                    String fallback = string(event.payload(), "itemName");
                    client.player.displayClientMessage(Component.literal(fallback.isEmpty() ? label : label + " " + fallback), false);
                }
            } else {
                if (event.isPing()) {
                    JsonObject payload = event.payload();
                    if (!payload.has("x") || !payload.has("y") || !payload.has("z")) {
                        return;
                    }

                    Vec3 pos = new Vec3(payload.get("x").getAsDouble(), payload.get("y").getAsDouble(), payload.get("z").getAsDouble());
                    String dimension = string(payload, "dimension");
                    UUID author = UUID.fromString(event.playerId());
                    String entity = string(payload, "entityId");
                    UUID entityId = entity.isEmpty() ? null : UUID.fromString(entity);
                    long sequence = payload.has("sequence") ? payload.get("sequence").getAsLong() : event.id();
                    ItemStack item = ItemShareCodec.decode(string(payload, "itemSnbt"), client.level.registryAccess());
                    PingMarkers.add(author, event.playerName(), pos, dimension, entityId, item, sequence, true);
                }
            }
        }
    }

    private static List<String> tabIds(Minecraft client, UUID self) {
        List<UUID> listed = new ArrayList<>();
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            for (PlayerInfo info : connection.getOnlinePlayers()) {
                if (info != null && info.getProfile() != null && info.getProfile().id() != null) {
                    listed.add(info.getProfile().id());
                }
            }
        }

        return PartyRoster.listedIds(self, listed);
    }

    private static String string(JsonObject json, String key) {
        if (json != null && json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsString();
            } catch (Exception ignored) {
                return "";
            }
        } else {
            return "";
        }
    }
}
