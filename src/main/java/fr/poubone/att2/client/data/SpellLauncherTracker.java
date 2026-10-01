package fr.poubone.att2.client.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

public final class SpellLauncherTracker {
    public static final int FLASH_TICKS = 20;
    private static final Pattern LEVEL_PATTERN = Pattern.compile("lvl(\\d+)");
    private static List<SpellLauncherTracker.LauncherState> states = List.of();
    private static final Map<Integer, Integer> lastTenths = new HashMap<>();
    private static final Map<Integer, Integer> flash = new HashMap<>();

    private SpellLauncherTracker() {
    }

    public static List<SpellLauncherTracker.LauncherState> states() {
        return states;
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            reset();
        } else {
            Map<Integer, SpellLauncherTracker.LauncherState> found = new TreeMap<>();

            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                consider(found, player.getInventory().getItem(slot));
            }

            consider(found, player.getOffhandItem());

            for (SpellLauncherTracker.LauncherState state : found.values()) {
                Integer previous = lastTenths.get(state.spellId);
                if (previous != null && previous > 0 && state.cooldownTenths == 0) {
                    flash.put(state.spellId, 20);
                }

                lastTenths.put(state.spellId, state.cooldownTenths);
                Integer remaining = flash.get(state.spellId);
                if (remaining != null && remaining > 0) {
                    state.flashTicks = remaining;
                    flash.put(state.spellId, remaining - 1);
                }
            }

            lastTenths.keySet().retainAll(found.keySet());
            flash.keySet().retainAll(found.keySet());
            states = List.copyOf(found.values());
        }
    }

    private static void consider(Map<Integer, SpellLauncherTracker.LauncherState> found, ItemStack stack) {
        SpellLauncherTracker.LauncherState state = parse(stack);
        if (state != null) {
            found.putIfAbsent(state.spellId, state);
        }
    }

    public static SpellLauncherTracker.LauncherState parse(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && !data.isEmpty()) {
                CompoundTag tag = data.copyTag();
                if (!"launcher".equals(tag.getString("Dahal").orElse(""))) {
                    return null;
                }

                int spellId = tag.getIntOr("Spell", 0);
                if (spellId <= 0) {
                    return null;
                }

                int tenths = 0;
                CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
                if (modelData != null && !modelData.floats().isEmpty()) {
                    tenths = Math.round(modelData.floats().get(0));
                }

                tenths = Math.max(0, Math.min(10, tenths));
                int level = 0;
                Matcher matcher = LEVEL_PATTERN.matcher(stack.getHoverName().getString());
                if (matcher.find()) {
                    level = Integer.parseInt(matcher.group(1));
                }

                return new SpellLauncherTracker.LauncherState(spellId, stack, level, tenths);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static void reset() {
        states = List.of();
        lastTenths.clear();
        flash.clear();
    }

    public static final class LauncherState {
        public final int spellId;
        public final ItemStack stack;
        public final int level;
        public final int cooldownTenths;
        public int flashTicks;

        LauncherState(int spellId, ItemStack stack, int level, int cooldownTenths) {
            this.spellId = spellId;
            this.stack = stack;
            this.level = level;
            this.cooldownTenths = cooldownTenths;
        }

        public boolean ready() {
            return this.cooldownTenths == 0;
        }
    }
}
