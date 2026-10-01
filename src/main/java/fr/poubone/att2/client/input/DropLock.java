package fr.poubone.att2.client.input;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemRarity;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class DropLock {
    private static final long CONFIRM_WINDOW_MS = 2000L;
    private static ItemStack pending = ItemStack.EMPTY;
    private static long pendingUntilMs;
    private static boolean allowOnce;

    private DropLock() {
    }

    public static void allowOnce() {
        allowOnce = true;
    }

    public static boolean shouldBlock(Minecraft client, ItemStack stack) {
        if (allowOnce) {
            allowOnce = false;
            return false;
        }

        HUDConfig config = HUDConfig.get();
        if (config.dropLockEnabled && stack != null && !stack.isEmpty()) {
            ItemRarity rarity = ItemRarity.fromStack(stack);
            if (rarity != null && config.dropLockRarities.contains(rarity.id)) {
                long now = System.currentTimeMillis();
                if (!pending.isEmpty() && now < pendingUntilMs && ItemStack.isSameItemSameComponents(pending, stack)) {
                    pending = ItemStack.EMPTY;
                    return false;
                }

                pending = stack.copy();
                pendingUntilMs = now + 2000L;
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal("§c").append(ModLanguageManager.get("drop_lock.confirm")), true);
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
