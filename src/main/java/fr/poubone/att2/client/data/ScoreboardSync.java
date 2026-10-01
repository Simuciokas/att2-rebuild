package fr.poubone.att2.client.data;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.input.KeybindManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.permissions.Permissions;

public final class ScoreboardSync {
    private static final List<String> ROTATING_SLOTS = List.of("dark_purple", "blue", "dark_green");
    private static final int FAST_ROTATION_INTERVAL_TICKS = 1;
    private static final Set<String> FEEDBACK_KEYS = Set.of(
        "commands.scoreboard.objectives.display.set", "commands.scoreboard.objectives.display.alreadySet", "arguments.objective.notFound"
    );
    private static final Map<String, String> SLOT_CONTENTS = new HashMap<>();
    private static boolean fastMode = false;
    private static List<String> fastPool = List.of();
    private static int rotationIndex = 0;
    private static int slotIndex = 0;
    private static int ticksUntilRotation = 0;
    private static int ticksSinceLastCommand = Integer.MAX_VALUE;
    private static final int FEEDBACK_HIDE_WINDOW_TICKS = 40;

    private ScoreboardSync() {
    }

    public static void reset() {
        fastMode = false;
        rotationIndex = 0;
        slotIndex = 0;
        ticksUntilRotation = 0;
        ticksSinceLastCommand = Integer.MAX_VALUE;
        SLOT_CONTENTS.clear();
        ScoreCache.clear();
    }

    public static void setFastMode(boolean enabled, List<String> objectives) {
        fastMode = enabled;
        fastPool = enabled ? List.copyOf(objectives) : List.of();
        rotationIndex = 0;
        slotIndex = 0;
        ticksUntilRotation = 0;
    }

    public static boolean canSync(Minecraft client) {
        LocalPlayer player = client.player;
        return player != null && player.connection != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
            && !FlashbackCompat.isInReplay();
    }

    public static boolean isCommandFeedback(Component message) {
        return ticksSinceLastCommand > 40 ? false : containsFeedbackKey(message, 0);
    }

    private static boolean containsFeedbackKey(Component component, int depth) {
        if (component != null && depth <= 3) {
            if (component.getContents() instanceof TranslatableContents translatable) {
                if (FEEDBACK_KEYS.contains(translatable.getKey())) {
                    return true;
                }

                if (translatable.getKey().equals("chat.type.admin")) {
                    for (Object arg : translatable.getArgs()) {
                        if (arg instanceof Component inner && containsFeedbackKey(inner, depth + 1)) {
                            return true;
                        }
                    }
                }
            }

            for (Component sibling : component.getSiblings()) {
                if (containsFeedbackKey(sibling, depth + 1)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && client.level != null) {
            ScoreCache.refresh(client);
            if (ticksSinceLastCommand < Integer.MAX_VALUE) {
                ticksSinceLastCommand++;
            }

            if (KeybindManager.showCustomHUD && canSync(client)) {
                if (fastMode && !fastPool.isEmpty()) {
                    if (ticksUntilRotation > 0) {
                        ticksUntilRotation--;
                        return;
                    }

                    ticksUntilRotation = 1;
                    String objective = fastPool.get(rotationIndex);
                    rotationIndex = (rotationIndex + 1) % fastPool.size();
                    String slot = ROTATING_SLOTS.get(slotIndex);
                    slotIndex = (slotIndex + 1) % ROTATING_SLOTS.size();
                    sendDisplay(player, slot, objective);
                }
            }
        }
    }

    private static void sendDisplay(LocalPlayer player, String teamColor, String objective) {
        if (!objective.equals(SLOT_CONTENTS.get(teamColor))) {
            SLOT_CONTENTS.put(teamColor, objective);
            ticksSinceLastCommand = 0;
            player.connection.sendCommand("scoreboard objectives setdisplay sidebar.team." + teamColor + " " + objective);
        }
    }
}
