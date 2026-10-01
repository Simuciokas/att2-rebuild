package fr.poubone.att2.client.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

public final class ScoreCache {
    private static final Map<String, Integer> VALUES = new HashMap<>();
    private static final Map<String, Map<String, Integer>> HOLDERS = new HashMap<>();

    private ScoreCache() {
    }

    public static void clear() {
        VALUES.clear();
        HOLDERS.clear();
    }

    public static void put(String objectiveName, int value) {
        if (objectiveName != null && !objectiveName.isBlank()) {
            VALUES.put(objectiveName, value);
        }
    }

    public static void putHolder(String objectiveName, String holderName, int value) {
        if (objectiveName != null && !objectiveName.isBlank() && holderName != null) {
            HOLDERS.computeIfAbsent(objectiveName, key -> new HashMap<>()).put(holderName, value);
        }
    }

    public static void remove(String objectiveName) {
        if (objectiveName != null) {
            VALUES.remove(objectiveName);
        }
    }

    public static OptionalInt get(String objectiveName) {
        OptionalInt live = readLive(objectiveName);
        if (live.isPresent()) {
            VALUES.put(objectiveName, live.getAsInt());
            return live;
        } else {
            Integer cached = VALUES.get(objectiveName);
            return cached == null ? OptionalInt.empty() : OptionalInt.of(cached);
        }
    }

    public static int getOrDefault(String objectiveName, int fallback) {
        return get(objectiveName).orElse(fallback);
    }

    public static boolean has(String objectiveName) {
        return get(objectiveName).isPresent();
    }

    public static OptionalInt readLive(String objectiveName) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return OptionalInt.empty();
        }

        LocalPlayer player = client.player;
        if (player != null && client.level != null) {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName);
            Objective listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
            if (objective == null && listObjective != null && objectiveName.equals(listObjective.getName())) {
                objective = listObjective;
            }

            if (objective == null) {
                return OptionalInt.empty();
            }

            ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, objective);
            Integer direct = info == null ? null : info.value();
            return ScoreboardLiveRead.read(
                objectiveName,
                player.getScoreboardName(),
                objective.getName(),
                listObjective == null ? null : listObjective.getName(),
                direct,
                listedEntries(scoreboard, objective)
            );
        } else {
            return OptionalInt.empty();
        }
    }

    public static OptionalInt readHolder(String objectiveName, ScoreHolder holder) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null && holder != null) {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName);
            if (objective == null) {
                return OptionalInt.empty();
            }

            ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(holder, objective);
            return info == null ? OptionalInt.empty() : OptionalInt.of(info.value());
        } else {
            return OptionalInt.empty();
        }
    }

    public static OptionalInt readHolder(String objectiveName, String holderName) {
        return readHolder(objectiveName, ScoreHolder.forNameOnly(holderName));
    }

    public static OptionalInt getHolder(String objectiveName, String holderName) {
        OptionalInt live = readHolder(objectiveName, holderName);
        if (live.isPresent()) {
            HOLDERS.computeIfAbsent(objectiveName, key -> new HashMap<>()).put(holderName, live.getAsInt());
            return live;
        }

        Map<String, Integer> byHolder = HOLDERS.get(objectiveName);
        if (byHolder == null) {
            return OptionalInt.empty();
        }

        Integer cached = byHolder.get(holderName);
        return cached == null ? OptionalInt.empty() : OptionalInt.of(cached);
    }

    public static OptionalInt getHolder(String objectiveName, ScoreHolder holder) {
        return holder == null ? OptionalInt.empty() : getHolder(objectiveName, holder.getScoreboardName());
    }

    static void refresh(Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && client.level != null) {
            Scoreboard scoreboard = client.level.getScoreboard();

            for (Objective objective : scoreboard.getObjectives()) {
                cacheObjective(player, scoreboard, objective);
            }

            Objective listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
            if (listObjective != null) {
                cacheObjective(player, scoreboard, listObjective);
            }
        }
    }

    private static void cacheObjective(LocalPlayer player, Scoreboard scoreboard, Objective objective) {
        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, objective);
        Integer direct = info == null ? null : info.value();
        List<PlayerListScore.Entry> listed = listedEntries(scoreboard, objective);
        ScoreboardLiveRead.read(objective.getName(), player.getScoreboardName(), objective.getName(), null, direct, listed)
            .ifPresent(value -> VALUES.put(objective.getName(), value));
        Map<String, Integer> holders = HOLDERS.computeIfAbsent(objective.getName(), key -> new HashMap<>());

        for (PlayerListScore.Entry entry : listed) {
            holders.put(entry.owner(), entry.value());
        }
    }

    private static List<PlayerListScore.Entry> listedEntries(Scoreboard scoreboard, Objective objective) {
        List<PlayerListScore.Entry> listed = new ArrayList<>();

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            listed.add(new PlayerListScore.Entry(entry.owner(), entry.value()));
        }

        return listed;
    }
}
