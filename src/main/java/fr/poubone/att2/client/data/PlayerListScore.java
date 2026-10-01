package fr.poubone.att2.client.data;

import java.util.OptionalInt;

public final class PlayerListScore {
    private PlayerListScore() {
    }

    public static OptionalInt find(String playerName, Iterable<PlayerListScore.Entry> listed) {
        if (playerName != null && listed != null) {
            for (PlayerListScore.Entry entry : listed) {
                if (entry != null && playerName.equals(entry.owner())) {
                    return OptionalInt.of(entry.value());
                }
            }

            return OptionalInt.empty();
        } else {
            return OptionalInt.empty();
        }
    }

    public record Entry(String owner, int value) {
    }
}
