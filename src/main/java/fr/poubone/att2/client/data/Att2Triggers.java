package fr.poubone.att2.client.data;

import java.util.ArrayDeque;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class Att2Triggers {
    public static final String OBJECTIVE = "ScoreTrigger";
    public static final int REPAIR_HELMET = 416;
    public static final int REPAIR_CHESTPLATE = 417;
    public static final int REPAIR_LEGGINGS = 418;
    public static final int REPAIR_BOOTS = 419;
    public static final int REPAIR_OFFHAND = 420;
    public static final int REPAIR_MAINHAND = 421;
    public static final int REPAIR_ALL = 422;
    public static final int TOOLS_NUMBER_INFO = 3607;
    public static final int RUNE_BUNDLE_INFO = 3608;
    public static final int MAIN_QUEST_GO = 1068;
    public static final int CONSCIOUSNESS_SIDEQUEST_LIST = 2315;
    public static final int CONSCIOUSNESS_DAILYQUEST_LIST = 3280;
    public static final int CONSCIOUSNESS_CLEAR = 2310;
    public static final int HORSE_WHISTLE = 1070;
    public static final int COLLECT_ITEMS = 1072;
    public static final int CONSCIOUSNESS_ATTRIBUTE = 2313;
    public static final int CONSCIOUSNESS_PLAYER_INFO = 2314;
    public static final int CONSCIOUSNESS_CURRENCY = 2312;
    public static final int PLAYER_GLOW = 1085;
    public static final int STAT_DISPLAY_ALL_RUN = 556;
    public static final int STAT_DISPLAY_ALL_STOP = 557;
    public static final int STAT_DISPLAY_COUNT = 558;
    public static final Map<String, Integer> STAT_UPGRADE = Map.of(
        "STR", 1077, "RES", 1078, "HAS", 1079, "SPD", 1080, "HER", 1081, "DAR", 1082, "LUC", 1083, "HUN", 1084, "CRT", 3529
    );
    private static final ArrayDeque<Integer> QUEUE = new ArrayDeque<>();
    private static final int SEND_INTERVAL_TICKS = 2;
    private static int cooldown = 0;

    private Att2Triggers() {
    }

    public static String command(int trigger) {
        return "trigger ScoreTrigger set " + trigger;
    }

    public static void send(int trigger) {
        if (!QUEUE.contains(trigger)) {
            QUEUE.addLast(trigger);
        }
    }

    public static void sendAlways(int trigger) {
        QUEUE.addLast(trigger);
    }

    public static boolean isGameplayPaused(Minecraft client) {
        return client.isPaused() || client.screen != null && client.screen.isPauseScreen();
    }

    public static void tick(Minecraft client) {
        if (!isGameplayPaused(client)) {
            if (cooldown > 0) {
                cooldown--;
            } else {
                LocalPlayer player = client.player;
                if (player != null && player.connection != null) {
                    Integer trigger = QUEUE.pollFirst();
                    if (trigger != null) {
                        cooldown = 2;
                        player.connection.sendCommand(command(trigger));
                    }
                } else {
                    QUEUE.clear();
                }
            }
        }
    }

    public static void reset() {
        QUEUE.clear();
        cooldown = 0;
    }

    public static int parseTriggerCommand(String command) {
        if (command == null) {
            return -1;
        }

        String normalized = command.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).stripLeading();
        }

        String[] parts = normalized.split("\\s+");
        if (parts.length != 4) {
            return -1;
        }

        if (!parts[0].equalsIgnoreCase("trigger") && !parts[0].equalsIgnoreCase("minecraft:trigger")) {
            return -1;
        }

        if (parts[1].equals("ScoreTrigger") && parts[2].equalsIgnoreCase("set")) {
            try {
                return Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                return -1;
            }
        } else {
            return -1;
        }
    }
}
