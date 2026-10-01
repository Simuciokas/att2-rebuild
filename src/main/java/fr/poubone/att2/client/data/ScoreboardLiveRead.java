package fr.poubone.att2.client.data;

import java.util.OptionalInt;

public final class ScoreboardLiveRead {
    private ScoreboardLiveRead() {
    }

    public static OptionalInt read(
        String requestedObjective,
        String playerName,
        String knownObjectiveName,
        String listDisplayObjective,
        Integer directScore,
        Iterable<PlayerListScore.Entry> listed
    ) {
        if (requestedObjective != null && !requestedObjective.isBlank()) {
            boolean available = requestedObjective.equals(knownObjectiveName) || requestedObjective.equals(listDisplayObjective);
            if (!available) {
                return OptionalInt.empty();
            } else {
                return directScore != null ? OptionalInt.of(directScore) : PlayerListScore.find(playerName, listed);
            }
        } else {
            return OptionalInt.empty();
        }
    }
}
