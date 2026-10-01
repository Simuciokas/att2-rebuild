package fr.poubone.att2.client.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

public class BroadcastScanner {
    private static final Map<UUID, Long> seen = new HashMap<>();
    private static final long LIFESPAN_MS = 2000L;

    public static void tick(Minecraft client) {
        if (client.level != null && client.player != null) {
            long now = System.currentTimeMillis();

            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity instanceof ArmorStand stand && stand.getCustomName() != null) {
                    String name = stand.getCustomName().getString();
                    if (name.equals("[BROADCAST_TAG]")) {
                        UUID id = stand.getUUID();
                        if (!seen.containsKey(id)) {
                            ItemStack stack = stand.getMainHandItem();
                            if (!stack.isEmpty()) {
                                ChatItemUtils.sendItemInChat(stack);
                                seen.put(id, now);
                            }
                        }
                    }
                }
            }

            Iterator<Entry<UUID, Long>> it = seen.entrySet().iterator();

            while (it.hasNext()) {
                Entry<UUID, Long> entry = it.next();
                if (now - entry.getValue() > 2000L) {
                    it.remove();
                }
            }
        }
    }
}
