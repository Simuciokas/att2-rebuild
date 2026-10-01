package fr.poubone.att2.client.data;

import fr.poubone.att2.client.input.DropLock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class SpellLevelSelector {
    private static final int VERIFY_TICKS = 50;
    private static final int WAIT_TRIGGERS_TICKS = 12;
    private static final int WAIT_ITEM_TICKS = 20;
    private static final int RELOAD_IDLE = 0;
    private static final int RELOAD_WAIT_TRIGGERS = 1;
    private static final int RELOAD_DROP_LAUNCHER = 2;
    private static final int RELOAD_WAIT_BOOK = 3;
    private static final int RELOAD_DROP_BOOK = 4;
    private static final int RELOAD_WAIT_LAUNCHER = 5;
    private static final Map<Integer, Set<Integer>> locked = new HashMap<>();
    private static int pendingSpell;
    private static int pendingLevel;
    private static int verifyTicks;
    private static int reloadPhase;
    private static int reloadWait;

    private SpellLevelSelector() {
    }

    public static void select(int spellId, int level) {
        int trigger = SpellSelectTriggers.levelSelectTrigger(spellId, level);
        if (trigger >= 0) {
            Att2Triggers.send(trigger);
            int obtain = SpellSelectTriggers.obtainTrigger(spellId);
            if (obtain >= 0) {
                Att2Triggers.send(obtain);
            }

            pendingSpell = spellId;
            pendingLevel = level;
            verifyTicks = 50;
            reloadPhase = 1;
            reloadWait = 12;
        }
    }

    public static boolean isLocked(int spellId, int level) {
        Set<Integer> levels = locked.get(spellId);
        return levels != null && levels.contains(level);
    }

    public static void tick() {
        if (verifyTicks > 0 || reloadPhase != 0) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                verifyTicks = 0;
                reloadPhase = 0;
            } else if (accepted(player)) {
                Set<Integer> levels = locked.get(pendingSpell);
                if (levels != null) {
                    levels.remove(pendingLevel);
                }

                verifyTicks = 0;
                reloadPhase = 0;
            } else {
                tickReload(player);
                if (verifyTicks > 0) {
                    verifyTicks--;
                    if (verifyTicks == 0) {
                        locked.computeIfAbsent(pendingSpell, k -> new HashSet<>()).add(pendingLevel);
                        reloadPhase = 0;
                    }
                }
            }
        }
    }

    public static void reset() {
        locked.clear();
        verifyTicks = 0;
        reloadPhase = 0;
        reloadWait = 0;
    }

    private static boolean accepted(LocalPlayer player) {
        SpellLauncherTracker.LauncherState held = SpellLauncherTracker.parse(player.getMainHandItem());
        return held != null && held.spellId == pendingSpell && held.level == pendingLevel;
    }

    private static void tickReload(LocalPlayer player) {
        if (reloadPhase != 0) {
            if (Minecraft.getInstance().screen == null) {
                switch (reloadPhase) {
                    case 1:
                        if (--reloadWait <= 0) {
                            reloadPhase = 2;
                            reloadWait = 20;
                        }
                        break;
                    case 2:
                        dropLauncher(player);
                        break;
                    case 3:
                        if (GrimoireXp.isGrimoire(player.getMainHandItem(), pendingSpell)) {
                            reloadPhase = 4;
                            reloadWait = 20;
                            dropBook(player);
                        } else if (--reloadWait <= 0) {
                            reloadPhase = 0;
                        }
                        break;
                    case 4:
                        dropBook(player);
                        break;
                    case 5:
                        if (--reloadWait <= 0) {
                            reloadPhase = 0;
                        }
                        break;
                    default:
                        reloadPhase = 0;
                }
            }
        }
    }

    private static void dropLauncher(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (GrimoireXp.isGrimoire(held, pendingSpell)) {
            reloadPhase = 4;
            reloadWait = 20;
            dropBook(player);
        } else {
            SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(held);
            if (launcher != null && launcher.spellId == pendingSpell) {
                if (tryDrop(player)) {
                    reloadPhase = 3;
                    reloadWait = 20;
                } else if (--reloadWait <= 0) {
                    reloadPhase = 0;
                }
            } else {
                reloadPhase = 0;
            }
        }
    }

    private static void dropBook(LocalPlayer player) {
        if (!GrimoireXp.isGrimoire(player.getMainHandItem(), pendingSpell)) {
            if (--reloadWait <= 0) {
                reloadPhase = 0;
            }
        } else {
            if (tryDrop(player)) {
                reloadPhase = 5;
                reloadWait = 20;
            } else if (--reloadWait <= 0) {
                reloadPhase = 0;
            }
        }
    }

    private static boolean tryDrop(LocalPlayer player) {
        DropLock.allowOnce();
        return player.drop(false);
    }
}
